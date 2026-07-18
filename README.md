# Contact Networking Pro

A professional networking Android app for sharing contact details and capturing business cards.

## Features

- Share standards-compliant vCard 3.0 contact QR codes with Android and Apple devices
- Share LinkedIn, WhatsApp, and custom QR codes
- Scan business cards on-device with Google ML Kit, without an API key or network request
- Review and correct recognized fields before saving a phone contact
- Dark luxury design inspired by [michael-gritzbach.eu](https://michael-gritzbach.eu)

## Stack

- Kotlin and Jetpack Compose
- CameraX and Google ML Kit Text Recognition
- ZXing for QR generation and decoding

## Build and test

1. Clone the repository and open it in Android Studio.
2. Install Android SDK 35.
3. Run the regression tests with `./gradlew testDebugUnitTest`.
4. Build a debug APK with `./gradlew assembleDebug`.

Release APKs are signed separately. Never commit `local.properties`, keystores, or generated APKs.
