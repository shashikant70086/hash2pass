# hash2pass // OTP Gateway

> Turn an Android phone into a self-hosted OTP service and test gateway. SIM-based SMS, an HTTP API, no Firebase, no Twilio, no monthly bill.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./otp-gateway-android/LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7f52ff?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.12-orange?logo=ktor)](https://ktor.io)
[![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Glossary/HTML5)

**hash2pass** is an open-source project that allows developers to avoid expensive SMS verification bills by using any Android phone with an active SIM card as a dedicated SMS gateway. It exposes a simple REST API that your backend can call to send and verify OTPs, and provides a web-based dashboard for live telemetry and testing.

## System Architecture

```text
+------------------------+             +-------------------------+
|   Android Device       |             |   Testing Web Client    |
|   (hash2pass App)      |             |   (Local / Cloud)       |
+-----------+------------+             +------------+------------+
            |                                       ^
            | (Intercepts SMS / Notif)              | (Receives Payload)
            v                                       |
+-----------+---------------------------------------+------------+
|                 OTP EXTRACTION ENGINE                          |
|        - Regex Pattern Matching & Payload Formatting           |
+---------------------------+------------------------------------+
                            |
                            | (HTTP POST JSON)
                            v
                   +--------+--------+
                   | Target Endpoint |
                   +-----------------+
```

## Why use hash2pass?

Building auth for a side project or college app and don't want:
- **A Twilio bill** (SMS costs add up quickly for thousands of users)
- **A Firebase Blaze plan** (Required for Phone Auth)
- **A PC running 24/7** to act as a gateway

Just plug in any Android phone with a Wi-Fi connection and an active SIM. The app exposes a tiny HTTP API. SMS goes out via the phone's native `SmsManager`. 

## Project Structure

This repository is divided into two main components:

### 1. hash2pass Android App (`otp-gateway-android`)
The core Android application that you install on your phone. It runs a lightweight HTTP server (Ktor) and handles sending SMS messages and validating OTP codes securely.
- `POST /v1/otp/request` and `POST /v1/otp/verify` — the entire surface area.
- Bcrypt-hashed OTPs, never stored in plaintext.
- Per-phone and per-API-key rate limits (configurable).
- API key auth (`x-api-key` or `Authorization: Bearer`).
- Foreground service so the phone stays the gateway through Doze and OEM kill.

[**→ View Android App Documentation**](./otp-gateway-android/README.md)

### 2. hash2pass Tester (`otp-gateway-tester` & `docs`)
A lightweight, single-page web interface to easily test your Android gateway over the local network without writing any code. Also includes a live telemetry dashboard hosted on GitHub Pages.
- Dynamic Timeline of commits.
- System configurations and API payload templates.
- Easy testing interface for sending OTP requests.

[**→ View Web Dashboard Documentation**](./otp-gateway-tester/README.md)

## Quick Start (3 minutes)

1. **Install** the APK on a phone with an active SIM.
2. **Open** the app, grant SMS + Notifications permissions, and tap *Start server*.
3. **Copy** the Base URL (`http://192.168.x.x:8787`) and API key (`sk_…`) from the Home screen.
4. **Test** the gateway from any device on the same Wi-Fi:
   ```bash
   curl -X POST http://192.168.x.x:8787/v1/otp/request \
     -H "x-api-key: sk_…" \
     -H "content-type: application/json" \
     -d '{"phone":"+919876543210"}'
   ```
5. **Verify** the received SMS using the `requestId`:
   ```bash
   curl -X POST http://192.168.x.x:8787/v1/otp/verify \
     -H "x-api-key: sk_…" \
     -H "content-type: application/json" \
     -d '{"requestId":"req_…","code":"123456"}'
   ```

## Security & Privacy

Since **hash2pass** runs entirely on your own hardware, your data stays yours. 
- OTPs are hashed using bcrypt before being stored in memory.
- There is no central cloud server that processes your messages.
- You have full control over rate limiting and API key revocation.
- Designed to be reachable through ngrok, Cloudflare Tunnel, or Tailscale for secure public access.

## Contributing

We welcome contributions! Whether it's adding new features, improving the UI, or fixing bugs, your help is appreciated. Please see our [Contributing Guide](./CONTRIBUTING.md) for more details.

## License

This project is open-source and licensed under the [Apache 2.0 License](./otp-gateway-android/LICENSE).
