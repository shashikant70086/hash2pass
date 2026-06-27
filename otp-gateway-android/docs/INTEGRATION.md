# Integration

Copy-paste recipes for the two endpoints that matter. All examples assume:

```
BASE = http://192.168.x.x:8787   (or your tunnel URL in prod)
KEY  = sk_…                       (from the Home screen)
```

## The 2-step flow

1. User types phone → your backend calls `/v1/otp/request` → store `requestId` against the user's session.
2. User types code → your backend calls `/v1/otp/verify` with `{requestId, code}` → if `ok:true`, mark phone proven and issue your own session token.

## cURL

```bash
# Request
curl -X POST "$BASE/v1/otp/request" \
  -H "x-api-key: $KEY" \
  -H "content-type: application/json" \
  -d '{"phone":"+919876543210"}'

# Verify
curl -X POST "$BASE/v1/otp/verify" \
  -H "x-api-key: $KEY" \
  -H "content-type: application/json" \
  -d '{"requestId":"req_…","code":"123456"}'
```

## Node.js (fetch — Node 18+)

```javascript
const BASE = process.env.OTP_BASE_URL;
const KEY  = process.env.OTP_API_KEY;

async function call(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'x-api-key': KEY, 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
  return { status: res.status, body: await res.json().catch(() => ({})) };
}

export async function requestOtp(phone) {
  const r = await call('/v1/otp/request', { phone });
  if (r.status !== 200) throw Object.assign(new Error(r.body.error || 'request failed'), { status: r.status });
  return r.body; // { requestId, expiresInSeconds }
}

export async function verifyOtp(requestId, code) {
  const r = await call('/v1/otp/verify', { requestId, code });
  return r.body.ok === true; // boolean
}
```

### Express route wired up

```javascript
import express from 'express';
import { requestOtp, verifyOtp } from './otp.js';

const app = express();
app.use(express.json());

app.post('/auth/start', async (req, res) => {
  try {
    const { phone } = req.body;
    const { requestId, expiresInSeconds } = await requestOtp(phone);
    req.session.pendingOtp = { requestId, phone };
    res.json({ expiresInSeconds });
  } catch (e) {
    res.status(e.status || 500).json({ error: e.message });
  }
});

app.post('/auth/confirm', async (req, res) => {
  const { code } = req.body;
  const pending = req.session.pendingOtp;
  if (!pending) return res.status(400).json({ error: 'no pending OTP' });
  if (!(await verifyOtp(pending.requestId, code))) return res.status(401).json({ error: 'wrong code' });
  delete req.session.pendingOtp;
  req.session.userPhone = pending.phone;
  res.json({ ok: true });
});

app.listen(3000);
```

## Python (requests)

```python
import os, requests

BASE = os.environ["OTP_BASE_URL"]
KEY  = os.environ["OTP_API_KEY"]
H = {"x-api-key": KEY, "content-type": "application/json"}

def request_otp(phone: str) -> dict:
    r = requests.post(f"{BASE}/v1/otp/request", json={"phone": phone}, headers=H, timeout=15)
    r.raise_for_status()
    return r.json()  # {"requestId": "...", "expiresInSeconds": 300}

def verify_otp(request_id: str, code: str) -> bool:
    r = requests.post(f"{BASE}/v1/otp/verify", json={"requestId": request_id, "code": code}, headers=H, timeout=15)
    if r.status_code == 200:
        return r.json().get("ok") is True
    return False
```

### FastAPI route

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from .otp import request_otp, verify_otp

app = FastAPI()
PENDING = {}  # swap for Redis in prod

class StartIn(BaseModel): phone: str
class ConfirmIn(BaseModel): phone: str; code: str

@app.post("/auth/start")
def start(body: StartIn):
    r = request_otp(body.phone)
    PENDING[body.phone] = r["requestId"]
    return {"expiresInSeconds": r["expiresInSeconds"]}

@app.post("/auth/confirm")
def confirm(body: ConfirmIn):
    req_id = PENDING.get(body.phone)
    if not req_id: raise HTTPException(400, "no pending OTP")
    if not verify_otp(req_id, body.code): raise HTTPException(401, "wrong code")
    PENDING.pop(body.phone, None)
    return {"ok": True}
```

## PHP

```php
<?php
function otpCall(string $path, array $body): array {
    $base = getenv('OTP_BASE_URL');
    $key  = getenv('OTP_API_KEY');
    $ch = curl_init($base . $path);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_HTTPHEADER => ['x-api-key: ' . $key, 'content-type: application/json'],
        CURLOPT_POSTFIELDS => json_encode($body),
        CURLOPT_TIMEOUT => 15,
    ]);
    $raw = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['status' => $status, 'body' => json_decode($raw, true) ?: []];
}

function requestOtp(string $phone): array {
    $r = otpCall('/v1/otp/request', ['phone' => $phone]);
    if ($r['status'] !== 200) throw new Exception($r['body']['error'] ?? 'request failed');
    return $r['body'];
}

