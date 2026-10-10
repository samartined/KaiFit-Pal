# AGENTS.md · KaiFit-Pal

Instrucciones para agentes de código que trabajen en este repositorio. Léelo entero antes de tocar nada.

## Qué es y en qué estado está

KaiFit-Pal es una app Android, nacida como Trabajo de Fin de Grado, que calcula el gasto energético diario (TDEE) y el % de grasa corporal, propone un reparto de macronutrientes según el objetivo e incluye Kai-Q, un asistente de nutrición que ejecuta Gemma 4 E2B en el propio móvil.

- **Versión 2.0.0**, reescrita en Kotlin + Jetpack Compose + Material 3. Vive en la rama `feat/ui-modernization`, publicada y con la CI en verde.
- **`main` sigue siendo la versión original** en Java + XML, igual que la rama de respaldo `backup/original-java` (commit `50e5e23`). Nada se ha fusionado en `main` ni hay pull request abierta: eso lo decide el operador.
- Nada se ha probado aún en un móvil real ni en emulador.

## Lee primero

1. [`HANDOFF.md`](HANDOFF.md): estado al cierre de la última sesión, pendientes, decisiones abiertas y siguientes pasos.
2. [`docs/dev/DECISIONS.md`](docs/dev/DECISIONS.md): decisiones tomadas y su motivo, y las decisiones pendientes del operador.
3. [`docs/dev/MODERNIZATION_PLAN.md`](docs/dev/MODERNIZATION_PLAN.md): plan técnico, diagnóstico de la versión original y lista de commits por fase.
4. [`docs/dev/reference/litertlm-0.18.0.md`](docs/dev/reference/litertlm-0.18.0.md) antes de tocar Kai-Q; [`docs/dev/reference/toolchain.md`](docs/dev/reference/toolchain.md) antes de tocar el build.
5. [`docs/dev/AGENT_WORKFLOW.md`](docs/dev/AGENT_WORKFLOW.md) si el trabajo es grande.

## Cómo trabaja el operador

- **Idioma**: español en la conversación, en la app (`strings.xml`) y en la documentación de producto. Código, identificadores y mensajes de commit en inglés.
- **Decisiones del operador**: producto, textos visibles, privacidad, licencias o alcance. No las tomes tú ni las apliques por defecto. Deja el estado actual o lo que diga el plan aprobado, anótalas en `HANDOFF.md` con contexto, opciones y tu propuesta, y preséntalas todas juntas al final. No las vayas narrando mientras trabajas.
- **Trabajo grande**: el operador quiere una arquitectura coordinada de agentes. Un coordinador Opus media; el implementador es Opus 5.5 con esfuerzo `xhigh`; QA y revisores son Opus. Fable actúa solo como asesor de último recurso, sin implementar, y siempre avisando antes al operador. Ver `docs/dev/AGENT_WORKFLOW.md` y el guion reutilizable en `docs/dev/agent-workflow/`.
- **Ramas**: convención `tipo/descripcion` (por ejemplo `feat/…`, `fix/…`, `docs/…`), sin la palabra "tfg". Nunca hagas push a `main` ni a `backup/original-java`, no fusiones y no abras pull requests sin que el operador lo pida.

## Entorno y build

Requisitos: **JDK 21** (los tests con Robolectric y SDK 36 lo necesitan; compilar solo exige JDK 17), Android SDK con `platforms;android-37.0` y `build-tools;36.0.0`, y `sdk.dir` en `local.properties` o `ANDROID_HOME`. No hace falta ninguna clave de API.

```bash
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # tests JVM y Robolectric (409 al cierre de la 2.0.0)
./gradlew lintDebug              # 0 errores; solo se espera el aviso OldTargetApi
./gradlew recordRoborazziDebug   # capturas en app/build/outputs/roborazzi/
./gradlew assembleRelease        # R8 + shrinkResources, sin firmar
```

En un contenedor de Claude Code en la nube:

