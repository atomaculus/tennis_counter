# PLAYCE - Play Store release checklist

## Build
- [ ] Set `PLAYCE_VERSION_CODE` / `PLAYCE_VERSION_NAME` in `gradle.properties`.
- [ ] Copy `keystore.properties.template` -> `keystore.properties` and fill real values.
- [ ] Build signed artifacts:
  - `./gradlew :mobile:bundleRelease`
  - `./gradlew :app:assembleRelease`

## Crash + metrics
- [ ] Set `enableCrashlytics=true` in `gradle.properties`.
- [ ] Add `google-services.json` to `mobile/` and `app/` if both report crashes.
- [ ] Validate a forced test crash is visible in Firebase Crashlytics.

## QA
- [ ] Release install on real phone + real watch.
- [ ] Scoring rules: deuce/advantage/game/set.
- [ ] Sync flows: online, offline, reconnection, duplicate ACK.
- [ ] App kill / reboot recovery both devices.

## Listing
- [ ] Short description / full description (ES + EN).
- [ ] Screenshots (phone + watch).
- [ ] Icon and feature graphic.
- [ ] Privacy policy URL published.
