<!--
Notas de toolchain verificadas en la Fase 1 (2026-10-09) en un proyecto desechable con
Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21 y JDK 21. Se omite el apartado con el contenido completo
de los ficheros de build: la versión vigente está en el propio repositorio. Las rutas `COORD/...`
eran de un directorio temporal de la sesión; el init script del espejo está ahora en
`docs/dev/env/central-mirror.init.gradle.kts`.
-->

# Fase 1 - Hechos verificados de toolchain (Investigador, 2026-10-09)

Todo lo de este documento se ha VALIDADO en el proyecto desechable
`COORD/sandbox/toolchain-check` (fuera del repo): `assembleDebug`, `assembleRelease` (R8 + shrinkResources),
`testDebugUnitTest` (Robolectric + compose-ui-test, 2/2 OK), `recordRoborazziDebug` / `verifyRoborazziDebug`
(PNG generado) y `lintDebug` (0 errores) -> BUILD SUCCESSFUL con Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, JDK 21.
Captura de prueba: `COORD/artifacts/fase-1/research/roborazzi-greeting.png`. Logs: `COORD/artifacts/fase-1/research/*.log`.

## 1. Versiones exactas (estables, verificadas en maven-metadata.xml)

| Componente | Version | Fuente |
|---|---|---|
| Gradle (wrapper) | 9.8.1 (bin; sha256 dist `dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f`) | services.gradle.org |
| AGP `com.android.application` | 9.4.1 (min Gradle 9.6.0, JDK 17, build-tools min/default 36.0.0, API max 37) | Google Maven + release notes 9.4 |
| Kotlin (compose compiler plugin, serialization plugin) | 2.4.21 (2.5.0-Beta1 existe: NO usar) | Maven Central |
| Compose BOM | 2026.09.00 (ui 1.12.1, material3 1.4.0) | Google Maven |
| activity-compose | 1.13.0 | Google Maven |
| core-ktx | 1.19.1 | Google Maven |
| core-splashscreen | 1.2.0 | Google Maven |
| lifecycle-runtime-compose / lifecycle-viewmodel-compose | 2.11.0 | Google Maven |
| navigation-compose | 2.10.2 | Google Maven |
| datastore-preferences | 1.2.1 | Google Maven |
| kotlinx-serialization-json | 1.11.0 (1.12.0-RC: no) | Maven Central |
| kotlinx-coroutines (android / test) | 1.11.0 | Maven Central |
| junit | 4.13.2 | Maven Central |
| robolectric | 4.17 | Maven Central |
| androidx.test:core | 1.7.0 | Google Maven |
| androidx.test.ext:junit | 1.3.0 | Google Maven |
| androidx.test:runner (si se necesita) | 1.7.0 | Google Maven |
| compose ui-test-junit4 / ui-test-manifest | via BOM (1.12.1) | Google Maven |
| Roborazzi (plugin `io.github.takahirom.roborazzi` + libs) | 1.76.0 | Maven Central |
| build-tools | por defecto 36.0.0 (instalada); 37.0.0 tambien instalada. No hace falta `buildToolsVersion` | SDK |

## 2. AGP 9 + Kotlin integrado (built-in Kotlin) - como configurarlo

- AGP 9 trae Kotlin integrado y ACTIVADO por defecto: **NO aplicar `org.jetbrains.kotlin.android`** (ni en raiz ni en app ni en el catalogo).
  La doc oficial (developer.android.com/build/migrate-to-built-in-kotlin) indica eliminarlo; solo se puede volver al plugin con
  `android.builtInKotlin=false` + `android.newDsl=false` (desaconsejado, desaparece en AGP 10). `kotlin-kapt` es incompatible (usar KSP si hiciera falta).
- `android { kotlinOptions {} }` ya no existe: se usa el bloque de nivel superior `kotlin { compilerOptions { ... } }`
  (VALIDADO). `jvmTarget` es opcional: por defecto toma `android.compileOptions.targetCompatibility` (17). En el sandbox se fijo
  explicitamente `JvmTarget.JVM_17` y compila.
- Version de Kotlin: AGP 9.4.1 depende de KGP 2.2.10, pero al declarar en el build raiz `org.jetbrains.kotlin.plugin.compose`
  2.4.21 (apply false) Gradle sube el classpath a KGP 2.4.21 (`kotlin-gradle-plugin:2.2.10 -> 2.4.21` en `buildEnvironment`, VERIFICADO).
  No hace falta `buildscript { classpath }`.
- Compose: plugin `org.jetbrains.kotlin.plugin.compose` 2.4.21 + `buildFeatures { compose = true }`. Sin `composeOptions.kotlinCompilerExtensionVersion`.
- Serialization: plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21 funciona con el Kotlin integrado (validado con `@Serializable data object HomeRoute`).
- compileSdk (DSL nuevo de AGP 9, VALIDADO ambos):
  - `compileSdk = 37` -> resuelve a `platforms;android-37.0` (instalada).
  - Variante minor: `compileSdk { version = release(37) { minorApiLevel = 2 } }` -> resuelve a `android-37.2`
    (comprobado: lint model `compileTarget="android-37.2"`). Ambas instaladas. AGP 9.4.1 `MAX_RECOMMENDED_COMPILE_SDK_VERSION = 37.2`.
  - Recomendacion: `compileSdk = 37` es suficiente para Compose 1.12; usar la minor solo si se necesita API de 37.2.
