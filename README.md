# Contact Networking Pro

A high-class professional networking Android app.

## Features
- Share your contact details, LinkedIn & WhatsApp via QR code
- Scan business cards with AI (Claude Vision) to auto-fill contacts
- Dark luxury design inspired by [michael-gritzbach.eu](https://michael-gritzbach.eu)

## Stack
- Kotlin + Jetpack Compose
- CameraX (business card scanning)
- Anthropic Claude API (vision-based card parsing)
- ZXing (QR generation)

## Setup
1. Clone the repo
2. Add your Anthropic API key to `local.properties`:
   ```
   ANTHROPIC_API_KEY=sk-ant-...
   ```
3. Open in Android Studio and run