- **Instalar el SDK** (unos 800 MB; ver detalles en `docs/dev/reference/toolchain.md`):
  ```bash
  mkdir -p /root/android-sdk/cmdline-tools && cd /root/android-sdk/cmdline-tools
  curl -sSLO https://dl.google.com/android/repository/commandlinetools-linux-9862592_latest.zip
  unzip -q commandlinetools-linux-9862592_latest.zip && mv cmdline-tools latest
  yes | latest/bin/sdkmanager --licenses >/dev/null
  latest/bin/sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
  echo "sdk.dir=/root/android-sdk" > /home/user/KaiFit-Pal/local.properties
  ```
- **Las variables de entorno no persisten** entre llamadas a Bash: usa `local.properties`.
- **Maven Central responde a veces HTTP 429** desde el contenedor. Si el build falla con "Received status code 429", añade `-I docs/dev/env/central-mirror.init.gradle.kts`, que antepone el espejo público de Google. No lo apliques en la CI ni lo conviertas en configuración del proyecto.
- Los builds tardan minutos: usa un timeout de Bash de 600000 ms o ejecútalos en segundo plano.
- Kotlin crea `.kotlin/` mientras hay un daemon de Gradle; está ignorado, pero no uses `git add -A` a ciegas.
- **El proxy rechaza el push de tags** (HTTP 403). No reintentes: anótalo para el operador.

## Mapa del código

```
app/src/main/kotlin/com/tfg/kaifit_pal/
├── KaiFitApp.kt, AppContainer.kt   inyección manual: repositorios y motor, una instancia cada uno
├── MainActivity.kt                  splash, edge-to-edge, tema
├── domain/        Kotlin puro: medidas y validación, US Navy, TDEE, objetivo, macros
├── data/profile/  perfil guardado (DataStore "profile")
├── data/settings/ tema, color dinámico, procesador, solo Wi-Fi (DataStore "settings")
├── data/llm/      modelo de Kai-Q: descarga, importación, comprobaciones, prompt, LiteRT-LM
└── ui/            theme, navigation (rutas tipadas), components y una carpeta por pantalla
```

Los tests están en `app/src/test/kotlin/...` con la misma estructura; los fakes y utilidades compartidas, en `testing/`.

## Convenciones de código

- **Pantallas**: `XxxScreen` con estado y ViewModel, `XxxContent` sin estado y con `@Preview` claro y oscuro. ViewModels con `StateFlow`, factorías con `viewModelFactory` y `APPLICATION_KEY`, y `collectAsStateWithLifecycle` en la UI.
- **Textos** siempre en `res/values/strings.xml`, en español. Tamaños en `sp`, objetivos táctiles de al menos 48 dp, `contentDescription` o `semantics` en lo que no sea texto. Lint trata `HardcodedText`, `SpUsage` y `ContentDescription` como errores.
- **Colores** solo desde `MaterialTheme.colorScheme`. Los tokens claro y oscuro están en `ui/theme/Color.kt` y `ColorTokensTest` los protege.
- **Excepciones**: captura tipos concretos (`IOException`, `IllegalArgumentException`, `LiteRtLmJniException`…). No hay `catch (Exception)`, `Throwable` ni `runCatching` en `src/main`; mantenlo así.
- **Dominio** sin imports de Android. Para comprobarlo: `grep -rn "import android\." app/src/main/kotlin/com/tfg/kaifit_pal/domain`. El patrón `import android` también encaja con `androidx`.
- **Navegación**: Navigation Compose con rutas `@Serializable`. Si añades enums a una ruta, añade su `-keep enum` en `proguard-rules.pro`.

## Invariantes que no se deben romper

