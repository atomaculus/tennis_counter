# Cosas a llevar a Android

## Formato de resultado de partido desde Garmin

Decision de producto tomada despues de validar reloj Garmin + telefono real:

- el formato en que Garmin envia y muestra el resultado final del partido en Android gusta mas que el formato actual que llega desde Wear OS
- este formato pasa a ser el default deseado para Android

Alcance de la decision:

- aplicar este formato como referencia para el flujo de partidos terminados en `tennis_counter`
- usarlo como baseline para lo que llega al historial y a cualquier pantalla/resumen equivalente del lado Android
- cuando haya divergencia entre el formato actual de Wear OS y el formato validado desde Garmin, priorizar el formato Garmin

Que se valoro del formato Garmin:

- mejor lectura combinada de sets, games y points
- mejor sensacion de resumen de partido terminado
- mejor resultado visual y funcional que el formato actual heredado desde Wear OS

Nota:

- esto no implica cambiar el contrato tecnico del bridge inmediatamente
- implica que, cuando se haga la pasada de producto/UI sobre Android, el formato Garmin debe tomarse como target por defecto