function verifyOtp(string $requestId, string $code): bool {
    $r = otpCall('/v1/otp/verify', ['requestId' => $requestId, 'code' => $code]);
    return ($r['body']['ok'] ?? false) === true;
}
```

## Go

```go
package otp

import (
    "bytes"
    "encoding/json"
    "fmt"
    "net/http"
    "os"
    "time"
)

var (
    base   = os.Getenv("OTP_BASE_URL")
    apiKey = os.Getenv("OTP_API_KEY")
    client = &http.Client{Timeout: 15 * time.Second}
)

func call(path string, body any) (int, map[string]any, error) {
    b, _ := json.Marshal(body)
    req, _ := http.NewRequest("POST", base+path, bytes.NewReader(b))
    req.Header.Set("x-api-key", apiKey)
    req.Header.Set("content-type", "application/json")
    res, err := client.Do(req)
    if err != nil { return 0, nil, err }
    defer res.Body.Close()
    var out map[string]any
    _ = json.NewDecoder(res.Body).Decode(&out)
    return res.StatusCode, out, nil
}

type RequestResp struct{ RequestID string; ExpiresInSeconds int }

func Request(phone string) (*RequestResp, error) {
    s, b, err := call("/v1/otp/request", map[string]string{"phone": phone})
    if err != nil { return nil, err }
    if s != 200 { return nil, fmt.Errorf("request: %v", b["error"]) }
    return &RequestResp{
        RequestID:        b["requestId"].(string),
        ExpiresInSeconds: int(b["expiresInSeconds"].(float64)),
    }, nil
}

func Verify(requestID, code string) (bool, error) {
    _, b, err := call("/v1/otp/verify", map[string]string{"requestId": requestID, "code": code})
    if err != nil { return false, err }
    ok, _ := b["ok"].(bool)
    return ok, nil
}
```

## Kotlin / Android (OkHttp)

> If you're integrating from another Android app that you wrote. For the print-atm-student app see its own `PhoneAuthClient.kt`.

```kotlin
class OtpClient(
    private val base: String,
    private val apiKey: String,
    private val http: OkHttpClient = OkHttpClient()
) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun requestOtp(phoneE164: String): Result<Pair<String, Int>> = runCatching {
        val json = post("/v1/otp/request", JSONObject().put("phone", phoneE164))
        json.getString("requestId") to json.getInt("expiresInSeconds")
    }

    suspend fun verifyOtp(requestId: String, code: String): Boolean {
        val json = runCatching {
            post("/v1/otp/verify", JSONObject().put("requestId", requestId).put("code", code))
        }.getOrNull() ?: return false
        return json.optBoolean("ok", false)
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url("$base$path")
            .post(body.toString().toRequestBody(JSON))
            .header("x-api-key", apiKey)
            .build()
        http.newCall(req).execute().use { resp ->
            return JSONObject(resp.body?.string().orEmpty().ifBlank { "{}" })
        }
    }
}
```

## Swift / iOS

```swift
import Foundation

struct OtpClient {
    let base: URL
    let apiKey: String
    let session: URLSession = .shared

    func requestOtp(phoneE164: String) async throws -> (requestId: String, expiresInSeconds: Int) {
        let body = ["phone": phoneE164]
        let json = try await post("/v1/otp/request", body)
        guard let id = json["requestId"] as? String,
              let ttl = json["expiresInSeconds"] as? Int else { throw URLError(.badServerResponse) }
        return (id, ttl)
    }

    func verifyOtp(requestId: String, code: String) async -> Bool {
        let body = ["requestId": requestId, "code": code]
        let json = (try? await post("/v1/otp/verify", body)) ?? [:]
        return (json["ok"] as? Bool) == true
    }

    private func post(_ path: String, _ body: [String: String]) async throws -> [String: Any] {
        var req = URLRequest(url: base.appendingPathComponent(path))
        req.httpMethod = "POST"
        req.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        req.setValue("application/json", forHTTPHeaderField: "content-type")
        req.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, _) = try await session.data(for: req)
        return (try JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
    }
}
```

## Common patterns

### Where do I store `requestId`?

Anywhere session-y:
- Server-side session (Redis, encrypted cookie, your DB)
- In-memory Map keyed by user/phone (fine for dev)
- Never on the client — a malicious client could pass any `requestId` and re-attempt forever (well, until the OTP expires or attempts run out).

### Should the app talk to the gateway directly?

**No** for any production app — that puts your API key in the APK, where it can be extracted with `apktool`. Run a backend in front; the backend holds the key, the app talks to the backend.

For a college project / closed beta / personal dev tool, direct-from-app is fine and saves you a backend.

### How do I rate-limit per user?

The gateway rate-limits per phone and per API key globally. For per-user limits, do it in your backend before calling the gateway.

### Should I retry on 502?

Yes, **once**, after a 2–3 second delay. `502` usually means SMS layer failed transiently. More than one retry is just spam.

### Where do I get tunnel URLs?

See [DEPLOYMENT.md](DEPLOYMENT.md).
