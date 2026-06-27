# Configuration Reference

Every Settings field, explained. All values are persisted on-device via DataStore and survive restarts. There is no `.env` file — the Settings screen *is* the env file.

## Server

### `Port`
- **Default**: `8787`
- **Range**: `1`–`65535`
- The port the embedded HTTP server binds to on `0.0.0.0`.
- **Restart server** after changing.
- Common alternates: `3000`, `8080`, `9000`. Don't pick `<1024` — Android won't let unprivileged processes bind.

### `Public base URL`
- **Default**: empty
- The HTTPS URL your tunnel publishes (e.g. `https://otp.your-tunnel.app`).
- The Home screen displays this as the "primary" URL when set.
- It's display-only — the server itself still listens on `0.0.0.0:port` locally. The tunnel does the public ↔ local mapping.
- Leave blank for LAN-only use.

## Auth

### `API key`
- **Default**: auto-generated on first launch in the form `sk_<43-char base64url>`.
- 32 bytes of cryptographic randomness — strong by construction.
- Tap the **🔄 rotate** icon to invalidate the current key and generate a new one. **All clients lose access until updated.**
- You can also paste a custom key (e.g. shared with your team). Keep it ≥ 32 chars.

## OTP

### `Digits`
- **Default**: `6`
- **Range**: `4`–`8`
- Length of the OTP. 6 is the standard. 4 is friendlier for elderly users but ~100× weaker. 8 is enterprise-grade.

### `TTL (seconds)`
- **Default**: `300` (5 minutes)
- **Range**: `30`–`1800`
- How long an OTP is valid for. Tighter is more secure but more annoying.
- Recommended: 120 (high-security), 300 (default), 600 (rural / slow SMS networks).

### `SMS template`
- **Default**: `Your verification code is {otp}. It expires in {ttl_min} min.`
- Placeholders: `{otp}` → the code, `{ttl_min}` → TTL converted to whole minutes.
- See the **Multi-template pool** note below for randomized variants (anti-filtering).
- **Avoid these words** for Indian DLT compliance: *OTP, verification, password, login, KYC, bank, transaction*. They get pattern-matched by carriers.

#### Multi-template pool (recommended)

If you make this field multi-line, the app picks a random line per send. Useful when carriers are dropping repeated messages.

Example pool:
```
Your code is {otp} ({ttl_min}m to use it)
Code: {otp} — valid for {ttl_min} min
Use {otp} to continue. Expires in {ttl_min}m.
{otp} is your code. Don't share it. ({ttl_min}m)
```

## Limits

### `Max OTP requests per phone per hour`
- **Default**: `5`
- **Range**: `1`–`100`
- A single phone number can only request this many OTPs per rolling hour. Protects your SIM credit and the user from accidental spam.

### `Max API calls per key per minute`
- **Default**: `30`
- **Range**: `1`–`1000`
- Global throttle for an API key. Catches misconfigured backends or abuse.

### `Max verify attempts per OTP`
- **Default**: `5`
- **Range**: `1`–`20`
- Wrong-code attempts allowed before the pending OTP is purged. Set to `3` for high security.

## CORS

### `Allowed origins`
- **Default**: `*` (any origin)
- Comma-separated list of allowed browser origins for CORS.
- Example: `https://app.printatm.in,https://admin.printatm.in`
- `*` is fine for LAN dev. **Set explicit origins before exposing publicly.**

## How settings are applied

| Field | Takes effect |
|---|---|
| API key, OTP digits, TTL, SMS template, rate limits, max verify attempts | Immediately on Save (next request uses the new value) |
| Port, CORS allowed origins | Only after **Stop server → Start server** |
| Public base URL | Display-only, no restart needed |

## Resetting

The **Reset defaults** button in Settings restores every value above except the API key (so you don't lock yourself out of integrations). To also rotate the key, tap the 🔄 next to it.

To wipe everything (key included), uninstall + reinstall the app.

## Backup / restore

There's no export today. If your phone dies, you'll lose:
- The API key (just generate a new one — easy)
- The send log (last 100 sends, on-device)
- Pending OTPs (in-flight requests — users just request fresh codes)

User accounts, sessions, JWTs etc. live in *your backend* and are unaffected.
