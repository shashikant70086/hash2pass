# hash2pass

> Turn an Android phone into a self-hosted OTP service. SIM-based SMS, an HTTP API, no Firebase, no Twilio, no monthly bill.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7f52ff?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.12-orange?logo=ktor)](https://ktor.io)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

```
┌──────────────────────┐    HTTP    ┌────────────────────────┐    SMS    ┌───────────┐
│ Your backend / app   ├───────────►│ This app (Ktor on phone)├──────────►│ User SIM  │
└──────────────────────┘            └────────────────────────┘            └───────────┘
                                            │
                                            └── env-style settings on-device
```

## Why

Building auth for a side project or college app and don't want:

- **A Twilio bill** (₹0.20–₹0.80 per OTP × thousands of users = real money)
- **A Firebase Blaze plan** (required for Phone Auth — billing-enabled card needed)
- **A PC running 24/7** as the SMS gateway

Plug in any Android phone with an active SIM and a Wi-Fi connection. The app exposes a tiny HTTP API your backend can call. SMS goes out via the phone's `SmsManager`.

## What you get

- `POST /v1/otp/request` and `POST /v1/otp/verify` — the entire surface area
- Bcrypt-hashed OTPs, never stored in plaintext
- Per-phone and per-API-key rate limits (configurable)
- API key auth (`x-api-key` or `Authorization: Bearer`)
- One Settings screen that mirrors an `.env` file — port, OTP TTL, SMS template pool, rate limits, CORS
- Foreground service so the phone stays the gateway through Doze and OEM kill
- Delivery log of the last 100 sends, on-device
- Designed to also be reachable through ngrok / Cloudflare Tunnel / Tailscale for public access

## Quick start (3 minutes)

1. **Install** the APK on a phone with an active SIM ([docs/SETUP.md](docs/SETUP.md)).
2. **Open** the app, grant SMS + Notifications, tap *Start server*.
3. **Copy** the Base URL (`http://192.168.x.x:8787`) and API key (`sk_…`) from the Home screen.
4. From any device on the same Wi-Fi:
   ```bash
   curl -X POST http://192.168.x.x:8787/v1/otp/request \
     -H "x-api-key: sk_…" \
     -H "content-type: application/json" \
     -d '{"phone":"+919876543210"}'
   ```
5. SMS arrives. Verify with the returned `requestId`:
   ```bash
   curl -X POST http://192.168.x.x:8787/v1/otp/verify \
     -H "x-api-key: sk_…" \
     -H "content-type: application/json" \
     -d '{"requestId":"req_…","code":"123456"}'
   ```

That's it. There's no relay server, no FCM, no cloud account.

## Docs

| Doc | What it covers |
|---|---|
| [SETUP](docs/SETUP.md) | Install, permissions, first-run checklist |
| [API](docs/API.md) | Every endpoint, request/response, errors |
| [INTEGRATION](docs/INTEGRATION.md) | cURL, Node, Python, PHP, Go, Kotlin, Swift snippets |
| [CONFIG](docs/CONFIG.md) | All Settings fields explained, defaults, what to tune |
| [DEPLOYMENT](docs/DEPLOYMENT.md) | Going beyond LAN — tunnels, static IP, hosting backends |
| [SECURITY](docs/SECURITY.md) | Threat model, what's safe, what to harden |
| [TROUBLESHOOTING](docs/TROUBLESHOOTING.md) | Common errors and fixes |
| [FAQ](docs/FAQ.md) | Honest answers about DLT, billing, scale, alternatives |

## Status

Beta. The wire protocol is stable; UI may still change.
Issues + PRs welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache 2.0](./LICENSE)
