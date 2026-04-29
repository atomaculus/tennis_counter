# Garmin Connect IQ Mobile SDK

Drop the official Garmin Connect IQ Mobile SDK `.aar` file in this folder.

The Gradle build picks up any `*.aar` placed here:

```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
```

## Where to get it

The SDK is **not** distributed via Maven Central. Download it from Garmin's
developer portal:

- https://developer.garmin.com/connect-iq/sdk/
- Look for "Connect IQ Mobile SDK" / "Companion App SDK" for Android

The expected artifact is something like
`connectiq-companion-app-sdk-X.Y.Z.aar` (file name does not matter — the
Gradle rule matches `*.aar`).

## Why this isn't checked in

The SDK has its own license and is not redistributable through this repo.
Each developer must download it once and place it here locally. CI
workflows that build `:mobile` should pull it from a private artifact
storage at provisioning time.

## After dropping the .aar

```
./gradlew :mobile:assembleDebug
```

should compile without any change to source code.
