# Deployment

Going beyond "phone and laptop on the same Wi-Fi". Covers tunneling, static addresses, and where to host the calling backend.

## Topology choices

```
A. LAN only
   user → backend(LAN) → phone(LAN)
   • zero infra, zero cost
   • only reachable from the same Wi-Fi
   • good for: college projects, internal tools, kiosks

B. Phone fronted by a tunnel
   user → backend(cloud) → tunnel.example.com → phone
   • public reach, no port-forwarding
   • backend can live anywhere
   • good for: side projects, beta launches

C. Direct from the app (no backend)
   user → phone (via tunnel)
   • simplest possible
   • API key embedded in the client app — extractable
   • good for: closed demos, classroom apps

D. Phone behind your VPN (Tailscale / WireGuard)
   user → backend(VPS, in VPN) → phone(in VPN)
   • E2E private, no public exposure
   • requires VPN on both ends
   • good for: small teams, prosumer setups
```

## A. LAN only

You're already here after [SETUP](SETUP.md). Notes:

- Reserve a **static DHCP lease** for the phone in your router so the IP doesn't change. Look for "DHCP reservations" or "address binding" in your router admin.
- If the phone is a dedicated gateway, leave it plugged in 24/7. Set the screen to never sleep (Settings → Display → Sleep → Never).
- Block port `8787` from the WAN side at your router. You don't want the world dialing in.

## B. Phone fronted by a tunnel

Three good options. All let the phone keep its dynamic LAN IP while giving you a stable public HTTPS URL.

### Option 1: Cloudflare Tunnel (free, recommended)

Requirements: a domain on Cloudflare, plus a sidecar runner on the phone.

**Easiest path** — run the tunnel from a PC on the same LAN as the phone:

```bash
# On a PC always-on, on the same LAN as the phone
cloudflared tunnel login
cloudflared tunnel create otp-gateway
cloudflared tunnel route dns otp-gateway otp.example.com
cloudflared tunnel run --url http://192.168.x.x:8787 otp-gateway
```

Now `https://otp.example.com` ↔ `http://192.168.x.x:8787`. Put `https://otp.example.com` into the gateway's **Public base URL** field, and as your backend's `OTP_BASE_URL`.

If you don't have a 24/7 PC, run `cloudflared` inside Termux on the phone itself — search "cloudflared termux".

### Option 2: ngrok (5-minute easiest)

```bash
ngrok http http://192.168.x.x:8787
```

→ `https://abcd1234.ngrok.app`

Free tier rotates the URL every restart, which is annoying. The cheapest paid tier (~$10/mo) gives you a stable subdomain.

### Option 3: Tailscale Funnel (free for personal use)

If the phone has Tailscale installed (Play Store), Funnel exposes a HTTPS URL over the Tailnet to the public internet.

```bash
# On the phone (via Tailscale app), enable Funnel for port 8787
```

→ `https://your-phone.tailnet.ts.net`

No domain needed. Limit: personal accounts only.

### Don't bother with

- **Port forwarding on the router** — most ISP routers (esp. CGNAT in India) block it.
- **No-IP / DynDNS** — same CGNAT problem; only works if you have a public IPv4.
- **Localtunnel** — community-run, often down.

## C. Direct from the app

Wire the app to call the gateway directly. See [INTEGRATION.md](INTEGRATION.md) for client snippets.

- Bake `BASE` and `KEY` into your app via `BuildConfig.OTP_BASE_URL` and `BuildConfig.OTP_API_KEY`.
- Anyone with `apktool` can extract `KEY` from the APK. Accept this or use option B/D.
- Tighten the rate limits in Settings to ~5/min if you choose this path.

## D. Phone in a VPN

Most private option. Suitable when you only need a few known clients reaching the gateway.

- Install **Tailscale** on the phone and on each client (Mac/Win/Linux backend).
- The phone gets a stable Tailnet IP like `100.x.y.z`.
- Use `http://100.x.y.z:8787` as your backend's `OTP_BASE_URL`.

WireGuard works too if you don't want to depend on Tailscale's coordination.

## Hosting the calling backend

The gateway is the SMS-sending leaf. *Your* backend (the thing that owns sessions, JWTs, rate limits per user) goes anywhere normal Node/Python/Go runs:

| Host | Notes |
|---|---|
| Render, Railway, Fly.io | Free / cheap tiers, deploy from git. Easiest. |
| VPS (Hetzner, DigitalOcean, AWS Lightsail) | ₹400–800/mo. More control. |
| Your laptop + ngrok | Dev only — laptop sleeps = service down. |

Whatever you pick, set `OTP_BASE_URL=https://your-tunnel` and `OTP_API_KEY=sk_…` as env vars. The backend never needs special permissions or to be on the phone's network — it just reaches the phone via the tunnel.

## High availability

For real production with paying users, the phone is a single point of failure. Things you can do:

1. **Two paired phones**, each with its own SIM. Round-robin via a config in your backend.
   - The gateway app doesn't directly support multi-device pairing — implement the round-robin in your backend by configuring two gateway URLs and choosing one per request.
2. **Phone + WhatsApp Cloud API fallback**. WhatsApp via Meta is server-to-server (no SIM needed) and ~₹0.20/msg. Your backend tries WhatsApp first, falls back to the phone gateway for SMS. (Sketched in earlier conversations — implementation guide is on the roadmap.)
3. **SMS provider fallback** — keep a Twilio account with $5 credit. Your backend tries the phone gateway first, falls back to Twilio if 502. Pay only when the phone fails.

## Monitoring

Recommended free monitors:

- **UptimeRobot** — ping `/healthz` every 5 min, alert on failure.
- **BetterStack** — same, prettier.
- Or roll your own: cron a `curl /healthz` every minute and Slack-webhook on non-200.

The Home screen shows a "today's count" stat, but you'll want external observability before relying on this in production.

## Going to production checklist

- [ ] Battery whitelist + OEM kill protections set (see [SETUP](SETUP.md) §6)
- [ ] Phone plugged in to power 24/7
- [ ] Static DHCP lease for the phone
- [ ] Tunnel (Cloudflare / Tailscale / ngrok) up and stable
- [ ] **Public base URL** set in app Settings
- [ ] **CORS** narrowed from `*` to your real domain(s)
- [ ] Rate limits tuned to your actual traffic
- [ ] **API key rotated** (don't ship the first auto-generated one publicly)
- [ ] `/healthz` monitor wired with Slack/email alerts
- [ ] Backend `OTP_BASE_URL` uses the tunnel URL, **not** the LAN IP
- [ ] Backend reads `OTP_API_KEY` from env, not source
- [ ] HTTPS end-to-end (tunnel provides it; never expose `http://` publicly)
