# PLAYCE (play + ace)

Sistema de scoring de tenis: Wear OS (partido en vivo) + Android mobile (historial y share card).

## Estructura
- `app/` -> Wear OS (contador, timer, sync con ACK)
- `mobile/` -> Android phone (Room, historial, detalle, compartir)

## Build de release
1. Configurar versiones en `gradle.properties`:
   - `PLAYCE_VERSION_CODE`
   - `PLAYCE_VERSION_NAME`
2. Copiar `keystore.properties.template` a `keystore.properties` y completar datos reales.
3. Generar artefactos:
   - `./gradlew :mobile:bundleRelease`
   - `./gradlew :app:assembleRelease`

## Crash reporting
- Activar `enableCrashlytics=true` en `gradle.properties`.
- Agregar `google-services.json` en módulos que reporten crashes.

## Publicación
Ver checklist detallado en `docs/playstore-checklist.md`.
Borrador de política en `docs/privacy-policy.md`.
