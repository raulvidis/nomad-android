# Contributing to NOMAD Android

Thanks for your interest in contributing! This project aims to make critical survival knowledge available offline on Android devices.

## How to Contribute

### Bug Reports

Open an issue with:
- Device info (model, Android version, RAM)
- Steps to reproduce
- Expected vs actual behavior
- Logcat output if available

### Feature Requests

Open an issue describing the feature and why it matters for offline survival use cases.

### Pull Requests

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Make your changes
4. Ensure tests pass: `./gradlew testDebugUnitTest`
5. Ensure lint passes: `./gradlew lint`
6. Ensure build passes: `./gradlew assembleDebug`
7. Open a PR against `main`

### Code Style

- Kotlin, following [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- Jetpack Compose for all UI
- MVVM + Repository pattern (see existing code)
- Hilt for dependency injection
- Room for persistence

### Development Setup

- JDK 17 (Temurin)
- Android SDK API 35, Build Tools 35.0.0
- Android Studio (latest stable)

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
