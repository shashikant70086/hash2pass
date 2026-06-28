# hash2pass — Tester

Single-page tester for the standalone hash2pass Android app. Talks to the phone over HTTP. No server, no build step, no install.

## Use it

Pick whichever is easiest:

**Just open the file**
Double-click `index.html`. Works in Chrome/Edge/Firefox/Safari.

**Or serve it (recommended on Windows)**
```cmd
cd C:\Users\shash\Downloads\otp-gateway-tester
python -m http.server 5500
```
Open `http://localhost:5500`.

**Or from any machine on the same Wi-Fi**
Same `python -m http.server` command, then open `http://<your-pc-ip>:5500` from your laptop / second phone.

## What you need before testing

1. Gateway app installed and running on a phone (you'll see "Running" + a URL on the Home screen).
2. The phone's **Base URL** (e.g. `http://192.168.29.120:8787`).
3. The phone's **API key** (Home screen → copy icon next to the masked key).
4. The tester device on the **same Wi-Fi** as the gateway phone.

## Flow

1. Paste Base URL + API key → **Save** → **Check connection** (should turn green and show config).
2. Enter a phone in E.164 (`+91…`) → **Send OTP** → SMS arrives on that number.
3. Enter the code → **Verify** → ✓ Verified.

The bottom card shows the cURL + JSON of every call you just made, ready to paste into your real backend.

## Integration snippets

The page has **Copy cURL** and **Copy fetch (JS)** buttons that pre-fill the snippet with your current Base URL and API key. Drop them into your backend, replace `$PHONE` / `req_…`, and you're integrated.

### One-paragraph backend recipe

> POST `/v1/otp/request` with `{phone}`. Save the returned `requestId` against the user's session. When they submit the code, POST `/v1/otp/verify` with `{requestId, code}`. Treat `{ok:true}` as "phone proven", issue your own session token, never trust the gateway's response for anything else.

## Gotchas

- **Mixed content**: if you host the tester on HTTPS, browsers will block calls to `http://192.168.x.x`. Either host the tester over HTTP, or open the local file directly.
- **CORS**: the gateway app's default CORS is `*`. Tighten in Settings → CORS once you know your real origins.
- **Phone IP changes** when it switches networks. The Home screen always shows the current URL.
- **Public access**: for use outside your LAN, set a `Public base URL` in the gateway Settings and point a tunnel (ngrok, Cloudflare Tunnel, Tailscale Funnel) at `http://localhost:8787` on the phone — out of scope here.
