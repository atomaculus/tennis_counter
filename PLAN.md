# Plan: Sonidos + Live Score + Reloj Espectador

## Estado

Las tres fases principales de este plan ya están implementadas en el repositorio:

- sonidos diferenciados por jugador en Wear
- live score en mobile
- modo spectator en un segundo reloj

Además, el estado actual de Wear ya incluye dos mejoras extra de UX durante el partido:

1. **Indicador de sacador por game**
   - resalta el botón del jugador que está sacando
   - la alternancia sigue el orden completo del partido, también al cambiar de set

2. **Indicador de lado de saque**
   - halo/brillo lateral en la pantalla
   - cambia izquierda/derecha según la paridad de puntos del game actual

Este archivo queda como referencia de la evolución de esas features; el foco actual del proyecto pasó a hardening, calidad de release y UX incremental.

## Resumen de Features

1. **Sonidos en Wear**: beep al sumar punto (distinto para Jugador A vs B)
2. **Live Score en Mobile**: el Counter tab muestra score en vivo del Wear (read-only) + sonidos
3. **Reloj Espectador**: segundo reloj ve el score en vivo (read-only)

## Arquitectura General

```
Watch A (scorer)                    Watch B (spectator)
  |                                        |
  |-- writes DataItem "/playce/live" -->   |
  |                                        |
  |          Phone (observer)              |
  |              |                         |
  <-- onDataChanged --+-- onDataChanged -->
```

- **Watch A (scorer)**: tiene la lógica de scoring (TennisViewModel). Después de cada punto,
  escribe el estado completo del partido en un `DataItem` vía `DataClient.putDataItem()` con `setUrgent()`.
- **Phone**: observa el `DataItem` con un `WearableListenerService`. Cuando cambia, actualiza UI
  y reproduce sonido.
- **Watch B (spectator)**: observa el mismo `DataItem`. Muestra score read-only.

Se usa `DataClient` (no `MessageClient`) porque:
- Broadcast automático a todos los nodos conectados (phone + watch B)
- Persistente (sobrevive desconexiones Bluetooth)
- No requiere relay manual del phone al watch B

El contrato existente de `/match_finished` (MessageClient) NO se toca.

## DataItem Contract

Path: `/playce/live`

```
DataMap {
    "playerA_points": Int       // raw points (0,1,2,3,4,5...)
    "playerA_games": Int
    "playerA_sets": Int
    "playerB_points": Int
    "playerB_games": Int
    "playerB_sets": Int
    "completedSets": String     // "6-4,3-6" (comma-separated)
    "pointLabelA": String       // display label ("0","15","30","40","AD")
    "pointLabelB": String
    "elapsedSeconds": Int
    "isMatchActive": Boolean    // true while match is in progress
    "lastScoredPlayer": String  // "A" or "B" (para saber qué sonido tocar)
    "scorerNodeId": String      // node ID del watch scorer (para que B sepa que no es él)
    "timestamp": Long           // para forzar propagación (DataClient deduplica payloads idénticos)
}
```

Cuando el partido termina o se resetea: `isMatchActive = false` (limpia la UI en phone/spectator).

---

## Branch 1: `feature/point-sounds` (Sonidos en Wear)

### Archivos a crear
- `app/src/main/java/com/example/tenniscounter/sound/PointSoundManager.kt`
  - Wrapper de `ToneGenerator` (no requiere archivos de audio)
  - `playPlayerASound()`: tono agudo corto (ej: `TONE_PROP_BEEP`, freq alta)
  - `playPlayerBSound()`: tono más grave (ej: `TONE_PROP_BEEP2`, freq baja)
  - `release()` para cleanup

### Archivos a modificar
- `app/src/main/java/com/example/tenniscounter/MainActivity.kt`
  - Instanciar `PointSoundManager` en el composable scope
  - Llamar `playPlayerASound()` / `playPlayerBSound()` en los handlers de `addPointToPlayerA()` / `addPointToPlayerB()`
  - Release en `onDispose`

### Sin dependencias nuevas
- `ToneGenerator` es parte del framework Android (no necesita dependencia extra)

---

## Branch 2: `feature/live-score-sync` (Live Score en Mobile + Sonidos)

Se hace merge de `feature/point-sounds` primero, luego se construye encima.

### Wear (broadcaster) - Archivos a crear/modificar

**Crear:**
- `app/src/main/java/com/example/tenniscounter/sync/LiveScoreBroadcaster.kt`
  - Clase que encapsula `DataClient.putDataItem()`
  - Método `broadcastState(matchState, lastScoredPlayer, scorerNodeId)`
  - Método `clearLiveScore()` (setea `isMatchActive = false`)
  - Usa `PutDataMapRequest.create("/playce/live").setUrgent()`

