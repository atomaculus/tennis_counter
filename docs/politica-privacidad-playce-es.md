# Política de Privacidad de PLAYCE

Última actualización: 27 de marzo de 2026

PLAYCE es una app Android compuesta por:
- una app Wear OS para llevar el marcador en vivo de tenis o pádel
- una app móvil complementaria para historial, detalle de partidos, visualización en vivo y compartir resultados

Esta política describe cómo PLAYCE trata los datos en el estado actual del código.

## Datos que procesa la app

PLAYCE puede procesar las siguientes categorías de datos generados por el uso de la app:
- datos del marcador del partido, incluyendo puntos, games, sets y resultado final
- duración del partido
- marcas de tiempo usadas para historial local y reintentos de sincronización
- identificadores técnicos de sincronización como `idempotencyKey`
- estado de compra necesario para habilitar funciones Premium mediante Google Play Billing

## Para qué se usan esos datos

La app usa esos datos para:
- ejecutar el contador en vivo en Wear OS y mobile
- sincronizar resultados entre el reloj y el teléfono Android vinculado
- guardar historial local de partidos en el teléfono cuando existe acceso Premium
- evitar inserciones duplicadas durante la sincronización reloj -> teléfono
- generar una imagen compartible del resultado cuando eliges compartirla explícitamente
- restaurar y validar el entitlement Premium a través de Google Play Billing

## Compartición de datos

PLAYCE no opera un backend propio para almacenar partidos.

Los datos solo se comparten en estos casos:
- entre el reloj y el teléfono vinculado mediante Google Play Services Wearable Data Layer
- con Google Play Billing para procesar la compra Premium
- con otra app únicamente si usas de forma explícita el selector de compartir de Android

PLAYCE no vende tus datos.

## Diagnóstico y fallos

El reporte de crashes es opcional en este proyecto y está controlado por la propiedad Gradle `enableCrashlytics`.

- Si `enableCrashlytics=false`, Firebase Crashlytics no queda activado en la build publicada.
- Si `enableCrashlytics=true`, Firebase Crashlytics puede recopilar información técnica como trazas de error, versión de la app, modelo del dispositivo y versión del sistema operativo para mejorar la estabilidad.

Cualquier release pública debe mantener esta política alineada con la configuración real de la build que se sube a Google Play.

## Almacenamiento local

PLAYCE guarda datos localmente en tus dispositivos:
- teléfono: base de datos Room local para historial y metadatos del partido
- reloj: estado local de sincronización pendiente para reintentos y ACK

Puedes eliminar los datos almacenados limpiando los datos de la app o desinstalándola.

## Datos que PLAYCE no solicita directamente

En la implementación actual, PLAYCE no requiere directamente:
- creación de cuenta
- ubicación precisa
- contactos
- micrófono
- cámara

## Contacto

Email de soporte: `atiliogmaculus@gmail.com`
