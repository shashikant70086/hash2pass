# Changelog

All notable changes to this project will be documented here. Follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Roadmap (not promises)

- WhatsApp Cloud API channel (server-to-server, sketched in conversations)
- Multi-template SMS pool with no-repeat picker
- Webhook for delivery status
- QR-code pair flow for non-technical devs
- Built-in basic monitoring dashboard
- Auto-start-on-boot toggle
- Receive-SMS support (auto-fill OTP into your app via SMS Retriever)

## [0.2.0] — 2026-06-11

Major architectural pivot — the phone is now the gateway.

### Added
- Embedded Ktor HTTP server (`CIO` engine) running on the phone
- `POST /v1/otp/request` and `POST /v1/otp/verify` endpoints
- `GET /v1/info` and `GET /healthz` introspection
- API key authentication (`x-api-key` / `Authorization: Bearer`)
- bcrypt-hashed OTP storage (via `at.favre.lib:bcrypt`)
- Per-phone-per-hour and per-key-per-minute rate limits
- Settings screen mirroring an env file: port, API key, OTP digits, TTL, SMS template, limits, CORS
- API key auto-generation on first launch + rotate button
- Public Base URL field for tunnel deployments
- Tester website (`otp-gateway-tester/index.html`) — single page, no server
- Full docs tree: SETUP, API, INTEGRATION, CONFIG, DEPLOYMENT, SECURITY, TROUBLESHOOTING, FAQ

### Removed
- FCM (Firebase Cloud Messaging) dependency
- `google-services` plugin
- PC-side relay server requirement
- Pair screen and pair-code flow (no longer needed without a relay)

### Changed
- Database schema bumped to v2 (added `otp_pending` table)
- Foreground service now owns the HTTP server, not just a notification

## [0.1.0] — 2026-06-10

Initial release.

### Added
- Pair-with-relay flow (FCM-based)
- FCM-triggered SMS sending via `SmsManager`
- Foreground service for process longevity
- Room-backed send log
- Hilt DI throughout
- Companion `otp-gateway-relay` (Node/Express + Firebase Admin)

[Unreleased]: ../../compare/v0.2.0...HEAD
[0.2.0]: ../../compare/v0.1.0...v0.2.0
[0.1.0]: ../../releases/tag/v0.1.0
