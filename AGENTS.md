# AGENTS.md

Guía operativa para Codex y cualquier otro agente de IA que trabaje en este repositorio.

## 1. Descripción General del Proyecto

`tennis_counter` es una app Android multi-módulo para:
- **Wear OS** (`:app`): contador de tenis, cierre de partido y envío de resultado al teléfono.
- **Mobile** (`:mobile`): historial local de partidos, detalle y share de imagen, con recepción de eventos desde Wear y desde Garmin Connect IQ en paralelo.
- **Garmin Connect IQ** (repo hermano `playce_garmin`): port del scorer al ecosistema Garmin (Monkey C). El módulo `:mobile` actúa como companion para ambos relojes simultáneamente.

Estado funcional actual:
- Marcador de tenis con lógica de points/games/sets (incluye deuce/advantage).
- Timer persistente en Wear con control explícito de ciclo de vida.
- UX principal en Wear:
  - Tap para sumar puntos.
  - Long press por jugador para undo contextual.
  - Long press simultáneo (A+B) para acciones admin (reset game/reset match).
  - Indicador visual de sacador por game (resaltado del botón del jugador que saca).
  - Indicador visual de lado de saque con halo lateral dinámico izquierda/derecha según puntos del game.
  - La alternancia de saque sigue la secuencia completa del partido y no se reinicia al empezar un nuevo set.
- Flujo manual de finalización:
  - Botón `END MATCH` + confirmación.
  - Pantalla final `MATCH FINISHED`.
  - Botón `SAVE MATCH`.
  - Estado visual de sync en Wear bajo `SAVE MATCH`:
    - `Syncing...`
    - `Retry in Ns`
    - `Synced ✓` (transitorio)
  - Botón `NEW MATCH` visible y sin cambios de comportamiento.
- Sync Wear -> Mobile activo por Data Layer (`MessageClient`).
- Sync con idempotency + ACK Wear<->Mobile para limpiar pendientes en Wear.
- Mobile recibe, deduplica e inserta en Room (`MatchRepository`).
- Monetización freemium en Mobile (Google Play Billing, compra única `premium_unlock`):
  - App gratis + unlock Premium opcional.
  - Counter en mobile disponible gratis (lógica de scoring tipo Wear + timer local).
  - Sin Premium: mobile bloquea historial/detalle/share y no guarda matches recibidos desde Wear.
  - Con Premium: habilita guardado, historial, detalle y share card.
- Rediseño visual PLAYCE aplicado:
  - **Mobile (`:mobile`)** con estética dark minimal + acento verde tenis.
  - **Wear (`:app`)** con theme PLAYCE consistente (dark minimal + acento verde).
  - Branding PLAYCE aplicado en launcher icons (Mobile + Wear) con adaptive icon:
    - fondo negro sólido
    - logo `A` verde como foreground
  - Header principal de historial en mobile usa `wordmark` PLAYCE (imagen) en lugar de texto plano.
  - Mobile `MainActivity` usa theme `NoActionBar` para evitar barra superior nativa duplicada.
  - Wear score responsivo (sin overlap en `30-15`, `40-30`, `AD-40`).
  - Wear `MATCH FINISHED` conserva CTAs (`SAVE MATCH` / `NEW MATCH`) + estado de sync visible como pill discreto.
- Companion Garmin Connect IQ activo en `:mobile`:
  - `:mobile` acepta los mismos paths semánticos (`/playce/match-config`, `/playce/live`, `/match_finished`, `/match_finished_ack`) tanto desde Wear OS como desde Garmin sin duplicar lógica de scoring/persistencia.
  - El reloj Garmin corre la app del repo hermano `playce_garmin`.
  - App ID Connect IQ actual alineado con `playce_garmin/manifest.xml`: `c4f18a72b93e4d6fa1c8e5b2079d3a44`.
  - El envío de match-config desde el teléfono dispara Wear y Garmin en paralelo (cada watch ignora si no le corresponde).
  - Wear OS sigue funcionando intacto incluso si Garmin Connect Mobile no está instalado.

---

## 2. Módulos y Configuración Crítica

### Módulos actuales
- `:app` (Wear OS)
- `:mobile` (teléfono)

