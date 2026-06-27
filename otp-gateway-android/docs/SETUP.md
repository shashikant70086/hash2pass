# Setup

End-to-end first-run checklist. ~10 minutes.

## What you need

| Thing | Why |
|---|---|
| An Android phone (Android 8+ / API 26+) | Runs the gateway |
| An active SIM in that phone | Sends the SMS |
| Wi-Fi (preferably 5 GHz, same network as your dev machine) | Phone↔backend communication |
| A way to install an APK (sideload or Android Studio) | No Play Store distribution yet |
| 5 minutes to fiddle with battery/OEM settings | OEMs love killing background services |

## 1. Install the app

Two options:

### From source (recommended for dev)
```bash
git clone <this-repo>
cd otp-gateway-android
./gradlew :app:installDebug
```
Phone must be in **USB debugging** mode (Settings → About → tap *Build number* 7 times → Developer options → USB debugging).

### From a built APK
Drop the APK into the phone's storage and tap it. You'll need to allow "Install unknown apps" for whatever file manager you used.

## 2. First launch — permissions

When you open the app the first time, Android will ask for:

| Permission | Required? | Why |
|---|---|---|
| **Send SMS** | ✅ Yes | The whole point. Deny = nothing works. |
| **Post notifications** | ✅ Yes | Foreground-service notification + status visibility (Android 13+). |
| **Disable battery optimization** | Strongly recommended | Doze mode kills the embedded HTTP server otherwise. Settings screen has a button for this. |

If you accidentally deny SMS, fix it in **Phone Settings → Apps → OTP Gateway → Permissions**. Then restart the app.

## 3. First launch — the Home screen

You'll see:

```
●  Running                            ← green dot = server is up
   N OTPs sent today

Base URL                              ← the URL your backend will call
http://192.168.29.120:8787   📋

API key                               ← paste this into your backend's .env
sk_abcd…wxyz                 📋

[ Stop server ]
[ Disable battery optimization ]
```

The first time you open the app:
- The server **auto-starts** (`Running`).
- An **API key is auto-generated** in the form `sk_…`. You can rotate it any time in Settings.
- The base URL is your phone's **LAN IP** + port `8787`.

If you don't see `Running`, tap **Start server**. If `Base URL` says `(no Wi-Fi)`, connect to Wi-Fi.

## 4. Verify it works (without writing any code)

From a laptop on the same Wi-Fi:

```bash
curl http://192.168.29.120:8787/healthz
# → {"ok":true}
```

Then with your API key:

```bash
curl -X POST http://192.168.29.120:8787/v1/otp/request \
  -H "x-api-key: sk_abcd…wxyz" \
  -H "content-type: application/json" \
  -d '{"phone":"+919876543210"}'
# → {"requestId":"req_xxx…","expiresInSeconds":300}
```

SMS should land on `+919876543210` within ~10 seconds. Then:

```bash
curl -X POST http://192.168.29.120:8787/v1/otp/verify \
  -H "x-api-key: sk_abcd…wxyz" \
  -H "content-type: application/json" \
  -d '{"requestId":"req_xxx…","code":"123456"}'
# → {"ok":true}
```

If you don't have a backend yet, use the standalone tester website included in the project: `otp-gateway-tester/index.html`.

## 5. Tweak Settings (optional)

Tap **Settings** from the Home screen:

| Setting | Default | When to change |
|---|---|---|
| Port | 8787 | Conflicts with other server / want a memorable port |
| Public base URL | empty | If you front the phone with a tunnel (see [DEPLOYMENT](DEPLOYMENT.md)) |
| API key | auto-generated | Rotate after sharing it / suspected leak |
| OTP digits | 6 | Banking-grade → 8; casual → 4 |
| TTL (seconds) | 300 (5 min) | Tighten to 120 for higher security, loosen to 600 for slow SMS networks |
| SMS templates | see [CONFIG](CONFIG.md) | Match your brand voice |
| Rate limits | 5/phone/hr, 30/key/min | Tighten if abuse, loosen if you're hitting them during legit use |
| CORS origins | `*` | Lock to your domain(s) before going public |

After changing **Port** or **CORS**, you must **Stop server → Start server** for it to take effect. Other settings apply immediately on save.

## 6. Battery + OEM killers

The single biggest cause of "it worked yesterday, now it's down" is the OS killing the background service. Defenses:

1. **Battery whitelist** — tap the button on Home. Required.
2. **Brand-specific kill protection**:
   - Xiaomi / Redmi / POCO → Settings → Apps → OTP Gateway → Battery saver → *No restrictions*. Also enable *Autostart*.
   - OPPO / Realme → Settings → Battery → *Allow background activity*.
   - Vivo → Settings → Battery → Background power consumption management → *Allow*.
   - Samsung → Settings → Apps → OTP Gateway → Battery → *Unrestricted*.
   - OnePlus → Settings → Battery → Battery optimization → OTP Gateway → *Don't optimize*.
   - Stock Android (Pixel) — the battery-whitelist button is enough.
3. **Charger plugged in** if it's a dedicated gateway device — many OEMs are gentler when charging.
4. **Disable Wi-Fi power saving** (Settings → Wi-Fi → ⚙ → Wi-Fi power save) if you see the gateway becoming unreachable after a few minutes of idle traffic.

## 7. Hook up your backend

Plenty of recipes in [INTEGRATION.md](INTEGRATION.md). The contract is tiny:

- `POST /v1/otp/request` → save the `requestId` for the user
- `POST /v1/otp/verify` → on `{ok:true}`, mark phone proven

Use the returned `requestId` as the binding between *request* and *verify* — don't trust client-supplied request IDs.

## Done

You should now have a working gateway and a verified OTP flow. From here:

- [API.md](API.md) — exact endpoint reference
- [INTEGRATION.md](INTEGRATION.md) — wire up your backend or app
- [DEPLOYMENT.md](DEPLOYMENT.md) — when you need a public URL
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — when something goes wrong
