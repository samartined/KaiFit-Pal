# Revisión final del asesor experto

Revisión independiente y adversarial de `feat/ui-modernization` en `9208a82`, hecha el 10 de octubre de 2026 por el asesor experto (Claude Fable, esfuerzo `xhigh`, solo lectura) a petición del operador, al terminar las seis fases. Se reproduce sin cambios de fondo. El estado de cada hallazgo se sigue en `HANDOFF.md`.

## Veredicto global

El trabajo es sólido y el lavado de cara es convincente. El dominio es correcto (recalculado de forma independiente), la arquitectura es proporcionada, los 409 tests prueban lo que dicen probar, la CI está en verde y no hay secretos ni logs.

- **Listo para pruebas en dispositivo**: sí, ya.
- **Fusionar en `main`**: después de la primera prueba real de Kai-Q en un móvil y tras corregir los dos hallazgos «mayor» (M1 y M2), que son cambios pequeños y localizados en `LiteRtLmRepository` y `AssistantViewModel`.
- No hay nada bloqueante verificable en el código; el bloqueo real es que el motor nativo nunca se ha ejecutado.

Verificado por el asesor, sin fiarse de los informes: fórmulas y valores de referencia con cálculo propio; API y semántica de LiteRT-LM 0.18.0 leyendo `Conversation.kt`, `Engine.kt` y `jni/litertlm.cc`; que la URL fijada del modelo responde con `x-linked-size: 2588147712` y SHA-256 `181938…a63c`; que 0.18.0 sigue siendo la última versión; las reglas R8 efectivas en `configuration.txt`; el `action.yml` real de `setup-gradle@v6`; la autoría de los 78 commits; y el contenido del APK de release (51 MB sin firmar, 4 ABIs).

## Hallazgos

### Bloqueante

Ninguno verificado.

### Mayor

**M1. Cierre de la conversación nativa sin esperar al fin de la generación (posible uso tras liberar)**
- Fichero: `app/src/main/kotlin/com/tfg/kaifit_pal/data/llm/LiteRtLmRepository.kt`, `closeConversationLocked`, llamado desde `initialize`, `startConversation` y `close`.
- `cancelProcess()` es asíncrono: solo pide la cancelación y vuelve; el `onError(CancellationException)` llega después en el hilo nativo. Acto seguido se llama a `close()`, que borra el objeto C++ sin esperar. Si el destructor no espera al hilo de generación, se libera el objeto mientras el hilo aún lo usa.
- Escenarios: «Detener» y enseguida «Nueva conversación»; cambiar GPU o CPU en Ajustes con una respuesta en curso; «Borrar modelo» con una respuesta en curso; `onTrimMemory` en segundo plano con una respuesta en curso. Además, `onStop()` llama a `getTokenCount()` mientras el hilo aún cancela.
- No verificado: si el destructor del core C++ hace join (el core no estaba disponible). AI Edge Gallery evita el caso desde la UI.
- Origen: el ejemplo de referencia de la investigación de la Fase 5 ya hacía `cancelProcess(); close()`.
- Corrección: guardar un `CompletableDeferred<Unit>` de «generación en vuelo» que completen `onDone` y `onError`; hacer `closeConversationLocked` `suspend` y, tras `cancelProcess()`, esperar con `withTimeoutOrNull(3_000) { inFlight?.await() }` antes de `close()`. La misma espera antes de `getTokenCount()`. En el ViewModel, deshabilitar «Nueva conversación» y el selector de procesador mientras se genera.

**M2. En Android 14 o superior el motor se libera y el chat se vacía casi cada vez que la app pasa a segundo plano**
- Ficheros: `app/src/main/kotlin/com/tfg/kaifit_pal/KaiFitApp.kt` (`level >= TRIM_MEMORY_BACKGROUND`) y `ui/assistant/AssistantViewModel.kt` (`resetEngineState(ENGINE_RELEASED)` vacía los mensajes).
- Con `targetSdk 36`, Android 14+ solo envía `TRIM_MEMORY_UI_HIDDEN` y `TRIM_MEMORY_BACKGROUND`, y este último llega cuando el proceso pasa a la lista de procesos en caché, poco después de cambiar de app. El usuario vuelve a los pocos segundos y ve «Preparando Kai-Q…» con el chat borrado. La orden de la Fase 5 quería evitar justo eso.
- No verificado en dispositivo.
- Corrección: no borrar el historial al liberar; conservar los mensajes en el ViewModel y recrear la conversación con `ConversationConfig(initialMessages = historial)`; o liberar solo tras un periodo de gracia en segundo plano (2–5 min con `ProcessLifecycleOwner`), cancelado al volver.

### Menor

