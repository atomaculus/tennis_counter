# Garmin Connect IQ Mobile SDK

This project no longer requires manually dropping the Garmin Connect IQ Mobile SDK
`.aar` into `mobile/libs`.

The Android companion resolves the SDK directly from Maven Central:

```kotlin
implementation("com.garmin.connectiq:ciq-companion-app-sdk:2.2.0@aar")
```

## Source

Garmin documents that the Android SDK is available via Maven Central, and the
official Android sample repo uses the same dependency form.

References:

- https://developer.garmin.com/connect-iq/sdk/
- https://github.com/garmin/connectiq-android-sdk

## Why this folder still exists

The folder is kept only as a placeholder for historical context and to avoid
breaking local tooling that expects `mobile/libs` to exist.

## Build

```bash
./gradlew :mobile:assembleDebug
```

If dependency resolution fails, verify that `mavenCentral()` is enabled in the
root Gradle repository configuration and that the environment has network access.
