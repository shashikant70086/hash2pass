# Contributing

Thanks for considering a contribution. Couple of ground rules + the bits you need to know.

## Ground rules

- Open an issue before a large PR. Saves both of us time.
- Keep PRs focused — one fix or feature per PR.
- Match the existing code style (4-space Kotlin, `?.let { }` over `if (x != null) ...`, sealed interfaces for state).
- Don't add analytics, crash reporters, or anything that phones home. This project deliberately ships zero telemetry.
- Don't introduce paid SDKs.
- Don't break the existing API contract (`/v1/...`) — add new endpoints instead.

## Set up locally

```bash
git clone <repo>
cd otp-gateway-android
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # to a connected device
```

Requirements: Android Studio Hedgehog or newer, JDK 17, a real phone with a SIM for end-to-end tests.

## Where things live

```
app/src/main/java/com/skant/otpgateway/
├── MainActivity.kt              Compose host + permissions + service start
├── OtpGatewayApplication.kt     Hilt entry, notification channel
├── data/
│   ├── Config.kt                env-style settings + DataStore repo
│   ├── NetworkUtils.kt          LAN IP discovery
│   ├── OtpService.kt            generate → hash → SMS → log
│   ├── SmsSender.kt             SmsManager wrapper + delivery receiver
│   └── local/
│       └── SendLog.kt           Room: SendLogDao + OtpPendingDao
├── di/AppModule.kt              Hilt bindings (Room, DAOs)
├── server/EmbeddedServer.kt     Ktor CIO + routes + auth
├── service/GatewayForegroundService.kt   foreground service that owns the server
└── ui/
    ├── HomeScreen.kt            status, URL, key, today's count
    ├── SettingsScreen.kt        env-editor UI
    └── LogScreen.kt             last 100 deliveries
```

## Common dev tasks

### Add a new endpoint

1. Define request/response data classes near the top of `server/EmbeddedServer.kt` with `@Serializable`.
2. Add a route inside the `routing { }` block.
3. Call `authorize(call, cfg)` at the top of the handler.
4. Keep the response shape consistent: success returns the data, errors return `{ error: "..." }` or `{ ok: false, reason: "..." }`.
5. Document in [docs/API.md](docs/API.md).
6. Add a snippet in [docs/INTEGRATION.md](docs/INTEGRATION.md) if useful.

### Add a config field

1. Add the field to `Config` data class + `Config.DEFAULT`.
2. Add a `K.<NAME>` key in `ConfigRepository`.
3. Read it in `flow { ... }` and write it in `save(...)`.
4. Surface a UI control in `SettingsScreen.kt`.
5. Use it where it belongs (server, OtpService, etc.).
6. Document in [docs/CONFIG.md](docs/CONFIG.md).

### Add a Room column

1. Bump `version` in `@Database(... version = N+1 ...)`.
2. Either write a migration (`addMigrations(...)` in `AppModule`) or use `fallbackToDestructiveMigration()` (already on for dev).

## Style

- Prefer **sealed interface + data classes** for state, never enums-with-fields.
- Prefer **suspend functions** over `Flow<X>` when the call is one-shot.
- Prefer **`@Volatile`** for mutable singletons referenced across threads.
- Avoid Hilt for trivial classes — manual construction is fine in `companion object`.
- One thing per file when it's > ~200 lines. Below that, group related types together.

## Testing

There aren't many tests yet. PRs adding tests are very welcome:
- `app/src/test/` for pure-JVM logic (`Config`, `OtpService` without `SmsSender`).
- `app/src/androidTest/` for `SmsSender`, Room DAOs.

If you add behavior, add at least one test for the happy path and one for the failure mode.

## Commit messages

Conventional-ish:
- `fix: …` for bugs
- `feat: …` for new features
- `docs: …` for docs-only changes
- `refactor: …` for code restructure without behavior change
- `chore: …` for build/dep bumps

## Releasing (maintainers)

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Update [CHANGELOG.md](CHANGELOG.md).
3. `./gradlew :app:assembleRelease`
4. Sign the APK.
5. Tag the commit `v0.x.0`, push, create a GitHub Release with the APK attached.

## Questions

Open a discussion (preferred) or email the maintainer. Don't email about bugs — open an issue.