- **m1. Verificación del modelo sin SHA-256 y sin salida desde Kai-Q ante un modelo corrupto** (`DownloadModelManager.kt`). Un fichero del tamaño correcto pero corrupto llega al cargador nativo; la tarjeta de error solo ofrece reintentar o usar CPU. Corrección: hash en segundo plano tras el renombrado contra el SHA-256 publicado, o al menos sugerir borrar el modelo en Ajustes desde la tarjeta de error.
- **m2. RAM recomendada de 6 GB frente a los 8 GB que pide AI Edge Gallery** para Gemma 4 E2B. Plausible sin visión ni audio, pero es la hipótesis con más riesgo en gama media. Comprobar en un móvil de 6 GB.
- **m3. ABIs del release**: el APK de release lleva la librería `x86_64` (26 MB, solo emuladores). Para distribuir: App Bundle o `splits.abi`, o `abiFilters = arm64-v8a` solo en release.
- **m4. La CI no compila release**: solo `assembleDebug`, así que las reglas de R8 solo se han comprobado en local. Añadir `assembleRelease`. Además, `setup-gradle@v6` usa por defecto la caché comercial y las acciones van por tag mayor, no por SHA.
- **m5. Instalación encima de la 1.0.0**: sin `signingConfig` (correcto para un TFG). Si el móvil tiene la 1.0.0 compilada en otra máquina, el APK de la CI no se instala encima: hay que desinstalar antes.
- **m6. Sin tests de los adaptadores Android reales**: `AndroidSystemDownloads`, `UriImportSource` y `AndroidDeviceInfo`. Son las piezas que pueden comportarse distinto en un dispositivo. Robolectric (`ShadowDownloadManager`) cubre al menos `enqueue` y `query`.
- **m7. Cambio de procesador durante «Preparando Kai-Q…»**: cancelar el job no cancela `Engine.initialize()`, así que se encadenan dos cargas completas. Correcto pero lento; deshabilitar el selector mientras carga.
- **m8. Notificación del sistema al terminar la descarga**: con `VISIBILITY_VISIBLE_NOTIFY_COMPLETED`, tocar «Descarga completada» intenta abrir un `.part` que nada abre. Usar `VISIBILITY_VISIBLE` o un receptor que la reconcilie.

### Nota

- n1. Tras «Borrar modelo», Kai-Q muestra la tarjeta «sin modelo» y además el aviso «motor liberado». Cosmético.
- n2. El splash y el fondo de ventana siguen el modo noche del sistema, no el de Ajustes (ya conocido).
- n3. El prompt pide texto plano, pero Gemma puede emitir Markdown y la burbuja mostraría asteriscos. Aceptable.
- n4. Con el «Gestor de descargas» del sistema deshabilitado, la descarga falla con un error genérico; «Importar» es la salida.
- n5. Importar solo comprueba la cabecera: el parser nativo es la frontera de confianza frente a un fichero malicioso. Asumible.

## Verificación del dominio

Todo correcto. Recalculado: US Navy en pulgadas con las constantes originales da 16,1508 / 24,5635 / 10,9838 / 27,9720 % (la variante métrica daría 16,11 / 24,33; la decisión de conservar la del original es coherente y los tests la fijan). Katch-McArdle 80 kg al 20 % da 1752,4 y ×1,55 = 2716. Mifflin 30/180/80 da 1780 y 2759; mujer 30/165/60 da 1320,25 y 1584. Casos extremo a extremo 2819 y 1617. Macros a 2500 kcal: 188/69/282, 219/69/251, 156/56/343, con carbohidratos como resto. `GoalModifier` de −20 a +20 en pasos de 5, con `roundToInt` (2716 da de 2173 a 3259 exactos). Factores de actividad correctos (el original mostraba 1,38 y 1,73 en las etiquetas). Rangos y valores no finitos tratados. Entrada decimal con coma y punto. Los 10 bugs principales del diagnóstico original están corregidos o eliminados.

## Arquitectura, seguridad y tests

- **Arquitectura**: ViewModels con un único `StateFlow`; `SavedStateHandle` en Calculadora y Resultados; la ruta de Resultados sobrevive a la muerte del proceso; pestañas con guardado y restauración de estado y tests reales de pila; DataStore con una instancia por fichero y manejo de corrupción; edge-to-edge correcto; color dinámico solo desde Android 12; el splash espera a leer el tema.
- **Seguridad y privacidad**: único permiso `INTERNET`; solo `MainActivity` exportada; sin logs en `main`; sin tráfico propio (solo el gestor de descargas del sistema); el prompt con datos de salud no sale del proceso; el modelo está excluido de la copia. El perfil entra hoy en la copia en la nube (decisión pendiente).
- **Tests**: prueban lo correcto y no hay tests que pasen por razones equivocadas. Huecos: los adaptadores Android (m6), y nada puede cubrir M1 ni la semántica real de `onTrimMemory` sin dispositivo.

## Riesgos que solo se pueden comprobar en dispositivo

