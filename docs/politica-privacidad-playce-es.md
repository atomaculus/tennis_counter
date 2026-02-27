# Política de Privacidad de PLAYCE

**Última actualización:** 26 de febrero de 2026

## 1. Introducción

Esta Política de Privacidad describe cómo PLAYCE (la "Aplicación") trata la información cuando usas:

- la app móvil Android (`:mobile`)
- la app Wear OS (`:app`)

La Aplicación está diseñada para llevar un contador de tenis, guardar historial de partidos en el teléfono y sincronizar resultados entre reloj Wear OS y teléfono Android.

## 2. Responsable

**Desarrollador / Responsable:** `Atilio Maculus`  
**Marca:** PLAYCE  
**Correo de contacto:** `atiliogmaculus@gmail.com`

Importante:
- El correo de contacto debe coincidir con el canal de soporte que uses en Google Play Console.

## 3. Qué datos trata la Aplicación

La Aplicación puede tratar las siguientes categorías de datos, según las funciones que uses:

### 3.1. Datos de partidos (generados por el usuario)

Se generan y/o guardan datos relacionados con el uso del contador de tenis, por ejemplo:

- marcador final del partido (`finalScoreText`)
- detalle de sets (`setScoresText`, cuando aplica)
- duración del partido (`durationSeconds`)
- fecha/hora de creación del registro (`createdAt`)
- identificador técnico para deduplicación/sincronización (`idempotencyKey`)

Estos datos se usan para:

- mostrar el resultado del partido
- guardar historial local en el teléfono (función Premium)
- sincronizar el resultado entre Wear OS y móvil
- evitar duplicados durante la sincronización

### 3.2. Datos de sincronización entre reloj y teléfono (Wear OS / Android)

Cuando guardas un partido desde el reloj, la Aplicación envía datos al teléfono mediante Google Play Services (Wearable Data Layer / `MessageClient`), incluyendo:

- datos del partido (marcador, sets, duración, fecha)
- identificador técnico de idempotencia
- mensajes de confirmación (ACK) de recepción/estado (`inserted`, `duplicate`, `premium_locked`)

La sincronización se realiza entre tus dispositivos vinculados (reloj y teléfono compatibles).

### 3.3. Datos de compra dentro de la app (Google Play Billing)

La Aplicación ofrece una compra única de desbloqueo Premium (`premium_unlock`) mediante Google Play Billing.

La Aplicación puede tratar datos de estado de compra necesarios para habilitar funciones Premium, por ejemplo:

- estado de compra/restauración
- identificador del producto (`premium_unlock`)
- token/estado de transacción procesado por Google Play Billing (para validación/ack de compra dentro del flujo del SDK)

Aclaración:
- Los datos de pago (por ejemplo, tarjeta) son procesados por Google Play / Google y no por PLAYCE directamente.

### 3.4. Datos de diagnóstico y fallos (Firebase Crashlytics)

La Aplicación integra Firebase Crashlytics (Google) en módulos móvil y Wear OS para detectar errores y mejorar estabilidad.

Crashlytics puede recopilar información técnica, como:

- reportes de fallos (crashes)
- trazas de error
- versión de app
- versión del sistema operativo
- modelo de dispositivo
- identificadores técnicos del SDK necesarios para agrupar incidentes

Estos datos se usan exclusivamente con fines de diagnóstico, mantenimiento y mejora de la Aplicación.

## 4. Datos que PLAYCE no solicita directamente

Salvo que se indique lo contrario en una versión futura de la app, PLAYCE no solicita directamente:

- creación de cuenta de usuario
- nombre real o documento de identidad
- ubicación precisa (GPS)
- contactos
- micrófono
- cámara

Nota:
- Si utilizas la función de compartir imagen del resultado, la imagen se comparte mediante el selector de Android hacia la app de destino que tú elijas. Ese tratamiento posterior depende de la política de privacidad de la app receptora.

## 5. Dónde se almacenan los datos

### 5.1. Almacenamiento local en tu dispositivo

