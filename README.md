# Launchly

Launchly is an unofficial Android version manager for Minecraft: Bedrock Edition. It uses an authenticated Google delivery session, downloads compatible APK splits as persistent background work, validates the complete APK set, and hands installation to Android.

## Platform

- Android 9 (API 28) or newer
- `compileSdk` and `targetSdk` 35 by intentional choice for signed GitHub APK distribution
- ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86` when reported by the device and catalog
- System light/dark appearance, with dynamic color on Android 12+

Launchly is not distributed through Google Play. Release APKs are published through this repository’s GitHub Releases page as prereleases until device and real-account smoke testing is complete.

## Build

The project uses JDK 17, Android Gradle Plugin 9.3, Gradle 9.5, built-in Kotlin, Compose, Room, DataStore, and WorkManager.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew :app:verifyReleaseArtifact
```

Release signing is never stored in the repository. A signed build accepts these Gradle properties or environment variables:

- `LAUNCHLY_STORE_FILE`
- `LAUNCHLY_STORE_PASSWORD`
- `LAUNCHLY_KEY_ALIAS`
- `LAUNCHLY_KEY_PASSWORD`

Release acceptance pins certificate SHA-256 `42cf3d736aa375f4d5971b6bf3ae5ddc8ed4e230067cfc529c7a6bcfa7a0a8dd`, rejects legacy-client identifiers, and enforces a 15 MiB universal APK budget. Maintainers must also keep Android developer verification current for certified-device sideload distribution.

CI runs JVM tests, lint, the R8 acceptance build, dependency review, and x86_64 emulator tests on API 28, 31, 33, and 35, including a large-screen landscape case. Before promoting a prerelease, maintainers must additionally smoke-test ARM64 hardware, a real entitled account, fresh install, production-signed upgrade with `adb install -r`, download/resume, install, launch, and the explicitly confirmed downgrade path.

## Privacy and safety

Read the [privacy notice](PRIVACY.md) and [disclaimer](DISCLAIMER.md). Launchly deliberately has no automatic analytics. Diagnostic export is explicit and excludes credentials and account details.

## Support

- [GitHub Issues](https://github.com/flarialmc/Launchly/issues)
- [Source repository](https://github.com/flarialmc/Launchly)

Launchly is not an official Minecraft product and is not approved by or associated with Mojang, Microsoft, or Google.