1. Build de release con R8: abrir Resultados, cargar Kai-Q y enviar un mensaje; instalación encima de la 1.0.0 o limpia.
2. Carga del motor en GPU (Pixel o Samsung) y en un móvil sin OpenCL (debe pasar a CPU); tiempo de carga; móvil de 6 GB sin que el sistema mate el proceso.
3. M1: «Detener» e inmediatamente «Nueva conversación»; cambiar de procesador con una respuesta en curso; «Borrar modelo» con una respuesta en curso; pasar a segundo plano con una respuesta en curso. Buscar cierres nativos en logcat (`SIGSEGV`, `litertlm`).
4. M2: en Android 14+, salir a otra app 10–30 s y volver a Kai-Q.
5. Descarga: progreso, pausa al perder Wi-Fi, matar la app y volver sin que se encole otra, verificación final y toque en la notificación del sistema.
6. Importación desde Descargas: progreso, cancelar, matar a mitad y comprobar que el temporal desaparece.
7. Rotación y muerte del proceso en cada pantalla, sobre todo Kai-Q durante una respuesta y Resultados con macros ajustados.
8. TalkBack: aviso de fin de respuesta, encabezados y sliders.
9. Modo oscuro, color dinámico y el salto del splash con Ajustes en Oscuro y el sistema en claro.
10. Calculadora en es-ES y en-US, −20 % con su etiqueta y total de macros.

## Recomendaciones sobre las decisiones pendientes del operador

1. **Tag `v1.0.0`**: crearlo en GitHub (API o una release) sobre `50e5e23`.
2. **Etiqueta de la pestaña**: un sustantivo corto, «Cálculo», encaja mejor con Perfil, Ayuda y Ajustes que el verbo «Calcular». Mantener «Calculadora» como título y añadir una comprobación a 360 dp.
3. **Copia del perfil**: opción (b), excluir el perfil de la copia en la nube y mantener la transferencia entre dispositivos; mantener los ajustes. Actualizar la FAQ de privacidad y el README.
4. **Autoría de los commits**: dejarla; es veraz y las líneas de atribución ya lo declaran. Si la universidad exige otra cosa, un «Squash and merge» en GitHub produce un commit con el operador como autor sin reescribir la rama.
5. **Licencia**: Apache-2.0, alineada con Gemma y LiteRT-LM.
6. **Icono**: en el pase de identidad visual; el actual es aceptable para entregar.
7. **Caché de la CI**: `cache-provider: basic`.

## Revisión del proceso

- M1 nació en el ejemplo de referencia de la investigación: ya hacía `cancelProcess(); close()` y documentaba la cancelación como asíncrona sin unir ambas cosas. La orden lo convirtió en criterio literal y los revisores verificaron conformidad con la orden, no con la semántica nativa. Nadie tenía asignado «¿es correcta la orden?».
- M2: se asumió la semántica clásica de `onTrimMemory`; con `targetSdk 36` no se contrastó el comportamiento de Android 14+.
- Los adaptadores Android reales quedaron sin tests porque todos dieron por suficiente la estrategia «interfaz + fake».
- Puntos fuertes: mutaciones obligatorias por fase, clon limpio y build commit a commit, sondas propias de los revisores, y decisiones del operador agrupadas y no aplicadas por defecto. El asesor no se consultó durante las fases, coherente con «último recurso».

## Notas para agentes futuros

- `cancelProcess()` de LiteRT-LM es asíncrono: el fin real llega en `onError(java.util.concurrent.CancellationException)`. Nunca cierres la `Conversation` ni el `Engine` hasta recibir `onDone` u `onError`.
- No llames a `close()` sobre un `Engine` cuyo `initialize()` falló; `Conversation.close()` y `Engine.close()` lanzan `IllegalStateException` si se repiten (ya protegido).
- La variante `Flow` de la librería no detiene la generación y descarta trozos: usa siempre el `callbackFlow` propio de `LiteRtLmRepository`.
- El historial lo guarda el KV nativo: envía solo el mensaje nuevo. Para restaurar un chat tras liberar el motor existe `ConversationConfig.initialMessages`.
- `ModelSpec.DOWNLOAD_URL` está fijada a una revisión; la verificación es el tamaño exacto. SHA-256 publicado: `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`.
- R8: lo imprescindible propio es `-keep enum` de `Sex` y `ActivityLevel` y `-keep` de `com.google.ai.edge.litertlm.**`.
- El fichero del modelo solo existe en `models/` cuando está completo y verificado; `.part` e `.import` son temporales y se limpian en `refresh()`.
- Tests: JDK 21 obligatorio; `ScreenScreenshotTest` corre dentro de `testDebugUnitTest` sin grabar; las PNG de referencia no se versionan.
- Los DataStore se crean solo en `AppContainer`; en tests, `TemporaryDataStores`.
