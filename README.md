# Contact Networking Pro

Private, reliable contact exchange for real-world networking. Contact Networking Pro creates standards-compliant contact QRs, keeps your most-used networking codes one tap away, and turns paper business cards into editable phone contacts.

## Why it exists

Networking tools should make the moment of exchange faster, not ask both people to create accounts. This app works without a backend, subscription, or sign-in and treats the user's contact data as local-first by default.

## Product highlights

- **Cross-platform contact sharing:** conservative vCard 3.0 payloads tested for Android and Apple Contacts, including international names and UTF-8 content.
- **Purpose-built presentation mode:** full-brightness QR view with screen-awake behavior for fast scanning in difficult lighting.
- **Home-screen widget:** show a selected QR directly or add a discreet quick link to full-screen presentation.
- **Lock-screen-safe access:** an optional Android Quick Settings tile presents the selected QR without an always-running background service.
- **Private card capture:** Google ML Kit reads business cards on-device; users review every detected field before opening the phone's contact-save flow.
- **Flexible identity panels:** contact, LinkedIn, WhatsApp, and up to two named custom QR panels.
- **No tracking:** no account, analytics SDK, ad SDK, backend, or network permission.

## Product decisions

The app intentionally does not implement background shake-to-open. A reliable version would require continuous sensor monitoring, a foreground service, and a persistent notification. The widget and Quick Settings tile provide faster, predictable access without battery or accidental-trigger costs.

The lock-screen feature is user-initiated through Android's Quick Settings surface. It never dismisses the lock screen or bypasses device security.

## Architecture

- Kotlin, Jetpack Compose, and Material 3
- Google ML Kit Text Recognition for local OCR
- ZXing for UTF-8 QR encoding and decoding
- SharedPreferences in app-private storage; Android backup explicitly disabled
- Traditional Android App Widgets and Quick Settings Tile APIs

QR generation is centralized in one encoding contract (UTF-8, error correction level M, four-module quiet zone), shared by the app, widget, sharing flow, and lock-screen presentation.

## Build and verify

Requirements: Android Studio or Android SDK 36, JDK 17+, and an Android 8.0+ device or emulator.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
```

CI runs unit tests, Android lint, and debug/release builds for every push and pull request.

## Privacy

The app requests camera permission only when a user chooses to scan a card. Captured images are stored temporarily in the app cache, processed locally, and deleted after recognition. See the full [privacy policy](docs/privacy.html).

## Release

Signed APKs and release notes are published under [GitHub Releases](https://github.com/mgritzbach/contact-networking-pro/releases). Keystores, local SDK configuration, and generated binaries are intentionally excluded from source control.
