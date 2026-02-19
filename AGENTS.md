# AGENTS.md

Guía operativa para Codex y cualquier otro agente de IA que trabaje en este repositorio.

## 1. Descripción General del Proyecto

`tennis_counter` es una app Android multi-módulo para:
- **Wear OS** (`:app`): contador de tenis, cierre de partido y envío de resultado al teléfono.
- **Mobile** (`:mobile`): historial local de partidos, detalle y share de imagen, con recepción de eventos desde Wear.

Estado funcional actual:
- Marcador de tenis con lógica de points/games/sets (incluye deuce/advantage).
- Timer persistente en Wear con control explícito de ciclo de vida.
- UX principal en Wear:
  - Tap para sumar puntos.
  - Long press por jugador para undo contextual.
  - Long press simultáneo (A+B) para acciones admin (reset game/reset match).
- Flujo manual de finalización:
  - Botón `END MATCH` + confirmación.
  - Pantalla final `MATCH FINISHED`.
  - Botón `SAVE MATCH`.
- Sync Wear -> Mobile activo por Data Layer (`MessageClient`).
- Mobile recibe, deduplica e inserta en Room (`MatchRepository`).

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
- Valor actual en ambos: `com.example.tenniscounter`.

Notas:
- `:mobile` mantiene `namespace = "com.example.tenniscounter.mobile"`.
- `namespace` y `applicationId` no tienen que ser iguales entre sí, pero para Data Layer importa el `applicationId`.

---

## 3. Estructura Relevante

### Wear
- `app/src/main/java/com/example/tenniscounter/MainActivity.kt`
  - UI Wear Compose.
  - Handler de `SAVE MATCH` y envío Data Layer (`/match_finished`).
- `app/src/main/java/com/example/tenniscounter/ui/TennisViewModel.kt`
  - Lógica de score, timer, summary final y save local.
- `app/src/main/java/com/example/tenniscounter/ui/TimerStateStore.kt`
  - Estado persistido del timer (`isRunning`, `startElapsedRealtime`, `accumulatedSeconds`) + migración de keys legacy.
- `app/src/main/java/com/example/tenniscounter/timer/MatchTimerService.kt`
  - Service del timer con `onTaskRemoved()` para consolidar tiempo y frenar el timer.
- `app/src/main/AndroidManifest.xml`
  - Registro de `MatchTimerService`.

### Mobile
- `mobile/src/main/java/com/example/tenniscounter/mobile/sync/WearMatchListenerService.kt`
  - Receptor Data Layer (`WearableListenerService`).
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
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/detail/MatchDetailScreen.kt`
  - Muestra resultado global + detalle de sets.
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/share/ShareCard.kt`
  - Render de imagen shareable incluyendo detalle de sets.
- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/share/MatchShareManager.kt`
  - Prepara datos del share card.
- `mobile/src/main/AndroidManifest.xml`
  - Registro correcto del listener de Wear.

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

---

## 5. Manifest del Listener (Mobile)

El `service` de listener debe cumplir:
- `android:exported="true"`
- `intent-filter` con acción:
  - `com.google.android.gms.wearable.BIND_LISTENER`
- **NO** declarar:
  - `android:permission="com.google.android.gms.wearable.BIND_LISTENER"`

Ejemplo correcto:
```xml
<service
    android:name=".sync.WearMatchListenerService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.BIND_LISTENER" />
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
- El timer auto-arranca al entrar a `CounterScreen` con scoreboard nuevo (0-0) y timer en 0 detenido.
- Si hubo `task removed` y quedó tiempo viejo con scoreboard nuevo:
  - se resetea primero (`reset timer on fresh scoreboard after task removed`) y luego auto-start.
- `New Match` mantiene reset + start.
- `MatchTimerService.onTaskRemoved()` consolida tiempo, setea `isRunning=false` y `stopSelf()`.

Logs clave timer:
- `reset timer on fresh scoreboard after task removed`
- `auto-start timer on entering match screen`
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

Nota tras cambio de `applicationId` de mobile:
- Si Android Studio intenta lanzar `com.example.tenniscounter.mobile/...` y falla, recrear Run Configuration.
- Componente esperado de mobile:
  - paquete app: `com.example.tenniscounter`
  - activity: `com.example.tenniscounter.mobile.MainActivity`

---

## 8. Observabilidad / Debug

Tags de logs:
- Wear envío: `WearDataLayer`
- Wear timer/service: `MatchTimer`, `MatchTimerService`
- Mobile recepción: `WearMatchListener`

Casos de diagnóstico:
- Wear:
  - `connectedNodes count=0` => teléfono no detectable por Data Layer.
  - envío exitoso => log `Sent /match_finished`.
- Mobile:
  - `onMessageReceived path=/match_finished ...`
  - `Decoded payload ... durationSeconds=...`
  - `Match inserted...` o `Duplicate match ignored...`

---

## 9. Reglas para Agentes de IA

- Leer siempre `AGENTS.md` antes de modificar código.
- Mantener cambios incrementales y no romper lógica existente.
- En Data Layer, verificar siempre:
  - `applicationId` alineado entre módulos.
  - manifest de listener correcto.
  - contrato de ruta y keys.
- Para cambios de Room:
  - agregar migración explícita si cambia schema.
- Para cambios de timer Wear:
  - respetar comportamiento `stop on task removed`.
  - no introducir resets silenciosos fuera de reglas UX definidas.

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