- **US Navy** en pulgadas con las constantes originales. Referencias: hombre de 180 cm, cuello 38 y cintura 85, da 16,15 %; mujer de 165 cm, cuello 33, cintura 70 y cadera 95, da 24,56 %.
- **TDEE**: Katch-McArdle con % graso; si no lo hay, Mifflin-St Jeor. Referencias: 80 kg al 20 % con factor 1,55 da 2716 kcal; hombre de 30 años, 180 cm y 80 kg da un BMR de 1780.
- **Objetivo** entero de −20 a +20 en pasos de 5. Nunca acumular `double`.
- **Macros**: los porcentajes de cada objetivo suman 100. Proteína y grasa se redondean y los carbohidratos son el resto, de modo que el total queda a ±2 kcal del objetivo.
- **Lo guardado se lee**: `ProfileRepository.save` y la lectura comparten el criterio de validez.
- Los valores de referencia están en los tests de `domain/` y en `testing/ReferenceData.kt`. Si un cambio los rompe, el cambio está mal salvo que el operador diga lo contrario.

## Kai-Q y LiteRT-LM

- **API real 0.18.0** en `docs/dev/reference/litertlm-0.18.0.md`. Nada es `suspend`; `Backend.GPU()` es una clase; no existe `Conversation.stop()`.
- **`Engine.initialize()`** es bloqueante (hasta unos 10 s): siempre fuera del hilo principal. Cierra primero la conversación y después el motor, y protégete de los cierres dobles.
- **Cancelación**: cancelar el colector del Flow de la librería **no** detiene la generación nativa. Por eso `LiteRtLmRepository` usa un `callbackFlow` propio con `cancelProcess()`. No lo sustituyas por el Flow de la librería.
- **Solo hay librerías nativas para `arm64-v8a` y `x86_64`**. En 32 bits la app muestra «no compatible».
- **Release**: el AAR no trae reglas de R8; las de `proguard-rules.pro` son imprescindibles.
- **Modelo**: `gemma-4-E2B-it.litertlm` (2.588.147.712 bytes) en la revisión fijada de Hugging Face. Se verifica por tamaño exacto y cabecera `LITERTLM`. No se descarga nunca en tests ni en la CI.
- **Tests**: la clase real no se puede ejecutar en la JVM (`UnsatisfiedLinkError`). Todo se prueba con `FakeLlmRepository`, `FakeModelManager` y compañía; los riesgos reales están en la checklist de dispositivo de `HANDOFF.md`.

## Tests

- **JUnit 4 + Robolectric** con `sdk=36` (`app/src/test/resources/robolectric.properties`) y `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`, ya configurado en `app/build.gradle.kts`.
- **Compose**: usa `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`; el `createComposeRule` antiguo está deprecado.
- **Configuración**: los tests de UI fijan `@Config(qualifiers = "es-rES…")`. Las capturas fijan la zona horaria para que la fecha del perfil no cambie.
- **Capturas**: Roborazzi escribe en `app/build/outputs/roborazzi/` y no hay capturas de referencia versionadas. Las 8 del README están en `docs/screenshots/`; actualízalas a mano si cambia la interfaz.
- **Mutaciones**: la práctica de esta base de código es comprobar que un test nuevo falla si se rompe a propósito el código que protege.

## Git

- **Commits** pequeños y lógicos, Conventional Commits en inglés y asunto de 72 caracteres como máximo. Termina el mensaje con las líneas de atribución que indique tu sesión.
- **Historia**: no reescribas historia publicada. Nada de `rebase`, `amend` ni force-push sobre commits empujados; las correcciones van en commits nuevos.
- **Ficheros prohibidos**: no versiones `local.properties`, `.kotlin/`, `build/` ni modelos `.litertlm`.
- **Herramientas de IA en la documentación**: el código, el README, el CHANGELOG y el plan técnico no mencionan herramientas de IA ni agentes. Ese contexto vive solo en `AGENTS.md`, `CLAUDE.md`, `HANDOFF.md`, `docs/dev/AGENT_WORKFLOW.md` y `docs/dev/agent-workflow/`.
- **Al terminar una sesión**, actualiza `HANDOFF.md`: estado, SHA, pendientes y decisiones abiertas.