`settings.gradle.kts` incluye ambos:
- `include(":app")`
- `include(":mobile")`

### Configuración crítica para Wear Data Layer
Para que Google Play Services enrute mensajes entre Wear y Mobile:
- `applicationId` de `:app` y `:mobile` debe ser **idéntico**.
- Valor actual en ambos: `com.playce.tenniscounter.app`.

Notas:
- `:mobile` mantiene `namespace = "com.example.tenniscounter.mobile"`.
- `namespace` y `applicationId` no tienen que ser iguales entre sí, pero para Data Layer importa el `applicationId`.
- `targetSdk` y `compileSdk` actuales en ambos módulos: **35**.
- En Play Console, `versionCode` debe ser único por `applicationId` entre artefactos (mobile/wear), no reutilizar el mismo código al subir otro bundle.

---

## 3. Estructura Relevante

### Wear
- `app/src/main/java/com/example/tenniscounter/MainActivity.kt`
  - UI Wear Compose.
  - Handler de `SAVE MATCH`, envío Data Layer (`/match_finished`) y UI de estado de sync en `MATCH FINISHED`.
  - Theme/components PLAYCE para Wear (colores, cards/chips/buttons).
  - Score board responsivo (ajuste de tamaño por ancho disponible).
  - Indicadores visuales de saque (servidor actual + lado de saque).
  - `MATCH FINISHED` con layout scrollable para pantallas pequeñas.
- `app/src/main/java/com/example/tenniscounter/sync/PendingMatchStore.kt`
  - Persistencia local de pending message en Wear (`SharedPreferences`) para retry/ACK.
  - Lectura de estado UI (`getPending()` / `hasPending()`).
- `app/src/main/java/com/example/tenniscounter/sync/WearAckListenerService.kt`
  - Listener Wear para ACK desde mobile (`/match_finished_ack`) y limpieza de pending por `idempotencyKey`.
- `app/src/main/java/com/example/tenniscounter/ui/TennisViewModel.kt`
  - Lógica de score, timer, summary final, save local y estado derivado de saque actual.
- `app/src/main/java/com/example/tenniscounter/ui/TimerStateStore.kt`
  - Estado persistido del timer (`isRunning`, `startElapsedRealtime`, `accumulatedSeconds`) + migración de keys legacy.
- `app/src/main/java/com/example/tenniscounter/timer/MatchTimerService.kt`
  - Service del timer con `onTaskRemoved()` para consolidar tiempo y frenar el timer.
- `app/src/main/AndroidManifest.xml`
  - Registro de `MatchTimerService` y `WearAckListenerService`.
  - Launcher icon configurado vía `@mipmap/ic_launcher` (adaptive icon PLAYCE).
- `app/src/main/res/mipmap-anydpi-v26/*`
  - Adaptive icons Wear (`ic_launcher`, `ic_launcher_round`).
- `app/src/main/res/drawable/ic_launcher_background.xml`
  - Fondo negro del adaptive icon.
- `app/src/main/res/drawable/ic_launcher_foreground.png`
  - Foreground con logo `A` verde centrado/reescalado.

