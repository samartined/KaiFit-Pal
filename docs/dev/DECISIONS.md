# Registro de decisiones

Decisiones de la modernización 2.0.0, con su motivo. Las del operador mandan sobre las técnicas; las técnicas las tomó el coordinador de cada fase y están verificadas por QA. Para el plan completo, ver [`MODERNIZATION_PLAN.md`](MODERNIZATION_PLAN.md).

## Decisiones del operador (tomadas)

| Tema | Decisión |
|---|---|
| Alcance | Lavado de cara completo de interfaz y código, corrección de bugs y stack moderno. Se admitían tecnologías completamente distintas |
| Stack | Kotlin + Jetpack Compose + Material 3, reescritura nativa. Se conserva `applicationId com.tfg.kaifit_pal` |
| Asistente | Fuera OpenAI. Modelo local en el móvil: LiteRT-LM + Gemma 4 E2B, elegidos tras comparar con ML Kit GenAI (Gemini Nano, solo en móviles recientes) |
| Pestañas | Las cinco con contenido real; Perfil guarda los datos y sirve de contexto al asistente |
| Ramas | Trabajo en `feat/ui-modernization`; respaldo en `backup/original-java` y tag `v1.0.0` sobre `50e5e23`. Nombres sin "tfg" |
| Diseño | Sin pase de identidad visual en esta vuelta: Material 3 aporta el sistema. Un pase con capturas reales queda para después |
| Ejecución | Arquitectura coordinada de agentes (ver [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md)) |
| Decisiones pendientes | No se le plantean durante el trabajo ni se aplican por defecto: se le presentan todas al final |

## Decisiones pendientes del operador (no aplicadas)

1. **Tag `v1.0.0`.** Existe solo en el clon local: el proxy del entorno en la nube rechaza el push de tags (HTTP 403). Opciones: crearlo en GitHub sobre `50e5e23` o subirlo desde otra máquina.
2. **Etiqueta de la pestaña Calculadora.** A 360 dp de ancho se trunca como «Calculad…». Propuesta: «Calcular» en la pestaña y «Calculadora» como título de pantalla. Hoy sigue «Calculadora» (`nav_calculator`).
3. **Copia de seguridad del perfil.** Con `allowBackup="true"`, el perfil y los ajustes (DataStore) entran en la copia en la nube de Google y en la transferencia entre dispositivos. Opciones: (a) dejarlo; (b) excluir el perfil de la nube y mantener la transferencia; (c) excluir ambos. Propuesta: (b). Solo el modelo (`external/models/`) está excluido hoy. La FAQ de privacidad describe el estado vigente.
4. **Autoría de los commits.** Los commits de la rama de trabajo llevan la identidad de git del entorno en la nube. Cambiarla exige reescribir la historia publicada de la rama antes de fusionar.
5. **Licencia del proyecto.** No hay fichero `LICENSE`. Propuesta: Apache-2.0, como Gemma y LiteRT-LM.
6. **Icono propio.** El icono es el robot de la plantilla de Android Studio con los colores de la marca. Propuesta: diseñar uno en un pase de identidad visual aparte.
7. **Caché de la CI.** `gradle/actions/setup-gradle@v6` sin parámetros usa `cache-provider: enhanced`, el servicio comercial de Gradle. Propuesta: fijar `cache-provider: basic`, el de código abierto.

## Toolchain y build

- **AGP 9.4.1 con su Kotlin integrado.** No se aplica `org.jetbrains.kotlin.android`; las opciones de Kotlin van en `kotlin { compilerOptions { } }`. Declarar el plugin de Compose 2.4.21 sube el compilador a Kotlin 2.4.21.
- **Gradle 9.8.1**, wrapper regenerado y validado en la CI.
- **`compileSdk 37`** (lo exige Compose 1.12), **`targetSdk 36`** (aviso `OldTargetApi` aceptado), **`minSdk 26`**.
- **JVM 17** para compilar; **JDK 21** para ejecutar los tests: Robolectric con SDK 36 carga clases de Java 21. Los tests llevan `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`.
- **Release** con R8 y `shrinkResources`. Reglas `-keep` para LiteRT-LM (el AAR no trae reglas y el JNI busca clases por nombre), para las rutas de `kotlinx.serialization` y `-keep enum` para `Sex` y `ActivityLevel` (la navegación tipada los resuelve con `Class.forName`).
- **Lint** con `abortOnError` y como errores `HardcodedText`, `SpUsage` y `ContentDescription`. `MissingKeepAnnotation` desactivado porque los enums ya se conservan con `-keep enum`.
- **Capturas con Roborazzi 1.76.0** sobre Robolectric. Compose Preview Screenshot Testing solo existía en alfa.
- Sin `values-en`: Kai-Q responde en español y las 222 cadenas de salud no tienen revisión nativa.

## Dominio (`domain/`, Kotlin puro)