- Gradle 9.8.1 + AGP 9.4.1: compatible. Unico aviso: deprecacion `Configuration.setVisible(boolean)` emitida por el propio AGP
  (`BasePlugin.createAndroidJdkImageConfiguration`), no por nuestros scripts; inofensivo (se elimina en Gradle 11).
- Configuration cache (`org.gradle.configuration-cache=true`) funciona con AGP 9.4.1 + Roborazzi 1.76.0.

## 3. Herramienta de capturas headless: recomendacion = **Roborazzi 1.76.0**

| | Roborazzi 1.76.0 | Compose Preview Screenshot Testing |
|---|---|---|
| Estado | Estable (Maven Central) | Solo `0.0.1-alpha01..alpha16` en Google Maven (`com.android.compose.screenshot`), ninguna estable |
| Motor | Robolectric (ya en el plan) en modo `GraphicsMode.NATIVE`, en `src/test` (JUnit 4) | Layoutlib, source set propio `src/screenshotTest`, requiere flag experimental de AGP y motor JUnit 5 (`screenshot-validation-junit-engine`) |
| AGP 9.4 | VALIDADO aqui (record + verify OK, configuration cache OK) | No validado; alpha atado a versiones concretas de AGP |
| Reutiliza tests | Si: misma `createComposeRule` de los tests de UI | No, solo `@Preview` |

Elegida Roborazzi: estable, sin emulador, misma infraestructura que los tests Robolectric, y puede capturar los `@Preview`
claro/oscuro llamandolos desde un test (o con `roborazzi-compose-preview-scanner-support` si se quiere automatico).
Esto sustituye al plugin de Compose Preview Screenshot Testing citado en el plan (el plan ya preveia "si no funciona headless... se documenta").

Tareas Gradle de Roborazzi: `recordRoborazziDebug` (genera/actualiza PNG), `verifyRoborazziDebug` (compara con las de referencia y falla si difieren),
`compareRoborazziDebug` (genera diffs sin fallar). `testDebugUnitTest` normal ejecuta `captureRoboImage` sin escribir PNG (no falla).
La ruta pasada a `captureRoboImage("src/test/screenshots/x.png")` es relativa al modulo `app/`.


## 5. Comandos exactos y tiempos aproximados

```bash
export ANDROID_HOME=/root/android-sdk            # o sdk.dir en local.properties
INIT="-I docs/dev/env/central-mirror.init.gradle.kts"   # solo si Maven Central responde 429
# wrapper (desde un directorio con settings.gradle.kts minimo; ver trampa 5)
/opt/gradle-8.14.3/bin/gradle wrapper --gradle-version 9.8.1 --distribution-type bin      # ~35 s
./gradlew $INIT assembleDebug --console=plain          # 1a vez ~2,5 min (descarga dist Gradle + dependencias); luego ~30 s
./gradlew $INIT testDebugUnitTest --console=plain      # ~15-35 s (1a vez descarga android-all-instrumented SDK 36)
./gradlew $INIT recordRoborazziDebug --console=plain   # ~16 s
./gradlew $INIT verifyRoborazziDebug --console=plain
./gradlew $INIT lintDebug --console=plain              # ~30 s
./gradlew $INIT clean assembleDebug assembleRelease testDebugUnitTest verifyRoborazziDebug lintDebug   # ~1,5 min con cache
```
Todos con `timeout 600000` en Bash. Los comandos sin `$INIT` funcionan igual en CI (GitHub Actions) donde Maven Central no limita.

## 6. Trampas encontradas

1. **HTTP 429 de Maven Central** (`repo.maven.apache.org` y `repo1.maven.org`) desde este contenedor, intermitente (~50 %).
   Gradle falla con "Plugin ... was not found" / "Received status code 429". Solucion de ENTORNO: init script de arriba,
   que antepone el espejo publico de Google `https://maven-central.storage-download.googleapis.com/maven2/` (HTTP 200 estable).
   No meter el espejo en el repo; pasar `-I` en cada invocacion (o, si el coordinador lo decide, copiarlo a `~/.gradle/init.d/`).
2. **Robolectric descarga `android-all-instrumented` en tiempo de test** (fuera de la resolucion de Gradle) desde repo1.maven.org -> mismo 429
   ("Failed to fetch maven artifact org.robolectric:android-all-instrumented:16-robolectric-13921718-i7"). El init script fija
   `robolectric.dependency.repo.url` al espejo para las tareas Test.