**Modificar:**
- `app/src/main/java/com/example/tenniscounter/MainActivity.kt`
  - Instanciar `LiveScoreBroadcaster`
  - Obtener `localNodeId` vía `Wearable.getNodeClient().localNode`
  - Después de cada `addPointToPlayerA()` / `addPointToPlayerB()` → llamar `broadcastState()`
  - En `resetMatch()` / `finishMatch()` → llamar `clearLiveScore()`
  - En `onDispose` → `clearLiveScore()`

### Mobile (observer) - Archivos a crear/modificar

**Crear:**
- `mobile/src/main/java/com/example/tenniscounter/mobile/sync/LiveScoreListenerService.kt`
  - Extiende `WearableListenerService`
  - Override `onDataChanged()`: parsea DataItem en `/playce/live`
  - Publica en un singleton `LiveScoreRepository` (StateFlow)

- `mobile/src/main/java/com/example/tenniscounter/mobile/sync/LiveScoreRepository.kt`
  - Singleton con `StateFlow<LiveMatchState?>`
  - `LiveMatchState` data class con todos los campos del DataMap
  - `null` = no hay partido activo

- `mobile/src/main/java/com/example/tenniscounter/mobile/sound/PointSoundManager.kt`
  - Mismo concepto que en Wear: `ToneGenerator` con tonos distintos para A/B
  - Se usa desde la UI cuando `lastScoredPlayer` cambia

- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/counter/LiveScoreScreen.kt`
  - Composable read-only que muestra el score en vivo
  - Misma estética del counter actual pero sin botones de sumar/undo
  - Muestra: puntos, games, sets, sets completados, timer
  - Indicador visual "LIVE" (pulsante, color Accent)
  - Colores de jugadores: lime para A, blanco para B (consistente con counter)

**Modificar:**
- `mobile/src/main/AndroidManifest.xml`
  - Registrar `LiveScoreListenerService` con intent-filter para DataChanged
  ```xml
  <service android:name=".sync.LiveScoreListenerService" android:exported="true">
      <intent-filter>
          <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
          <data android:scheme="wear" android:host="*" android:pathPrefix="/playce/live" />
      </intent-filter>
  </service>
  ```

- `mobile/src/main/java/com/example/tenniscounter/mobile/MobileApp.kt`
  - En el Counter tab: si `LiveScoreRepository.state != null` → mostrar `LiveScoreScreen`
  - Si no hay partido activo → mostrar `MobileCounterScreen` (actual, sin cambios)

- `mobile/src/main/java/com/example/tenniscounter/mobile/ui/counter/MobileCounterScreen.kt`
  - Agregar observación de `LiveScoreRepository` + lógica de switch (o se hace en MobileApp.kt)

---

## Branch 3: `feature/spectator-watch` (Reloj Espectador)

Se hace merge de `feature/live-score-sync` primero.

### Wear (spectator mode) - Archivos a crear/modificar

**Crear:**
- `app/src/main/java/com/example/tenniscounter/ui/SpectatorScreen.kt`
  - Composable Wear read-only que muestra el score en vivo
  - Estética similar al scoreboard del scorer pero sin tap zones
  - Indicador "WATCHING LIVE" + badge pulsante
  - Sonidos al cambiar score (reutiliza `PointSoundManager`)

- `app/src/main/java/com/example/tenniscounter/sync/LiveScoreObserver.kt`
  - Observa `DataClient` para cambios en `/playce/live`
  - Filtra: solo muestra si `scorerNodeId != localNodeId` (no soy yo el scorer)
  - Expone `StateFlow<LiveMatchState?>`

**Modificar:**
- `app/src/main/java/com/example/tenniscounter/MainActivity.kt`
  - Al iniciar la app, chequear si hay un DataItem activo en `/playce/live` con `isMatchActive=true` y `scorerNodeId != myNodeId`
  - Si sí: mostrar opción "Join as Spectator" / "Watch Live" en la pantalla inicial
  - Si el usuario elige spectator → navegar a `SpectatorScreen`
  - Si el usuario elige "New Match" → entrar al flow normal de scorer
  - Mantener chequeo periódico: si el scorer termina el match, volver a pantalla inicial

---

## Orden de Implementación

```
main
  └── feature/point-sounds        ← PR #1 (aislado, se puede testear solo)
       └── feature/live-score-sync ← PR #2 (requiere #1)
            └── feature/spectator-watch ← PR #3 (requiere #2)
```

Cada branch se testea antes de mergear. Cada PR es independiente y funcional.

## Qué NO se toca
- Lógica de scoring (TennisViewModel, MobileCounterViewModel)
- Contrato de sync `/match_finished` y `/match_finished_ack`
- PendingMatchStore / WearAckListenerService
- Room DB / MatchRepository
- Premium billing
- History / Share / MatchDetail screens
