# FAQ

Honest answers, including the ones that say "don't use this for that".

## Is this production-ready?

For side projects, internal tools, college apps, beta launches: **yes**, with the [going-to-production checklist](DEPLOYMENT.md#going-to-production-checklist).

For a regulated fintech / health product with thousands of users / 24×7 SLA: **no, not as your only OTP path**. Layer it with a Twilio or WhatsApp Cloud API fallback so a dead SIM doesn't take down auth.

## How much does it cost?

| Item | Cost |
|---|---|
| The gateway app | ₹0 |
| Hosting | ₹0 (runs on your phone) |
| Per OTP | Whatever your SIM's SMS pack works out to — usually ₹0.05–₹0.20 in India |
| Tunneling (optional) | ₹0 with Cloudflare Tunnel or Tailscale Funnel; ~₹800/mo for ngrok stable subdomain |

Compare with Twilio (~₹0.50/SMS India) or MSG91 (~₹0.20–₹0.30 + GST).

## Will this get my SIM banned?

For modest volume (≤ 100 SMS/day from a consumer SIM), no.

For higher volume on Indian consumer SIMs, possibly yes:
- TRAI's DLT regulations technically require registered sender IDs for transactional SMS.
- Carriers can throttle/block a SIM that sends repetitive templated SMS to many recipients.

Mitigations:
- Use multiple SMS templates (Settings) so the messages look different.
- Avoid trigger words like *OTP, verification, code, password, login* in the template.
- Don't include URLs.
- Keep volume reasonable.
- For real volume, register as a sender on DLT (free, ~24h approval) and use a registered template.

## Why not just use Firebase Phone Auth?

Firebase Phone Auth requires the **Blaze plan** (pay-as-you-go) — you must put a billing-enabled credit card on file even if you stay within the free 10k/mo. Lots of dev/college accounts don't want this.

If you already pay for Firebase: Firebase Phone Auth is great and easier than this. Use it.

## Why not Twilio / MSG91 / Vonage?

Cost. ₹0.20–₹0.80 per OTP × thousands of users = real bills on day one.

For a side project that may or may not take off, "free until I have users" is a useful default.

## Why not the WhatsApp Cloud API?

Excellent for OTPs actually — higher delivery in India, free tier 1000/mo, no SIM dependency. Strong recommendation: **add it as your primary channel** for any user with WhatsApp installed, and use this gateway as the SMS fallback for users without WhatsApp.

Implementation is sketched in earlier conversations / the project roadmap.

## Can it send to international numbers?

Whatever your SIM can send to. Most Indian prepaid plans charge ₹3–₹5 per international SMS. Check before integrating users abroad.

## Can the phone *receive* SMS too (auto-read codes)?

Not yet. The app declares `RECEIVE_SMS` in the manifest but doesn't use it. Could be added if there's interest — open an issue.

## Can I run it on multiple phones (multi-SIM redundancy)?

Each phone runs an independent gateway with its own API key. To round-robin, do it in your backend:

```javascript
const GATEWAYS = [
  { base: 'https://gw1.example.com', key: process.env.GW1_KEY },
  { base: 'https://gw2.example.com', key: process.env.GW2_KEY },
];
function pick() { return GATEWAYS[Math.floor(Math.random() * GATEWAYS.length)]; }
```

A native multi-device mode (one app, multiple phones in a cluster) is on the roadmap if there's demand.

## Does it work on iOS?

No. iOS doesn't allow apps to send SMS programmatically — Apple restricts `MessageUI` to user-initiated `MFMessageComposeViewController` flows. You'd need an iOS host plus an Android gateway anyway.

## Can I run it on Termux / a Linux phone?

The current build is an Android app. The HTTP server (Ktor on CIO) is portable, but SMS sending uses Android's `SmsManager` — which is Android-only. A Termux version would need a different SMS backend (modem AT commands, etc.). Not planned.

## Does the gateway need internet?

For SMS: just cellular signal.
For the HTTP API: Wi-Fi or mobile data (so your backend can reach it).
You can run the API on Wi-Fi only and send SMS over cellular — they're independent.

## What's the deal with port 8787?

It's the default. Easy to remember. Change to whatever in Settings.

## Will it run on Android Go / 1 GB RAM phones?

Yes, but slowly under load. Ktor + bcrypt are memory-bearable but not free. Verify works fine for small volumes (< 100/day).

## Why Ktor instead of NanoHTTPD?

Ktor's CIO engine is Kotlin-native, plays well with coroutines (Android-friendly), and ships with JSON content-negotiation. NanoHTTPD would shave ~300 KB off the APK but cost legibility and async support.

## Why bcrypt over SHA-256?

You're hashing 6 digits — only 1 million possible values. SHA-256 is reversible in milliseconds via brute force. bcrypt's cost factor (10 = ~50ms/attempt) makes the brute-force prohibitive even if someone exfiltrates `gateway.db`.

## Why Kotlin and not Flutter / React Native?

Native SMS sending. `SmsManager` is plain Android API; you'd need a platform channel from RN/Flutter anyway. May as well skip the abstraction.

## Will you upload it to Play Store?

`SEND_SMS` is one of Google's heavily restricted permissions. Apps using it must justify the use case and get manual approval, which they routinely deny for "infrastructure" apps. The plan is sideload-only via signed APK from GitHub Releases.

## Can I sell SaaS based on this?

It's MIT-licensed — yes, you can. If you do, consider:
- Contributing improvements back (PRs welcome).
- Mentioning the project in your docs.
- Sponsoring continued maintenance.

## I have a feature request / bug

GitHub issues. Include the info from the bottom of [TROUBLESHOOTING.md](TROUBLESHOOTING.md#anything-else).

## I want to contribute

See [CONTRIBUTING.md](../CONTRIBUTING.md). Good first issues are labeled — touch the code, build the APK, ship a PR.
