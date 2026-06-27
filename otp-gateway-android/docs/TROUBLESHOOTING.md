# Troubleshooting

Symptom → likely cause → fix.

## "OTP gateway unavailable. Try again."

This is your backend's friendly version of "I couldn't reach the gateway, or the gateway said 502". Three causes, in order of likelihood:

### 1. The SIM can't send

Most common, especially if it's been a while since you recharged.

**Test from the gateway phone itself**: open Messages, send a normal text to any number.
- Fails too → SIM problem (no balance, expired plan, no signal, blocked carrier).
- Works → the SIM is fine; move to cause 2.

**Fixes**: recharge, switch to a SIM with an active SMS pack, or — if the carrier is dropping transactional-looking SMS — change the SMS template (Settings) to remove words like *OTP, verification, code, password, login*.

### 2. Your backend is pointed at the wrong place

Verify `OTP_BASE_URL` and `OTP_API_KEY` in your backend's `.env` match what the gateway's Home screen shows.

```bash
curl http://192.168.x.x:8787/healthz
# → {"ok":true}
```

If that fails: phone is on a different network, server is stopped, or wrong IP.

### 3. The gateway returned 502

In your gateway's **Log** screen, check the most recent attempt:

| Log status | Meaning |
|---|---|
| `failed (code 1)` | Generic carrier rejection. SIM / DLT / template trigger word. |
| `failed (code 2)` | Radio off — airplane mode or no cell connection. |
| `failed (code 4)` | No service — out of coverage. |
| `failed (code 5)` | Limit exceeded — carrier rate-limiting. Wait or change SIM. |
| Attempt not shown | Backend never reached the gateway — see cause 2. |

## "unauthorized" (401)

Your `OTP_API_KEY` doesn't match the gateway's. Re-copy from Home screen (the masked one — tap the 📋 copy icon, which copies the full unmasked key).

If you rotated the key in Settings, every client must be updated.

## "phone must be E.164" (400)

Phone number isn't in international format. Required:
- Starts with `+`
- Country code (no leading zero)
- Digits only after

| Bad | Good |
|---|---|
| `9876543210` | `+919876543210` |
| `+91 98765 43210` | `+919876543210` |
| `+91-9876543210` | `+919876543210` |
| `09876543210` | `+919876543210` |

Always normalize on your backend with `libphonenumber-js` (Node), `phonenumbers` (Python), etc., before sending.

## "rate limit exceeded for this phone" (429)

You hit the per-phone-per-hour limit (default 5). Wait an hour or increase the limit in Settings.

## "global rate limit exceeded" (429)

Your API key hit per-key-per-minute (default 30). Either you're being abused, you have a runaway loop, or you genuinely have legit traffic over the limit. Tune in Settings.

## OTP arrives, but verify returns 401 "wrong code"

The user typed it wrong, or your backend is sending the wrong `requestId`. Things to check:

- Are you using the *exact* `requestId` returned by `/v1/otp/request`?
- Are you stripping whitespace before sending the code?
- Is the user looking at the latest OTP (not yesterday's)?
- Did the user request a new OTP and you're still passing the old `requestId`?

The pending row is kept after wrong attempts (up to `maxVerifyAttempts`), so the user can retry without restarting.

## OTP arrives, but verify returns 410 "expired"

User took longer than the TTL. They need to request a new OTP. If this happens a lot, bump `TTL` in Settings.

## OTP arrives, but verify returns 404 "unknown request"

Three possibilities:
- The `requestId` was already verified (single-use).
- It expired and was garbage-collected.
- You're using a `requestId` that was never returned — typo or session-mix-up.

## The Home screen says `(no Wi-Fi)`

The phone isn't on Wi-Fi or the Wi-Fi interface has no IPv4. Fix:
- Connect to Wi-Fi.
- Turn airplane mode off/on.
- Reboot the phone if the address still doesn't appear.

The gateway works on mobile data too, but no useful LAN URL — you'd need a tunnel.

## The server says "Stopped" and won't start

Look in the foreground notification — the error text appears there.

| Notification text | Cause | Fix |
|---|---|---|
| `error: Address already in use` | Another app is on `8787` (or your chosen port). | Change port in Settings → Stop/Start. |
| `error: Permission denied` | Port `<1024` chosen. | Pick a port `≥ 1024`. |
| Nothing useful | Probably a crash. | Reboot the phone; if it persists, open an issue with `adb logcat -d \| grep -i OtpHttpServer`. |

## Server stops on its own after a few hours

OEM battery killer. Fix in this order:

1. **Battery whitelist** — tap the button on Home screen.
2. **Brand-specific kill protection** — see [SETUP §6](SETUP.md#6-battery--oem-killers).
3. **Keep the phone plugged in** — most OEMs relax kill rules on charging devices.
4. **Disable Wi-Fi power save** in Wi-Fi settings.
5. **Lock the app in recents** (slide down on the recents card on most launchers).

## Backend can reach the gateway but the test website can't

CORS issue. Set **Allowed origins** in Settings to include your website's origin (e.g. `http://localhost:5500`), or temporarily `*` while you debug.

## Verify works locally but fails through ngrok / Cloudflare Tunnel

Some tunnels strip the `x-api-key` header by default. Try with `Authorization: Bearer sk_…` instead — it survives more proxies.

## Phone reboots, server doesn't auto-start

Auto-start-on-boot is intentionally not enabled by default (battery, privacy). When you reboot, open the app once. (A future version may add an opt-in toggle.)

## Compiled the app from source but APK won't install

- "App not installed" + you have an older debug build installed → uninstall the previous version first.
- Signature mismatch → uninstall, reinstall.
- Min SDK is 26 (Android 8.0). Older phones won't install.

## Anything else

Open an issue with:
- Phone model + Android version
- Steps to reproduce
- Output of `adb logcat -d -s OtpHttpServer:* OtpService:* SmsSender:*`
- (If applicable) the offending API request as a `curl` line