- **US Navy** en pulgadas (cm / 2,54) con las constantes de la app original. Es la única variante que reproduce los valores de referencia del plan. Devuelve `null` si falta un dato, si un valor es ≤ 0 o no finito, si el argumento del logaritmo es ≤ 0 o si el resultado sale de (0, 100).
- **Katch-McArdle** cuando hay % graso; si no, **Mifflin-St Jeor**, que usa la edad. `TdeeCalculator.calculate` nunca lanza excepción y devuelve `null` con datos inválidos.
- **Rangos de validación**: edad 18-90, altura 100-250 cm, peso 30-300 kg, cuello 20-80, cintura 40-200, cadera 50-200 (solo mujer). Cintura debe superar al cuello.
- **Objetivo** como `GoalModifier`, un entero de −20 a +20 en pasos de 5. Elimina el error de coma flotante que etiquetaba −15 % como «Déficit intenso».
- **Reparto de macros** por objetivo en porcentajes enteros que suman 100: Déficit 35/25/40, Mantenimiento 30/25/45 (antes sumaba 105), Volumen 25/20/55. Proteína y grasa con `roundToInt`; carbohidratos = resto, nunca negativo. Total = objetivo ± 2 kcal.

## Datos (`data/`)

- **DataStore Preferences 1.2.1**, dos ficheros (`profile` y `settings`) creados solo en `AppContainer`. Un fichero corrupto se sustituye por preferencias vacías.
- **Inyección manual** con `AppContainer` y repositorios `by lazy`; sin Hilt ni Room.
- **`ProfileRepository.save`** es atómico y rechaza valores no finitos con el mismo criterio que la lectura, para que nada guardado se lea después como `null`.

## Interfaz (`ui/`)

- **Patrón** `XxxScreen` con estado + `XxxContent` sin estado; ViewModels con `StateFlow` y `collectAsStateWithLifecycle`; estado de formularios en `SavedStateHandle`.
- **Navigation Compose 2.10.2** con rutas tipadas. La ruta de Resultados lleva las medidas y la actividad y recalcula el TDEE, para poder guardar en el perfil.
- **Entrada decimal propia** que acepta coma y punto. `NumberFormat` de `es` leería «80.5» como 805.
- **Sexo sin elegir por defecto**, edad 30 y sedentario. La cadera solo existe con Mujer y se borra al cambiar a Hombre.
- **Color dinámico** desactivado por defecto; solo disponible en Android 12 o superior.
- **Accesibilidad**: encabezados, descripciones, estados legibles de sliders y desplegables, objetivos táctiles de 48 dp, aviso a TalkBack cuando Kai-Q termina de responder.

## Asistente local (`data/llm/`, `ui/assistant/`)

- **Fichero** `gemma-4-E2B-it.litertlm` genérico (2.588.147.712 bytes), con la URL fijada a la revisión `b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1` de Hugging Face. Se descartó la variante `-gpu`: solo GPU y solo texto, sin vuelta a CPU.
- **Verificación** por tamaño exacto y cabecera `LITERTLM`. El ETag del CDN no es el SHA-256, así que no se usa.
- **Descarga** con `DownloadManager` a un `.part` y renombrado atómico; solo Wi-Fi por defecto. **Importación** por el selector de archivos, con cabecera válida. Los `.part` e `.import` huérfanos se borran al recalcular el estado.
- **Requisitos** comprobados antes de descargar: ABI de 64 bits y espacio (tamaño + 500 MB) bloquean; menos de 5,5 GB de RAM solo avisa.
- **Motor** app-scoped detrás de la interfaz `LlmRepository`. GPU por defecto con paso automático a CPU. Sin visión ni audio, contexto de 4096 tokens, respuesta de hasta 1024, sampler topK 40 / topP 0,95 / temperatura 0,3, sin "thinking" ni `ExperimentalFlags`.
- **Streaming** con un `callbackFlow` propio que llama a `cancelProcess()` al cancelar: con el Flow de la librería, cancelar el colector no detiene la generación nativa.
- **Ciclo de vida**: el motor se carga al mostrar Kai-Q y se libera en `onTrimMemory` y al destruir el ViewModel. Nueva conversación al llegar a 2560 tokens o 20 turnos.
- **Contexto**: el prompt de sistema (`KaiQPrompt`) va en español y añade el perfil guardado en cada conversación nueva.
- **Copia de seguridad**: se excluye `external/models/`; los 2,6 GB superarían la cuota de Android y dejaría de copiarse todo.

## CI y documentación

- **GitHub Actions** en `ubuntu-24.04` con Temurin 21, SDK con `platforms;android-37.0` y `build-tools;36.0.0`, validación del wrapper, un único `assembleDebug testDebugUnitTest lintDebug` y artefactos de informes y APK. Acciones fijadas por versión mayor.
- **README** en español con 8 capturas en `docs/screenshots/`. Las capturas completas se regeneran con `recordRoborazziDebug`; no hay capturas de referencia versionadas para `verifyRoborazziDebug`.

## Desviaciones del plan aceptadas

- Capturas con Roborazzi en lugar de Compose Preview Screenshot Testing.
- Ruta de Resultados con las medidas completas en lugar de `Results(tdee, bf?, method)`.
- Asunto del commit `53348d7` de 73 caracteres (límite 72): se copió literal de la orden y no se reescribe la historia.

Respecto a la versión original, el `minSdk` sube de 24 a 26 (canales de notificación y `java.time`), como preveía el plan.
