# API Reference

Base URL is whatever the Home screen shows. All bodies are JSON. All endpoints (except `/healthz`) require an API key.

## Authentication

Send your key in either header on every request:

```
x-api-key: sk_…
```
or
```
Authorization: Bearer sk_…
```

Wrong / missing key → `401 Unauthorized` with `{"error":"unauthorized"}`.

## Endpoints

### `GET /healthz`

Liveness probe. No auth.

```http
GET /healthz
```

**200 OK**
```json
{ "ok": true }
```

Use this from your monitoring (UptimeRobot, BetterStack, your own ping).

---

### `GET /v1/info`

Gateway capabilities and current limits. Requires API key.

```http
GET /v1/info
x-api-key: sk_…
```

**200 OK**
```json
{
  "version": "0.2.0",
  "otpDigits": 6,
  "otpTtlSeconds": 300,
  "rateLimitPerPhonePerHour": 5,
  "rateLimitPerKeyPerMinute": 30
}
```

Useful for showing UI labels ("Codes valid for 5 minutes") without hardcoding.

---

### `POST /v1/otp/request`

Generate a new OTP and send it via SMS to a phone number.

```http
POST /v1/otp/request
x-api-key: sk_…
Content-Type: application/json

{ "phone": "+919876543210" }
```

#### Request body

| Field | Type | Required | Notes |
|---|---|---|---|
| `phone` | string | yes | **E.164 format**: starts with `+`, country code, no spaces or dashes. Examples: `+919876543210`, `+14155552671`. |

#### **200 OK** — OTP sent

```json
{
  "requestId": "req_aB3xK9q…",
  "expiresInSeconds": 300
}
```

Save `requestId` against the user's session — you'll need it for verify. **The OTP itself is never returned in the response** (it's only in the SMS).

#### Error responses

| Status | Body | Means |
|---|---|---|
| `400` | `{"error":"phone must be E.164, e.g. +919876543210"}` | Bad phone format |
| `400` | `{"error":"invalid body"}` | Malformed JSON / missing `phone` |
| `401` | `{"error":"unauthorized"}` | Missing or wrong API key |
| `429` | `{"error":"rate limit exceeded for this phone"}` | This phone hit the per-phone-per-hour limit |
| `429` | `{"error":"global rate limit exceeded"}` | Your API key hit the per-key-per-minute limit |
| `502` | `{"error":"sms send failed (code N)"}` | Carrier/SIM rejected the SMS. Common codes: 1=generic, 2=radio off, 4=no service. See [TROUBLESHOOTING](TROUBLESHOOTING.md). |

---

### `POST /v1/otp/verify`

Check a code submitted by the user.

```http
POST /v1/otp/verify
x-api-key: sk_…
Content-Type: application/json

{ "requestId": "req_aB3xK9q…", "code": "123456" }
```

#### Request body

| Field | Type | Required | Notes |
|---|---|---|---|
| `requestId` | string | yes | The `requestId` from `/v1/otp/request`. Single-use. |
| `code` | string | yes | Digits only. Length matches your configured `otpDigits`. |

#### **200 OK** — verified

```json
{ "ok": true }
```

`requestId` is consumed — a second verify on the same `requestId` returns `404`.

#### Error responses

| Status | Body | Means |
|---|---|---|
| `400` | `{"error":"invalid body"}` | Malformed JSON / missing fields |
| `401` | `{"ok":false,"reason":"wrong code"}` | Code didn't match. The pending OTP is **kept** so the user can retry (up to `maxVerifyAttempts`). |
| `404` | `{"ok":false,"reason":"unknown request"}` | `requestId` doesn't exist (typo, already verified, or expired and purged). |
| `410` | `{"ok":false,"reason":"expired"}` | OTP TTL elapsed. Pending entry is purged — user must request a new one. |
| `429` | `{"ok":false,"reason":"too many attempts"}` | Exceeded `maxVerifyAttempts`. Pending entry is purged — user must request a new one. |

## Status code summary

| Code | Meaning |
|---|---|
| `200` | Success |
| `400` | Client error (bad input) |
| `401` | Authentication failure (key) or verify failure (wrong code) |
| `404` | Unknown request id |
| `410` | OTP expired |
| `429` | Rate limited or too many attempts |
| `500` | Internal error — open an issue with logs |
| `502` | SMS layer failure (SIM / carrier) |

## Rate limits

Two layers, both configurable in Settings:

1. **Per phone per hour** — protects users from being spammed. Default 5.
2. **Per API key per minute** — protects against abuse. Default 30.

Rate-limit responses use `429`. There's no `Retry-After` header today — wait the window out. (Open issue if you need it.)

## Request lifecycle

```
client                     gateway                       SIM / carrier
  │                           │                                │
  │ POST /v1/otp/request      │                                │
  │──────────────────────────►│ generate 6-digit OTP           │
  │                           │ bcrypt-hash it                 │
  │                           │ store pending row              │
  │                           │ SmsManager.sendTextMessage ───►│
  │                           │◄── delivery PendingIntent ─────│
  │ {requestId, expiresIn}    │                                │
  │◄──────────────────────────│                                │
  │                                                            │
  │   (user reads SMS, types code)                             │
  │                                                            │
  │ POST /v1/otp/verify       │                                │
  │──────────────────────────►│ fetch pending row              │
  │                           │ bcrypt.compare(code, hash)     │
  │                           │ on match: delete pending row   │
  │ {ok: true}                │                                │
  │◄──────────────────────────│                                │
```

## What's NOT in the API

By design:

- No "list all OTPs" — surveillance hazard.
- No "show me the OTP" — defeats hashing.
- No user/session/JWT endpoints — that belongs in your backend.
- No webhooks — pull, don't push, for v1.

If your use case needs any of these, build them in your own backend layer in front of this gateway.