La información de partidos e historial se guarda localmente en el teléfono (base de datos local/Room) cuando la función está habilitada (Premium).

En el reloj Wear OS también puede existir almacenamiento local temporal de datos técnicos de sincronización pendientes (reintentos/ACK), con el fin de completar el envío al teléfono.

### 5.2. Servicios de terceros

Según la función utilizada, ciertos datos pueden ser tratados por servicios de Google, incluyendo:

- Google Play Services (comunicación Wear OS <-> Android)
- Google Play Billing (compras dentro de la app)
- Firebase Crashlytics (diagnóstico de fallos)

El tratamiento realizado por esos servicios se rige además por las políticas de privacidad y términos de Google/Firebase aplicables.

## 6. Finalidades del tratamiento

PLAYCE trata datos únicamente para las siguientes finalidades:

- operar el contador de tenis y mostrar resultados
- sincronizar resultados entre reloj y teléfono
- guardar historial local de partidos (Premium)
- habilitar y restaurar funciones Premium mediante Google Play Billing
- prevenir duplicados y gestionar reintentos de sincronización
- detectar errores y mejorar la estabilidad/rendimiento de la app

## 7. Compartición de datos

PLAYCE no vende datos personales.

La Aplicación puede compartir información solo en los siguientes casos:

- con servicios de Google necesarios para funciones técnicas (Play Services, Billing, Crashlytics)
- con la app de terceros que tú elijas al usar la función de compartir (Android Share Sheet)
- si fuera requerido por ley, regulación o autoridad competente

## 8. Retención y eliminación de datos

### 8.1. Datos locales (historial y registros de partido)

Los datos guardados localmente permanecen en tu dispositivo hasta que:

- los elimines desde la app (si la funcionalidad está disponible), o
- borres los datos de la app desde Android, o
- desinstales la app

### 8.2. Datos temporales de sincronización (Wear)

Los registros técnicos pendientes de sincronización en el reloj se conservan temporalmente hasta que:

- se recibe confirmación (ACK) del teléfono, o
- se reemplazan por nuevos estados de sincronización, según la lógica de la app

### 8.3. Datos de diagnóstico (Crashlytics)

Los reportes de fallos pueden almacenarse por Firebase/Google conforme a sus políticas de retención y configuración del servicio.

## 9. Seguridad

PLAYCE aplica medidas razonables para reducir riesgos de acceso no autorizado, pérdida o uso indebido de información, incluyendo:

- almacenamiento local en el dispositivo del usuario para el historial de partidos
- uso de APIs oficiales de Android / Google Play Services para sincronización y compras
- uso de identificadores técnicos para deduplicación y control de reintentos

Ningún sistema es 100% seguro; por ello no puede garantizarse seguridad absoluta.

## 10. Privacidad de menores

PLAYCE no está dirigida específicamente a menores de edad sin supervisión de un adulto. Si consideras que un menor proporcionó información de forma inapropiada, contáctanos para evaluar la situación.

## 11. Cambios a esta política

Podemos actualizar esta Política de Privacidad para reflejar cambios en la app, requisitos legales o cambios en servicios de terceros.

Cuando corresponda:

- actualizaremos la fecha de "Última actualización"
- publicaremos la versión vigente en una URL pública (por ejemplo, Google Sites)

## 12. Contacto

Si tienes consultas sobre esta Política de Privacidad o sobre el tratamiento de datos en PLAYCE, contáctanos en:

**Email:** `atiliogmaculus@gmail.com`

## 13. Información adicional para Google Play (referencia operativa)

Esta política está pensada para acompañar la publicación en Google Play y debe mantenerse consistente con:

- el formulario de **Seguridad de los datos (Data safety)** en Google Play Console
- las funciones reales de la app y los SDKs integrados (por ejemplo, Billing / Crashlytics)

Antes de publicar:

- verifica que la URL sea pública y accesible sin login
- revisa que el email de contacto sea correcto
- confirma que la declaración de Data safety coincida con la versión actual de la app
