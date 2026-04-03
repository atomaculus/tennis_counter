# Politica de Privacidad de PLAYCE

Ultima actualizacion: 3 de abril de 2026

PLAYCE es una app Android compuesta por:
- una app Wear OS para llevar el marcador en vivo de tenis o padel
- una app movil complementaria para historial, detalle de partidos, visualizacion en vivo y compartir resultados

Esta politica describe como PLAYCE trata los datos en el estado actual del codigo.

## Datos que procesa la app

PLAYCE puede procesar las siguientes categorias de datos generados por el uso de la app:
- datos del marcador del partido, incluyendo puntos, games, sets y resultado final
- duracion del partido
- marcas de tiempo usadas para historial local y reintentos de sincronizacion
- identificadores tecnicos de sincronizacion como `idempotencyKey`
- estado de compra necesario para habilitar funciones Premium mediante Google Play Billing

## Para que se usan esos datos

La app usa esos datos para:
- ejecutar el contador en vivo en Wear OS y mobile
- sincronizar resultados entre el reloj y el telefono Android vinculado
- guardar historial local de partidos en el telefono cuando existe acceso Premium
- evitar inserciones duplicadas durante la sincronizacion reloj -> telefono
- generar una imagen compartible del resultado cuando eliges compartirla explicitamente
- restaurar y validar el entitlement Premium a traves de Google Play Billing

## Comparticion de datos

PLAYCE no opera un backend propio para almacenar partidos.

Los datos solo se comparten en estos casos:
- entre el reloj y el telefono vinculado mediante Google Play Services Wearable Data Layer
- con Google Play Billing para procesar la compra Premium
- con otra app unicamente si usas de forma explicita el selector de compartir de Android

PLAYCE no vende tus datos.

## Diagnosticos y analitica

El proyecto actual incluye servicios de Firebase en ambos modulos de la app.

- Firebase Crashlytics puede recopilar informacion tecnica como trazas de error, version de la app, modelo del dispositivo, version del sistema operativo y otros metadatos tecnicos relacionados para mejorar la estabilidad.
- Firebase Analytics puede procesar informacion general de uso relacionada con como se utiliza la app y como funcionan los flujos del producto.

Cualquier release publica debe mantener esta politica alineada con la configuracion real de la build que se sube a Google Play.

## Almacenamiento local

PLAYCE guarda datos localmente en tus dispositivos:
- telefono: base de datos Room local para historial y metadatos del partido
- reloj: estado local de sincronizacion pendiente para reintentos y ACK

Puedes eliminar los datos almacenados limpiando los datos de la app o desinstalandola.

## Datos que PLAYCE no solicita directamente

En la implementacion actual, PLAYCE no requiere directamente:
- creacion de cuenta
- ubicacion precisa
- contactos
- microfono
- camara

## Contacto

Email de soporte: `atiliogmaculus@gmail.com`
