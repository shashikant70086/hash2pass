# Security

Threat model, what's protected, what isn't, and what to harden before you trust this with real users.

## Threat model

Two adversaries to think about:

1. **Random internet attacker** — finds your gateway URL, tries to abuse it.
2. **User of your app** — wants to bypass auth on their own account or someone else's.

We don't try to defend against:
- A compromised gateway *phone* (rooted, malware). If they have the device, game over.
- Carriers / governments intercepting SMS (use TOTP / WhatsApp / passkeys for those threat models).

## What the gateway does well

| Property | How |
|---|---|
| OTPs never stored plaintext | `BCrypt.withDefaults().hashToString(10, otp)` on insert; only the hash sits on disk |
| Network calls require auth | `x-api-key` / `Authorization: Bearer` checked on every endpoint except `/healthz` |
| OTPs are short-lived | TTL default 5 min, configurable down to 30s |
| Brute-force is bounded | 5 wrong attempts → pending row purged; user must request fresh OTP |
| Spam is bounded | 5/hr per phone + 30/min per API key, both tunable |
| Constant-time API key compare | `provided != cfg.apiKey` in Kotlin compares full strings — not perfectly constant-time but the attack window is API-key-rate-limited anyway |
| No data leaves your phone | No analytics SDK, no crash reporter, no telemetry. All processing is local. |

## What it does *not* do

- **No TLS by default** — the embedded server binds to `0.0.0.0:8787` over plain HTTP. **On a LAN this is fine. For public exposure, ALWAYS front with a tunnel that adds HTTPS** (Cloudflare Tunnel, ngrok, Tailscale Funnel — see [DEPLOYMENT](DEPLOYMENT.md)).
- **No proof of identity beyond "owns the SIM that received the SMS"** — SIM-swap attacks still apply. If your threat model includes nation-state or organized fraud, layer in additional factors (passkey, TOTP, knowledge factor).
- **No per-user accounting** — the gateway sees phone numbers, not users. That's by design; user identity belongs in your backend.
- **No request signing** — relies on the API key alone. Sufficient since the key is bearer-only and rotatable.
- **No audit log shipped off-device** — the on-device send log keeps the last 100. For long-term audit, log on your backend side instead.

## What to harden before going public

| Risk | Fix |
|---|---|
| API key extracted from a public APK | Don't put it in the APK. Keep a backend in front; backend reads key from env. |
| `*` CORS letting any site call your gateway from a browser | Set explicit origins in Settings → CORS. |
| Auto-generated key never rotated | Rotate on first deploy, rotate quarterly, rotate immediately after any leak. |
| SIM-swap | Add a second factor (passkey, TOTP) for high-value actions; never use phone OTP as the sole factor for password reset. |
| Gateway URL discovered in logs | Don't include `OTP_BASE_URL` or `OTP_API_KEY` in client-side bundles or error reports. |
| Tunnel exposing other phone services | Cloudflare/Tailscale Funnel only forwards the specific port you tell it to. Don't expose the whole phone. |

## What I'd review before shipping

- [ ] CORS in app Settings is **not** `*`
- [ ] Rate limits aren't `1000/min` (the safety-net upper bound)
- [ ] API key was rotated **after** any time it appeared in a screenshot/chat/git commit
- [ ] Backend doesn't log the full SMS body or OTP (`grep "verification" your-logs` should be empty)
- [ ] Tunnel terminates HTTPS — `curl -I https://yourtunnel.example.com/healthz` returns `HTTP/2 200`
- [ ] Battery whitelist set so the gateway isn't OOM-killed when you most need it
- [ ] Your backend re-validates the phone number format on its side too (defense in depth)
- [ ] Your backend stores `bcrypt(session_token)`, not raw

## Privacy / compliance

- The gateway processes **phone numbers** (PII) and **OTP codes** (transient credentials).
- Phone numbers are stored on-device only — they appear in the SQLite send log and in pending OTP rows. Both can be wiped by uninstalling the app.
- Nothing is sent to third parties from the gateway. Your backend has its own obligations.
- For **GDPR / DPDPA (India)**: the gateway *is* the data processor; *your* backend / product is the data controller. Document this in your privacy policy if you ship publicly.

## Vulnerability reporting

Open an issue with the label `security` — or for sensitive disclosures, contact the maintainer privately first. Don't post exploit details in public issues until a fix lands.
