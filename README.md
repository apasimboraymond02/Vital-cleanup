# Vital

A privacy-first, comprehensive health tracking application for women.

## Features

### Core Features
- **Cycle Tracking**: Period logging, symptom tracking, predictions
- **Fertility Calculator**: Safe period calculation with risk indicators
- **Pregnancy Tracker**: Week-by-week progress, milestones, tools
- **Health Content Library**: Medically reviewed articles
- **Health Metrics**: Temperature, weight, mood tracking
- **Dashboard**: Overview of all health data

### Privacy & Security
- Local-first data storage (SQLite)
- End-to-end encryption for sensitive data
- Biometric authentication
- Granular privacy controls
- No data selling policy
- GDPR/HIPAA inspired practices

### Technical Features
- Dark/Light theme support
- Offline functionality
- Data export (JSON/CSV)
- Push notifications
- Accessibility support
- Multi-language ready

## Getting Started

### Prerequisites
- Node.js 16+
- Expo CLI
- iOS Simulator or Android Emulator

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/vital.git
cd vital
```

2. Install JavaScript dependencies:
```bash
npm install
# or: npm install --legacy-peer-deps
```

3. Configure secrets (not committed to git):
```bash
# Expo / EAS
# - set your own IDs in app.json -> extra.eas.projectId
# - set your AdMob IDs in app.json -> react-native-google-mobile-ads plugin

# Android (native Kotlin app in app/src)
cp local.properties.example local.properties
# then edit sdk.dir to point at your Android SDK

cp app/google-services.json.example app/google-services.json
# then fill in your Firebase project values
```

4. Run:
```bash
# Expo
npx expo start

# Android native (needs Android SDK + JDK 17)
./gradlew :app:assembleDebug
```

## Notes for contributors

This repo intentionally excludes build outputs and secrets:

- `build/`, `app/build/`, `*.aab`, `*.apk`, Gradle caches
- `local.properties`, `release-key.keystore` / `*.keystore`, `*.jks`
- `app/google-services.json` (use `app/google-services.json.example` as template)
- `node_modules/`, `.expo/`, logs

See `.gitignore`, `local.properties.example`, and `app/google-services.json.example`.
Signing passwords are read from env vars / `gradle.properties`
(`VITAL_RELEASE_STORE_FILE`, `VITAL_RELEASE_STORE_PASSWORD`,
`VITAL_RELEASE_KEY_ALIAS`, `VITAL_RELEASE_KEY_PASSWORD`).