# AGENTS.md

Guía operativa para Codex y cualquier otro agente de IA que trabaje en este repositorio.

## 1. Descripción General del Proyecto

`tennis_counter` es una app Android multi-módulo para:
- **Wear OS** (`:app`): contador de tenis y cierre de partido.
- **Mobile** (`:mobile`): historial local de partidos y recepción de eventos desde Wear.

Estado funcional actual:
- Marcador de tenis con lógica de points/games/sets (incluye deuce/advantage).
- Timer de partido con persistencia base usando `DataStore` en Wear.
- UX principal en Wear:
  - Tap para sumar puntos.
  - Long press por jugador para undo contextual.
  - Long press simultáneo (A+B) para acciones admin (reset game/reset match).
- Flujo manual de finalización:
  - Botón `END MATCH` + confirmación.
  - Pantalla final `MATCH FINISHED`.
  - Botón `SAVE MATCH`.
- Guardado local del resultado final en Wear (`DataStore`).
- **Sincronización Wear -> Mobile activa** por Data Layer (`MessageClient`) al tocar `SAVE MATCH`.
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
- `:mobile` mantiene `namespace = "com.example.tenniscounter.mobile"` (válido).
- `namespace` y `applicationId` no tienen que ser iguales entre sí, pero para Data Layer importa el `applicationId`.

---

## 3. Estructura Relevante

- `app/src/main/java/com/example/tenniscounter/MainActivity.kt`
  - UI Wear Compose y flujo `SAVE MATCH`.
  - Envío Data Layer a Mobile en ruta `/match_finished`.
- `app/src/main/java/com/example/tenniscounter/ui/TennisViewModel.kt`
  - Lógica de score/timer/save local.
- `mobile/src/main/java/com/example/tenniscounter/mobile/sync/WearMatchListenerService.kt`
  - Listener de mensajes Data Layer (`WearableListenerService`).
- `mobile/src/main/java/com/example/tenniscounter/mobile/data/local/MatchDao.kt`
  - Query de deduplicación por campos.
- `mobile/src/main/java/com/example/tenniscounter/mobile/data/MatchRepository.kt`
  - Inserción con heurística anti-duplicados.
- `mobile/src/main/AndroidManifest.xml`
  - Registro del listener service de Wear.

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
  - valor actual: `summary.setsScore` (compacto y consistente con resultado final visible).
- `idempotencyKey` (`String`, UUID)

Comportamiento en Mobile:
- Si `path != "/match_finished"`: ignorar.
- Parseo de `DataMap.fromByteArray(messageEvent.data)`.
- Inserción en Room vía `MatchRepository`.
- Deduplicación heurística:
  - No inserta si ya existe match con mismo:
    - `createdAt`
    - `durationSeconds`
    - `finalScoreText`

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

## 6. Build y Ejecución

### Build por consola
Windows (PowerShell):
```bash
.\gradlew.bat :app:assembleDebug :mobile:assembleDebug
```

### Ejecución en Android Studio
- `:app` en dispositivo/emulador Wear.
- `:mobile` en teléfono.

Nota importante tras cambios de `applicationId`:
- Si Android Studio intenta lanzar `com.example.tenniscounter.mobile/...` y falla, recrear Run Configuration.
- Componente esperado de mobile:
  - paquete app: `com.example.tenniscounter`
  - activity: `com.example.tenniscounter.mobile.MainActivity`

---

## 7. Observabilidad / Debug

Tags de logs:
- Wear envío: `WearDataLayer`
- Mobile recepción: `WearMatchListener`

Casos clave de diagnóstico:
- Wear:
  - `connectedNodes count=0` => teléfono no detectable por Data Layer.
  - envío exitoso => log `Sent /match_finished`.
- Mobile:
  - `onMessageReceived path=/match_finished ...`
  - `Match inserted...` o `Duplicate match ignored...`

---

## 8. Reglas para Agentes de IA

- Leer siempre `AGENTS.md` antes de modificar código.
- Mantener cambios incrementales por fases y mostrar diff cuando se solicite.
- No romper la lógica existente de Wear.
- En tareas de Data Layer, verificar siempre:
  - `applicationId` alineado entre módulos.
  - manifest del listener correcto.
  - contrato de ruta y keys.
- No introducir migraciones de Room sin pedido explícito.

---

## 9. Convenciones de Commit

Formato sugerido:
- `feat: ...`
- `fix: ...`
- `refactor: ...`
- opcional por alcance:
  - `feat(wear): ...`
  - `feat(mobile): ...`
  - `fix(sync): ...`

Reglas:
- Commits coherentes por intención.
- Evitar commits masivos sin explicación.
- Separar cambios funcionales de limpieza técnica.