3. **Robolectric 4.17 + SDK 36 + JDK 21**: "Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?"
   (`IllegalAccessException ... jdk.internal.access`). Solucion (SI va al repo): en `testOptions.unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }`.
   Basta `--add-exports` (validado sin `--add-opens`). Tests con `@Config(sdk = [36])` (Robolectric 4.17 no ejecuta SDK 37; compileSdk 37 con targetSdk 36 funciona).
4. `androidx.compose.ui.test.junit4.createComposeRule` esta DEPRECADO en ui-test 1.12: usar `androidx.compose.ui.test.junit4.v2.createComposeRule`
   (usa `StandardTestDispatcher`; puede requerir `waitForIdle()`/`mainClock` explicitos). Validado con v2.
5. Generar el wrapper con `/opt/gradle-8.14.3/bin/gradle wrapper` en el repo configura el build existente (Java/AGP viejo) y puede fallar:
   alternativa segura = copiar `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` (sha256 `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`)
   y `gradle-wrapper.properties` desde `COORD/sandbox/toolchain-check/`, o ejecutar el comando cuando el settings/build ya sean los nuevos.
   `distributionSha256Sum=dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f` puede anadirse opcionalmente (no validado).
6. Lint (0 errores) avisa: `OldTargetApi` (targetSdk 36 < 37, decision del plan; aceptable o subir a 37), `NotShrinkingResources`
   (resuelto con `isShrinkResources = true`), `MissingApplicationIcon` (el repo real ya tiene icono).
7. Aviso de packaging inofensivo: "Unable to strip ... libandroidx.graphics.path.so, libdatastore_shared_counter.so" (no hay NDK; no afecta).
8. Deprecacion Gradle `Configuration.setVisible` viene de AGP 9.4.1, no de nuestros scripts.
9. `android.useAndroidX=true` se mantuvo en gradle.properties (no se probo sin el).

## 7. SDK de Android instalado en el contenedor (Fase 1)


### Rutas
- SDK: `/root/android-sdk` (783 MB)
- cmdline-tools: `/root/android-sdk/cmdline-tools/latest` (commandlinetools-linux-9862592_latest.zip, Pkg.Revision 10.0; sha256 177e20e12637e8e3ba2cbe5fe20b780c38c92bdd85a1633300bdc041e20b2036)
- `/home/user/KaiFit-Pal/local.properties` -> `sdk.dir=/root/android-sdk` (ignorado: `.gitignore:10:local.properties`)
- Entorno: `ANDROID_HOME` y `ANDROID_SDK_ROOT` = `/root/android-sdk`; `PATH` con `cmdline-tools/latest/bin` y `platform-tools`. Las variables no persisten entre llamadas a Bash en el contenedor: usa `sdk.dir` en `local.properties`.
- Licencias aceptadas (`yes | sdkmanager --licenses`).

### sdkmanager --list_installed
```
  Path                   | Version | Description                | Location
  build-tools;36.0.0     | 36.0.0  | Android SDK Build-Tools 36 | build-tools/36.0.0
  build-tools;37.0.0     | 37.0.0  | Android SDK Build-Tools 37 | build-tools/37.0.0
  platform-tools         | 37.0.1  | Android SDK Platform-Tools | platform-tools
  platforms;android-37.0 | 2       | Android SDK Platform 37.0  | platforms/android-37.0
  platforms;android-37.2 | 1       | Android SDK Platform 37.2  | platforms/android-37.2
```
adb 1.0.41 (37.0.1-15733141); aapt2 2.20-15087165 (build-tools 37.0.0).

### Decisiones y verificacion
- Plataformas estables disponibles para API 37: `android-37.0` (rev 2), `android-37.1`, `android-37.2` (rev 1). Hay `android-37.2-beta1..3`: NO usadas.
- `compileSdk = 37` (sin minorApiLevel) resuelve a `platforms;android-37.0` -> instalada. Tambien instalada `android-37.2` por si se opta por la minor mas reciente.
- AGP 9.4.1 (inspeccionado `com.android.builder.core.ToolsRevisionUtils` del jar `builder-9.4.1.jar` de Google Maven):
  - `MIN_BUILD_TOOLS_REV = DEFAULT_BUILD_TOOLS_REVISION = 36.0.0`
  - `MAX_RECOMMENDED_COMPILE_SDK_VERSION = 37.2` -> compileSdk 37.x sin aviso.
- build-tools estable mas reciente: `37.0.0` (>= minimo 36.0.0; solo existen rc posteriores de 37). Instalada tambien `36.0.0` porque es la que AGP usa por defecto si no se fija `buildToolsVersion`; asi el build no necesita descargarla. Si se quiere usar 37.0.0, fijar `buildToolsVersion = "37.0.0"`.

### Red / certificados
- Sin incidencias. curl (descarga del zip y del jar AGP) y sdkmanager (JVM, via JAVA_TOOL_OPTIONS con proxy 127.0.0.1 y truststore /etc/ssl/certs/java/cacerts) acceden a dl.google.com sin cambios adicionales.