### Mobile (Garmin bridge)
- `mobile/src/main/java/com/example/tenniscounter/mobile/MobileApplication.kt`
  - `Application` que inicializa `GarminConnectivityManager` en `onCreate`. Registrado en el manifest como `android:name=".MobileApplication"`.
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminConstants.kt`
  - App ID Connect IQ + paths + claves de payload + valores ACK (`inserted`/`duplicate`/`premium_locked`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminPayloadCodec.kt`
  - Helpers `getInt/getLong/getBoolean/getString` que normalizan el `Map<String, Any?>` que llega del SDK (Number puede venir como Long o Double).
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminConnectivityManager.kt`
  - Singleton del SDK. Inicializa `ConnectIQ` con `IQConnectType.WIRELESS`, expone `StateFlow<GarminConnectionState>` con `sdkState`/`knownDevices`/`connectedDeviceCount`, registra `IQDeviceEventListener` y `IQApplicationEventListener` por device.
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminMessageRouter.kt`
  - Recibe el envelope `Map<String, Any?>` y dispatcha por `path`.
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminLiveScoreHandler.kt`
  - Convierte payload de `/playce/live` a `LiveMatchState` y empuja a `LiveScoreRepository.update()` (mismo repo que usa Wear).
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminFinishedMatchHandler.kt`
  - Replica la lógica de `WearMatchListenerService`: gate premium, `MatchRepository.insertIfNotExists`, ACK con status (`inserted`/`duplicate`/`premium_locked`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminAckSender.kt`
  - Envía envelope `/match_finished_ack` por Connect IQ.
- `mobile/src/main/java/com/example/tenniscounter/mobile/garmin/GarminMatchConfigSender.kt`
  - Envía `/playce/match-config` por Connect IQ a todos los Garmin conectados (paralelo a `MatchConfigBroadcaster`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/components/GarminConnectionBadge.kt`
  - Chip discreto en la esquina superior del Counter route con el estado del SDK Garmin.
- `mobile/src/main/java/com/example/tenniscounter/mobile/MainActivity.kt`
  - Solicita permiso runtime `BLUETOOTH_CONNECT` (Android 12+) y re-inicializa el manager si lo concede.
- `mobile/src/main/AndroidManifest.xml`
  - Agrega permisos `BLUETOOTH`/`BLUETOOTH_ADMIN`/`BLUETOOTH_CONNECT`, `<queries>` para `com.garmin.android.apps.connectmobile`, y registra `MobileApplication`.

### Mobile
- `mobile/src/main/java/com/example/tenniscounter/mobile/sync/WearMatchListenerService.kt`
  - Receptor Data Layer (`WearableListenerService`).
  - Si no hay Premium, no guarda el match y responde ACK `premium_locked`.
- `mobile/src/main/java/com/example/tenniscounter/mobile/billing/*`
  - Billing de Google Play + cache local de entitlement Premium (`premium_unlock`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/data/local/MatchEntity.kt`
  - Entidad Room con `setScoresText` nullable.
- `mobile/src/main/java/com/example/tenniscounter/mobile/data/local/AppDatabase.kt`
  - Versión 2 + migración `1 -> 2` para agregar columna `setScoresText`.
- `mobile/src/main/java/com/example/tenniscounter/mobile/data/local/MatchDao.kt`
  - Query de deduplicación por `createdAt + durationSeconds + finalScoreText`.
- `mobile/src/main/java/com/example/tenniscounter/mobile/data/MatchRepository.kt`
  - Inserción con dedupe y soporte `setScoresText`.
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/history/HistoryScreen.kt`
  - Muestra resultado global y, si existe, detalle de sets.
  - Header usa `wordmark` PLAYCE (`playce_wordmark_header.png`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/counter/*`
  - Counter en mobile (scoring de tenis, undo, reset game/match, timer local).
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/detail/MatchDetailScreen.kt`
  - Muestra resultado global + detalle de sets.
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/share/ShareCard.kt`
  - Render de imagen shareable incluyendo detalle de sets.
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/share/MatchShareManager.kt`
  - Prepara datos del share card.
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/components/*`
  - Componentes UI PLAYCE reutilizables (`MatchCard`, `PrimaryButton`, `SectionHeader`, `ShareCard`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/theme/*`
  - Theme PLAYCE (`PlayceColors`, `PlayceTheme`, `PlayceShapes`, `PlayceTypography`).
- `mobile/src/main/AndroidManifest.xml`
  - Registro correcto del listener de Wear.
  - `MainActivity` con theme `Theme.Playce.Mobile` (sin barra superior nativa).
- `mobile/src/main/res/values/themes.xml`
  - Theme XML nativo (`android:Theme.Material.NoActionBar`) para quitar ActionBar sin depender de themes XML de Material/AppCompat.
- `mobile/src/main/res/mipmap-anydpi-v26/*`
  - Adaptive icons Mobile (`ic_launcher`, `ic_launcher_round`).
- `mobile/src/main/res/drawable/ic_launcher_background.xml`
  - Fondo negro del adaptive icon.
- `mobile/src/main/res/drawable/ic_launcher_foreground.png`
  - Foreground con logo `A` verde centrado/reescalado.
- `assets/branding/*`
  - Assets fuente de branding PLAYCE (ícono/wordmark de referencia).

---

## 4. Contrato de Sincronización Wear -> Mobile

Ruta fija:
- `/match_finished`

Formato:
- `DataMap` (NO JSON), serializado con `toByteArray()`.

Campos enviados desde Wear:
- `createdAt` (`Long`)
  - con fallback a `System.currentTimeMillis()` si el summary trae `<= 0`.
- `durationSeconds` (`Long`)
- `finalScoreText` (`String`)
  - valor actual: `summary.setsScore`.
- `setScoresText` (`String`, opcional)
  - formato: `"6-4 3-6 6-2"`.
- `idempotencyKey` (`String`, UUID)

Comportamiento en Mobile:
- Si `path != "/match_finished"`: ignorar.
- Parseo de `DataMap.fromByteArray(messageEvent.data)`.
- Inserción en Room vía `MatchRepository`.
- Deduplicación heurística (sin usar `idempotencyKey` todavía):
  - No inserta si ya existe match con mismo:
    - `createdAt`
    - `durationSeconds`
    - `finalScoreText`
- Backward compatibility:
  - Si `setScoresText` no viene, se guarda `null` y UI/share no se rompen.

ACK Mobile -> Wear:
- Ruta fija:
  - `/match_finished_ack`
- Formato:
  - `DataMap` serializado con `toByteArray()`
- Campos:
  - `idempotencyKey` (`String`)
  - `status` (`String`) valores actuales observados: `inserted` / `duplicate` / `premium_locked`
- Comportamiento en Wear:
  - `WearAckListenerService` parsea ACK y ejecuta `PendingMatchStore.clearIfMatches(...)`
  - Si el `idempotencyKey` no coincide con el pending actual, no limpia

Retry/pendiente en Wear (alto nivel):
- `SAVE MATCH` guarda primero un `PendingMatchMessage` local.
- Se intenta envío inmediato al teléfono.
- Si falla/no hay teléfono, queda pending para retry.
- `PendingMatchStore` conserva:
  - `idempotencyKey`
  - `payload`
  - `createdAtMillis`
  - `attemptCount`
  - `nextRetryAtMillis`
  - `targetNodeId` (opcional)

---

## 4.b. Contrato de Sincronización Garmin Connect IQ <-> Mobile

El bridge Garmin usa los **mismos paths semánticos** que Wear OS pero el transporte cambia: cada mensaje viaja como un envelope serializable a través de `Communications.transmit` (en Monkey C) y `ConnectIQ.sendMessage` (en Android), encapsulado en un `Map<String, Any?>`.

Envelope:

```
{
  "path": "/playce/live"             // ruta semántica
  "kind": "live_score"               // tipo lógico (match_config | live_score | finished_match | finished_match_ack)
  "payload": { ... }                 // datos específicos del path
  "idempotencyKey": null | "..."     // solo en finished_match / ack
  "timestamp": <Long>                // milisegundos
}
```

Paths y dirección:

| Path | Dirección | Kind |
|---|---|---|
| `/playce/match-config` | phone → watch | `match_config` |
| `/playce/live` | watch → phone | `live_score` |
| `/match_finished` | watch → phone | `finished_match` |
| `/match_finished_ack` | phone → watch | `finished_match_ack` |

Claves de payload:

- **match-config**: `playerAName`, `playerBName`, `setsToWin`, `tiebreakAtSixAll`, `tiebreakPoints`, `superTiebreakInFinalSet`, `noAdScoring`, `timestamp`.
- **live**: `playerA_points`, `playerA_games`, `playerA_sets`, `playerB_*`, `completedSets`, `pointLabelA`, `pointLabelB`, `elapsedSeconds`, `isMatchActive`, `lastScoredPlayer`, `scorerNodeId`, `timestamp`.
- **finished**: `createdAt`, `durationSeconds`, `finalScoreText`, `setScoresText`, `idempotencyKey`, `playerAName`, `playerBName`.
- **ack**: `idempotencyKey`, `status` (`inserted` / `duplicate` / `premium_locked`).

Las claves coinciden 1:1 con las que ya usa Wear OS sobre `DataMap`, por eso el `LiveScoreRepository` y `MatchRepository` se reutilizan sin tocar.

App ID Connect IQ:
- `c4f18a72b93e4d6fa1c8e5b2079d3a44` (alineado entre `GarminConstants.APP_ID` y `playce_garmin/manifest.xml`).

Comportamiento de retry:
- El watch Garmin reintenta `/match_finished` cada 8s hasta recibir un ACK con el mismo `idempotencyKey`.
- `MatchRepository.insertIfNotExists` ya es idempotente por `idempotencyKey`, así que reintentos repetidos no producen duplicados.

Reglas de no-romper-Wear:
- No tocar `WearMatchListenerService`, `LiveScoreListenerService`, `MatchConfigBroadcaster`, `LiveScoreRepository`. El bridge Garmin reutiliza, no reemplaza.
- En `MobileApp.onSendConfigToWatch` se invoca el broadcaster Wear y el sender Garmin en paralelo. No agregar lógica de selección.
- Si el SDK Garmin falla al inicializar (sin GCM o sin permisos), el badge muestra el estado y el resto de la app debe seguir funcionando intacta.

Dependencia Android actual del SDK Garmin:
- `mobile/build.gradle.kts` usa `implementation("com.garmin.connectiq:ciq-companion-app-sdk:2.2.0@aar")`.

Permisos Android requeridos por el SDK Garmin:
- `BLUETOOTH` y `BLUETOOTH_ADMIN` (legacy hasta Android 11).
- `BLUETOOTH_CONNECT` (Android 12+, runtime, solicitado en `MainActivity`).
- `<queries><package android:name="com.garmin.android.apps.connectmobile" /></queries>` para que la app pueda detectar Garmin Connect Mobile en Android 11+.

---

## 5. Manifest del Listener (Mobile)

El `service` de listener debe cumplir:
- `android:exported="true"`
- `intent-filter` específico de mensajes para evitar lint fatal `WearableBindListener`:
  - acción `com.google.android.gms.wearable.MESSAGE_RECEIVED`
  - `data` con `android:scheme="wear"`, `android:host="*"`, `android:pathPrefix="/match_finished"`
- **NO** usar `BIND_LISTENER` genérico en release (rompe `lintVitalRelease`).
- **NO** declarar `android:permission="com.google.android.gms.wearable.BIND_LISTENER"`.

Ejemplo correcto:
```xml
<service
    android:name=".sync.WearMatchListenerService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
        <data
            android:host="*"
            android:pathPrefix="/match_finished"
            android:scheme="wear" />
    </intent-filter>
</service>
```

---

## 6. Timer Wear (Comportamiento Esperado)

### Modelo persistido
- `isRunning` (`Boolean`)
- `startElapsedRealtime` (`Long`)
- `accumulatedSeconds` (`Long`)

### Cálculo de elapsed
- Si `isRunning=true`:
  - `accumulatedSeconds + (nowElapsed - startElapsedRealtime)/1000`
- Si `isRunning=false`:
  - `accumulatedSeconds`

### Reglas UX actuales
- El timer NO auto-arranca: inicio manual explícito del usuario (criterio compartido con iOS/watchOS y mobile; footer START/PAUSE/RESUME en Wear).
- Al entrar a `CounterScreen` con scoreboard nuevo (0-0), si quedó tiempo viejo de una sesión anterior sin Start manual, se resetea (`reset timer on fresh scoreboard after task removed`) y queda detenido.
- `New Match` resetea el timer y lo deja detenido hasta que el usuario lo inicie.
- `MatchTimerService.onTaskRemoved()` consolida tiempo, setea `isRunning=false` y `stopSelf()`.

Logs clave timer:
- `reset timer on fresh scoreboard after task removed`
- `MatchTimerService onTaskRemoved`

---

## 7. Build y Ejecución

### Build por consola
Windows (PowerShell):
```bash
.\gradlew.bat :app:assembleDebug :mobile:assembleDebug
```

Clean + mobile rebuild (útil para invalidar overlays/dex):
```bash
.\gradlew.bat clean :mobile:assembleDebug
```

### Ejecución en Android Studio
- `:app` en dispositivo/emulador Wear.
- `:mobile` en teléfono.
- Si aparece una barra superior nativa en mobile (duplicando el wordmark), verificar que `MainActivity` siga usando `Theme.Playce.Mobile`.

Nota tras cambio de `applicationId` de mobile:
- Si Android Studio intenta lanzar `com.example.tenniscounter.mobile/...` y falla, recrear Run Configuration.
- Componente esperado de mobile:
  - paquete app: `com.playce.tenniscounter.app`
  - activity: `com.example.tenniscounter.mobile.MainActivity`

---

## 8. Observabilidad / Debug

Tags de logs:
- Wear envío: `WearDataLayer`
- Wear ACK: `WearAckListener`
- Wear timer/service: `MatchTimer`, `MatchTimerService`
- Mobile recepción: `WearMatchListener`

Casos de diagnóstico:
- Wear:
  - `connectedNodes count=0` => teléfono no detectable por Data Layer.
  - envío exitoso => log `Sent /match_finished`.
  - ACK recibido => log con `ACK received ... clearedPending=true/false`.
  - UI `MATCH FINISHED` (bajo `SAVE MATCH`) refleja pending local:
    - pill superior discreto con estado (`SYNCING...`, `RETRY IN Ns`, `SENT`)
    - sin ocultar `SAVE MATCH` / `NEW MATCH`
- Mobile:
  - `onMessageReceived path=/match_finished ...`
  - `Decoded payload ... durationSeconds=...`
  - `Match inserted...` o `Duplicate match ignored...`
  - `ACK sent ... status=inserted|duplicate`

---

## 9. Reglas para Agentes de IA

- Leer siempre `AGENTS.md` antes de modificar código.
- Mantener cambios incrementales y no romper lógica existente.
- En cambios de branding/UI:
  - no modificar lógica de scoring/sync/timer/DB.
  - priorizar cambios en recursos/theme/layout.
- En Data Layer, verificar siempre:
  - `applicationId` alineado entre módulos.
  - manifest de listener correcto.
  - contrato de rutas y keys (`/match_finished` y `/match_finished_ack`).
- En UI Wear de `MATCH FINISHED`:
  - no ocultar ni cambiar comportamiento de `NEW MATCH`.
  - mantener el estado de sync/retry como pill/banner discreto (no ocupar el espacio principal del CTA).
- No tocar lógica de ACK/idempotency/retry al hacer ajustes visuales de estado; solo leer `PendingMatchStore`.
- En UI Wear del score:
  - priorizar legibilidad sobre densidad visual en pantallas pequeñas.
  - evitar overlap (usar layout responsivo/weights y, si hace falta, reducir fontSize por ancho).
- En mobile header:
  - no reintroducir barra superior nativa (ActionBar) si ya existe wordmark dentro de Compose.
- Para cambios de Room:
  - agregar migración explícita si cambia schema.
- Para cambios de timer Wear:
  - respetar comportamiento `stop on task removed`.
  - no introducir resets silenciosos fuera de reglas UX definidas.
- Para cambios en el bridge Garmin (`mobile/.../garmin/*`):
  - mantener los nombres de claves de payload alineados con el repo `playce_garmin` (`PlayceGarminContracts.mc`); divergir rompe la comunicación silenciosamente.
  - NO crear listeners Wear adicionales para los mismos paths — el bridge Garmin tiene su propio transporte y comparte solo los repositorios (`LiveScoreRepository`, `MatchRepository`).
  - cualquier nuevo path Garmin debe agregarse en `GarminConstants` y rutearse en `GarminMessageRouter`, no inline.
  - el SDK Connect IQ debe inicializarse exclusivamente desde `MobileApplication.onCreate` o en respuesta al permiso runtime concedido.

---

## 10. Convenciones de Commit

Formato sugerido:
- `feat: ...`
- `fix: ...`
- `refactor: ...`
- opcional por alcance:
  - `feat(wear): ...`
  - `feat(mobile): ...`
  - `fix(sync): ...`
  - `fix(timer): ...`

Reglas:
- Commits coherentes por intención.
- Evitar commits masivos sin explicación.
- Separar cambios funcionales de limpieza técnica.
