# AGENTS.md

Guía operativa para Codex y cualquier otro agente de IA que trabaje en este repositorio.

## 1. Descripción General del Proyecto

`tennis_counter` es una app Android para **Wear OS** que funciona como contador de tenis en reloj.

Estado funcional actual:
- Marcador de tenis con lógica de points/games/sets (incluye deuce/advantage).
- Timer de partido con persistencia base usando `DataStore`.
- Interacciones de UX en pantalla principal:
  - Tap para sumar puntos.
  - Long press por jugador para undo contextual.
  - Long press simultáneo (A+B) para acciones admin (reset game/reset match).
- Flujo manual de finalización:
  - Botón `END MATCH` + confirmación.
  - Pantalla final `MATCH FINISHED` con resumen y acciones.
- Guardado local mínimo del resultado final en `DataStore`.
- Stub de share preparado para futura integración con teléfono.

### Módulos actuales
- Solo existe **`:app`**.

### Estado actual del proyecto
- Proyecto en iteración activa de UX/funcionalidad para Wear OS.
- No existe módulo móvil implementado aún.

### Próximo paso previsto
- Próximamente se desarrollará **`:mobile`** (teléfono), pero actualmente no está incluido en `settings.gradle.kts`.

---

## 2. Estructura del Repositorio

Estructura principal detectada:
- `app/`
  - Único módulo Android actualmente.
  - Implementación Wear OS.
- `app/src/main/java/com/example/tenniscounter/`
  - `MainActivity.kt`: UI Compose + flujo principal y pantalla final.
- `app/src/main/java/com/example/tenniscounter/ui/`
  - `TennisViewModel.kt`: estado, lógica de marcador/timer y guardado final.
- `app/src/main/res/values/`
  - Recursos básicos (`strings.xml`, `themes.xml`).
- `app/src/main/AndroidManifest.xml`
  - Configuración de app Wear (`android.hardware.type.watch`).
- `build.gradle.kts` (raíz)
  - Plugins Android/Kotlin.
- `settings.gradle.kts`
  - Incluye solo `:app`.
- `gradle/`, `gradlew`, `gradlew.bat`
  - Wrapper y tooling de Gradle.

### Nota de higiene del repositorio
El repositorio puede contener archivos generados por el entorno de desarrollo.  
Cualquier tarea de saneamiento (`.gitignore` / limpieza de artefactos) debe realizarse en un commit separado y coordinado explícitamente.

### Diferenciación de módulos (estado actual)
- Wear OS: `:app` (actual).
- Mobile: no existe todavía (futuro `:mobile`).
- Compartidos: no existe módulo shared actualmente.

---

## 3. Cómo Construir y Ejecutar el Proyecto

### Abrir en Android Studio
1. Abrir la carpeta raíz del repo (`tennis_counter`).
2. Esperar sync de Gradle.
3. Seleccionar dispositivo/emulador Wear OS para ejecutar `:app`.

### Build por consola

Windows (PowerShell):
```bash
.\gradlew.bat :app:assembleDebug
```

Build completo:
```bash
.\gradlew.bat build
```

### Ejecutar app Wear
- Desde Android Studio: seleccionar configuración `app` y target Wear OS.
- Verificar que el dispositivo destino sea reloj físico o emulador Wear.

### Ejecutar futura app Mobile (cuando exista)
Cuando exista `:mobile`, el comando esperado será:
```bash
.\gradlew.bat :mobile:assembleDebug
```
Hoy ese comando fallará porque el módulo aún no existe.

### Configuración relevante
- `compileSdk = 34`
- `minSdk = 30`
- Kotlin/JVM target 17
- Compose habilitado en `:app`
- Si falla build por Java, configurar `JAVA_HOME` localmente.

---

## 4. Arquitectura y Convenciones

### Stack principal
- Lenguaje: **Kotlin**
- UI: **Jetpack Compose** (Wear Compose Material)
- Estado UI: `StateFlow` en ViewModel consumido con `collectAsState`

### Patrón detectado
- Predominio **MVVM** (ViewModel + UI reactiva).

### Convenciones de paquetes
- `com.example.tenniscounter`: actividad principal y composición de pantallas.
- `com.example.tenniscounter.ui`: modelos, estado y lógica de negocio del marcador.

### Reglas de navegación
- Navegación actual sin Navigation Compose formal.
- Se maneja por estado de pantalla (por ejemplo, enum `AppScreen`).
- Mantener navegación simple y estable hasta decisión explícita de migración.

### Convenciones de naming
- Clases/modelos: `PascalCase`
- Funciones/propiedades: `camelCase`
- Estados/acciones UI con nombres explícitos y legibles.

### Buenas prácticas obligatorias
- No mover lógica de negocio a Composables.
- Mantener la lógica de marcador/timer en ViewModel.
- Preservar feedback háptico y UX existente.
- Priorizar legibilidad y simplicidad para uso en Wear bajo movimiento.
- Hacer cambios pequeños, incrementales y fáciles de revisar.
- Regla crítica: **no romper funcionalidad existente en Wear OS**.

## Estrategia futura de modularización
Cuando se implemente el módulo :mobile:
- Evitar duplicar lógica de marcador o timer.
- Evaluar crear un módulo :shared si hay lógica compartida entre wear y mobile.
- Mantener separación clara entre capas de dominio y UI.
- Definir contratos explícitos entre módulos si hay sincronización futura.

---

## 5. Testing (Estado Actual)

Estado actual:
- El testing es **manual** desde Android Studio (emulador/dispositivo).
- No hay tests automatizados configurados aún.

Evolución futura recomendada:
- Unit tests (JUnit) para lógica de marcador/timer/undo/reset.
- Tests de UI con Compose Testing para flujos críticos.
- Para nuevas funcionalidades importantes, considerar tests básicos desde el inicio.

---

## 6. Reglas para Agentes de IA

- Leer siempre `AGENTS.md` antes de modificar código.
- Mantener consistencia entre módulos actuales y futuros.
- No duplicar lógica si puede compartirse.
- Evitar cambios innecesarios en archivos no relacionados.
- Respetar contratos entre UI y ViewModel.
- Hacer cambios incrementales y explicarlos claramente.
- Si una capacidad aún no existe (por ejemplo `:mobile` o share real), usar stubs seguros sin romper runtime.
- No asumir infraestructura inexistente; declarar límites explícitamente.

## Uso de contexto en sesiones largas
- Priorizar leer archivos del repositorio antes que depender de historial de conversación.
- Utilizar AGENTS.md como fuente de verdad.
- Trabajar en cambios incrementales y bloques pequeños cuando los contextos son extensos.

---

## 7. Convenciones de Commits

Usar mensajes claros, descriptivos y acotados al cambio.

Formato sugerido:
- `feat: ...`
- `fix: ...`
- `refactor: ...`
- Opcional por alcance: `feat(wear): ...`, `fix(ui): ...`

Reglas:
- Evitar commits masivos sin explicación.
- Un commit debe representar una intención coherente.
- Separar commits funcionales de commits de saneamiento técnico.
