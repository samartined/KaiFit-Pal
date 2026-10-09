# KaiFit-Pal · Plan de modernización (Kotlin + Compose + asistente local)

Plan técnico de la reescritura de KaiFit-Pal. El trabajo se hace en la rama `feat/ui-modernization`; la versión original (Java + XML) se conserva en la rama `backup/original-java` y en el tag anotado `v1.0.0`.

## Contexto

KaiFit-Pal es la app Android del TFG: calculadora de TDEE (Katch-McArdle) y % de grasa corporal (fórmula US Navy, hombre/mujer), reparto de macros ajustable y un chat "Kai-Q" que hoy llama a la API de OpenAI (gpt-3.5-turbo). Está escrita en Java + XML sobre `Theme.AppCompat` con layouts de márgenes absolutos, tres de cinco pestañas vacías (Perfil, Ayuda, Ajustes), cero tests reales y 20 bugs confirmados en la auditoría (3 crashes, un reparto de macros que suma 105 %, etiquetas de objetivo erróneas, build que falla sin `local.properties`, etc.).

### Decisiones tomadas

| Decisión | Elección |
|---|---|
| Stack | **Kotlin + Jetpack Compose + Material 3** (reescritura nativa; se mantiene `applicationId com.tfg.kaifit_pal`) |
| Asistente | **Modelo local en el dispositivo: LiteRT-LM 0.18.0 + Gemma 4 E2B** (2,58 GB, Apache-2.0). OpenAI se elimina por completo |
| Pestañas | Las cinco con contenido real: Perfil (datos persistidos), Calculadora, Resultados, Kai-Q, Ayuda (FAQ), Ajustes |
| Ramas | Trabajo: `feat/ui-modernization` (desde `main`). Respaldo: `backup/original-java` + tag anotado `v1.0.0` en `50e5e23` |
| Identidad visual | **No en este pase.** Material 3 ya aporta el sistema de diseño; si tras ver capturas reales se quiere una identidad más propia, se hará un pase de diseño aparte |

### Versiones verificadas

Versiones verificadas en Google Maven / Gradle el 2026-10-09: AGP 9.4.1 · Gradle 9.8.1 · Kotlin 2.4.21 · Compose BOM 2026.09.00 (material3 1.4.0, ui 1.12.1; exige compileSdk 37) · navigation-compose 2.10.2 · `com.google.ai.edge.litertlm:litertlm-android:0.18.0` (API Kotlin marcada "Stable" en la doc oficial).

---

## Diagnóstico resumido del código actual (para citar en el historial)

Top 10 (verificado línea a línea en `main` 50e5e23):

1. `MainActivity.java:137-148` + `:105,:113` — tras recrear la Activity (modo oscuro, fuente, muerte de proceso) `currentFragment` queda `null` → `hide(null)` → NPE al tocar cualquier pestaña; además `:49-59` crea fragments duplicados por reflexión.
2. `MainActivity.java:151-161` — `onBackPressed` deprecado y pila de back que crece en cada pestaña (`:107,:114`).
3. `Calculator.java:64-78` + `DataParser.java:13,21` + `CalculatorLogic.java:42-60` — sin validación: campos vacíos → `log10(0)` → TDEE 0 o `Integer.MIN_VALUE` → crash en `TdeeMacros.java:130` (`setMaxValue` negativo).
4. `CalculatorLogic.java:7,18` — la edad se pide y nunca se usa; no hay fórmula alternativa (Mifflin-St Jeor) cuando faltan medidas.
5. `TdeeMacros.java:158-178` — acumulación de `±0.05` en `double`: a −15 % muestra "Déficit intenso" (floorEntry cae en −0.20).
6. `MacrosManager.java:14` — Mantenimiento `{0.35, 0.25, 0.45}` suma **105 %**.
7. `TdeeMacros.java:216-234, 260-291` — editar un picker redefine el TDEE y pisa `originalTDEE`; hacks en `onPause/onSaveInstanceState`; manipula la ActionBar de la Activity.
8. `GPTApiCaller.java:43` — prompt de sistema enviado con rol `assistant`; `:35` sin timeouts; `:89` vuelca el cuerpo de error al chat. (Desaparece con el cambio a modelo local.)
9. `GPTApiCaller.java:30-33,69,83` → `KaiQ.java:90` — callback en hilo de red sobre un Fragment posiblemente desacoplado → `IllegalStateException`; `MessageAdapter.java:25` infla sin parent (burbujas del usuario no se alinean a la derecha).
10. `app/build.gradle.kts:21` — `properties.load(...)` sin comprobar existencia → el build falla en cualquier clon limpio/CI; wrapper en `8.8-rc-1`; `dataBinding` activado sin uso.

Otros: `values-night/themes.xml` vacío con parent Light (sin modo oscuro real); 21 `textSize` en `dp`; cero `contentDescription`; textos hardcodeados (strings.xml solo tiene `app_name`); `fragment_kai_q.xml:9` RecyclerView a 584dp fijos; ficheros basura `hqq`, `ot`, `taqqq`, `tatq` en la raíz; `.idea/` parcialmente versionado; README con enlace erróneo "gptapi.com". No hay ninguna clave API en el historial de git (142 commits revisados).

---

## Arquitectura objetivo (proporcionada a una app pequeña, sin Hilt ni Room)

```
app/src/main/kotlin/com/tfg/kaifit_pal/
├── KaiFitApp.kt                 Application: AppContainer (DI manual, lazy)
├── MainActivity.kt              setContent { KaiFitTheme { KaiFitNavHost() } }, enableEdgeToEdge
├── domain/                      Kotlin puro, sin Android (100 % testeable)
│   ├── BodyMeasurements.kt      data class + validate(): List<ValidationError>; Sex, ActivityLevel
│   ├── BodyFatCalculator.kt     usNavy(m): Double?  (null si faltan datos o log<=0)
│   ├── TdeeCalculator.kt        Katch-McArdle si hay % graso; si no, Mifflin-St Jeor (usa edad) → TdeeResult(method)
│   ├── Goal.kt                  Goal(DEFICIT 35/25/40, MAINTENANCE 30/25/45, SURPLUS 25/20/55), GoalIntensity
│   ├── GoalModifier.kt          value class Int (−20..+20, paso 5): adiós al bug de coma flotante
│   └── MacroPlan.kt             gramos desde kcal; withProteinGrams/withFatGrams (carbs = resto)
├── data/
│   ├── profile/ProfileRepository.kt       DataStore Preferences: medidas, % graso, TDEE, GoalModifier, macros, fecha
│   ├── settings/SettingsRepository.kt     tema (system/light/dark), dynamic color, backend (CPU/GPU), solo-WiFi
│   ├── llm/ModelManager.kt                estado del modelo: NotDownloaded/Downloading(p)/Downloaded/Error; DownloadManager del sistema → getExternalFilesDir("models"); importación manual (SAF) como alternativa
│   └── llm/LlmRepository.kt               LiteRT-LM: Engine único (app-scoped, lazy, close() en onTrimMemory), createConversation(systemInstruction = perfil), sendMessageAsync(): Flow<Message>
└── ui/
    ├── theme/  Color.kt (esquemas claro/oscuro, tokens abajo), Theme.kt (dynamicColor opcional ≥ API 31), Type.kt, Shape.kt
    ├── navigation/ KaiFitNavHost.kt + rutas tipadas (kotlinx.serialization): Profile, Calculator, Results(tdee, bf?, method), Assistant, Help, Settings
    ├── components/ NumberField (TextField numérico con sufijo de unidad y coma/punto), SegmentedSex, Stepper, MacroCard, MessageBubble, ModelStatusCard
    ├── calculator/  CalculatorScreen.kt + CalculatorViewModel.kt (StateFlow<CalculatorUiState>, validación en vivo, % graso en vivo)
    ├── results/     ResultsScreen.kt + ResultsViewModel.kt (GoalModifier, MacroPlan, "Guardar en perfil", "Preguntar a Kai-Q")
    ├── assistant/   AssistantScreen.kt + AssistantViewModel.kt (máquina de estados modelo→chat, streaming, stop, nueva conversación, prompts sugeridos)
    ├── profile/     ProfileScreen.kt + ProfileViewModel.kt
    ├── help/        HelpScreen.kt (FAQ desplegable, contenido en strings.xml)
    └── settings/    SettingsScreen.kt + SettingsViewModel.kt (tema, color dinámico, modelo: descargar/borrar/espacio, backend, acerca de)
```

Reglas transversales: todo texto en `res/values/strings.xml` (español por defecto; `values-en` opcional al final); tamaños en `sp`/tokens de tipografía; `contentDescription` en iconos; mínimo táctil 48 dp; edge-to-edge con insets; `Scaffold` + `NavigationBar` M3 con cinco destinos; Results es destino de detalle con flecha atrás (`TopAppBar`).

### Tokens Material 3 (marca verde; contrastes AA calculados)

Claro: `primary #3B7D2C`, `onPrimary #FFFFFF`, `primaryContainer #B2EDB0` (verde de marca), `onPrimaryContainer #0A2A05`, `secondary #54634D`, `secondaryContainer #D8E7CB`, `tertiary #1F6A6B` (acento del asistente), `tertiaryContainer #A8EFF0`, `error #BA1A1A`, `background/surface #F5F5EE` (beige de marca), `onSurface #1B1C18`, `surfaceVariant #E0E4D6`, `onSurfaceVariant #43483E`, `outline #73796E`, contenedores `#FFFFFF / #EFEFE8 / #E9E9E2 / #E3E4DC / #DDDED6`.

Oscuro: `primary #9FD88A`, `onPrimary #0B3900`, `primaryContainer #245F17`, `onPrimaryContainer #B2EDB0`, `secondary #BCCBB1`, `secondaryContainer #3D4B36`, `tertiary #86D3D4`, `tertiaryContainer #004F51`, `error #FFB4AB`, `background/surface #121410`, `onSurface #E3E3DC`, `surfaceVariant #43483E`, `onSurfaceVariant #C3C8BC`, `outline #8D9387`, contenedores `#0D0F0B / #1B1C18 / #1F201C / #292B26 / #343631`.

El verde oscuro original `#5A9E47` (3,27:1 sobre blanco) queda solo como token decorativo `brandGreen`.

Tipografía: escala M3 por defecto (fuente del sistema); TDEE en `displayMedium` con cifras tabulares. Formas: 4/8/12/16/28 dp (cards 12, hero 16, diálogos 28, botones stepper full). Espaciado: 4/8/12/16/24/32 dp, padding horizontal 16 dp.

### Pantallas

- **Calculadora**: `SegmentedButton` Hombre/Mujer · stepper edad 18–90 · campos altura/peso/cuello/cintura/cadera (`OutlinedTextField`, teclado decimal, sufijo cm/kg, acepta coma y punto, error por campo) · cadera solo visible si Mujer · card "% graso" en vivo con nota del método (US Navy o "introduce cuello y cintura") · `ExposedDropdownMenu` nivel de actividad (etiquetas coherentes: 1,375 / 1,725) · botón Calcular habilitado solo con datos válidos.
- **Resultados**: hero card con TDEE (kcal/día), método usado y % graso · selector de objetivo: `Slider` discreto −20..+20 (paso 5) con chip de etiqueta ("Déficit ligero"...) y kcal resultantes · tres `MacroCard` (proteína, grasa, carbohidratos) con gramos, % y `Slider` en proteína y grasa (carbohidratos se derivan; total siempre = objetivo ±2 kcal) · botones "Guardar en perfil" y "Preguntar a Kai-Q".
- **Kai-Q**: estados → `ModelStatusCard` (sin modelo: explica 2,6 GB, RAM recomendada ≥ 6 GB vía `ActivityManager.memoryInfo`, botón Descargar / Importar archivo; descargando: progreso; cargando motor: hasta ~10 s) → chat (`LazyColumn`, burbujas, texto en streaming, botón detener, "Nueva conversación", prompts sugeridos en estado vacío, aviso "no sustituye consejo médico"). `systemInstruction` = persona Kai-Q + datos del perfil si existen (sexo, edad, peso, % graso, TDEE, objetivo, macros).
- **Perfil**: resumen de última medición y resultado guardados; "Recalcular" abre la calculadora precargada; "Borrar datos".
- **Ayuda**: FAQ con `ExpandableCard`: medir cintura/cuello/cadera (textos que hoy viven en `Calculator.java:92-108`, corregidos), qué es TDEE, qué fórmulas se usan, cómo funciona el asistente sin conexión, privacidad.
- **Ajustes**: tema (sistema/claro/oscuro), color dinámico (≥ Android 12), modelo (estado, tamaño, borrar, backend CPU/GPU, solo WiFi), acerca de (versión, licencias Gemma/LiteRT-LM).

---

## Fases de implementación (commits pequeños y legibles)

### Fase 0 — Ramas e higiene del repo
- `git branch backup/original-java 50e5e23` + `git tag -a v1.0.0 50e5e23 -m "Original TFG version (Java + XML)"`; push de rama y tag.
- `git checkout -b feat/ui-modernization 50e5e23` (`main` en ese momento).
- Borrar `hqq`, `ot`, `taqqq`, `tatq`; dejar `.idea/` fuera del control de versiones (una única regla `/.idea/` en `.gitignore` en lugar de las entradas parciales, `git rm -r --cached .idea`); README: sección "Estado: en modernización" con enlace a la rama de respaldo y al tag; este plan técnico en `docs/dev/MODERNIZATION_PLAN.md`.
- Commits: `chore(repo): remove stray git log dumps`, `chore(repo): stop tracking IDE settings`, `docs(readme): add modernization status note`, `docs(dev): add technical modernization plan`.

### Fase 1 — Toolchain y esqueleto Compose (sustituye la UI Java)
- Wrapper de Gradle 9.8.1 (distribución `-bin` verificada con `distributionSha256Sum`). Version catalog `gradle/libs.versions.toml`: AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00 (material3 1.4.0, ui 1.12.1), navigation-compose 2.10.2, lifecycle 2.11.0, kotlinx-serialization 1.11.0, coroutines 1.11.0, JUnit 4, Robolectric 4.17, compose-ui-test y Roborazzi 1.76.0. datastore-preferences 1.2.1 y litertlm 0.18.0 quedan declarados en el catálogo, sin usar hasta las Fases 3 y 5.
- `build.gradle.kts` raíz y `app/build.gradle.kts` reescritos con el Kotlin integrado de AGP 9: sin el plugin `kotlin-android` (AGP 9 lo sustituye) ni `kapt`; plugins `com.android.application`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization` y Roborazzi. `compileSdk = 37` (plataforma `android-37.0`), `targetSdk = 36`, `minSdk = 26` (canales de notificación para la descarga y `java.time`; LiteRT-LM exige ≥ 24), JVM 17, `buildFeatures.compose = true`, `versionCode 2` / `versionName 2.0.0`, sin `dataBinding`/`viewBinding`, sin OkHttp ni `BuildConfig.AKy_URL`, y sin leer `local.properties` desde el build (un clon limpio compila con `ANDROID_HOME`).
- Release con `isMinifyEnabled = true` e `isShrinkResources = true`; reglas keep de kotlinx.serialization para las rutas de navegación tipadas (las de LiteRT-LM, si hacen falta, llegan en la Fase 5). Lint con `HardcodedText`, `SpUsage` y `ContentDescription` como error.
- `AndroidManifest.xml`: `KaiFitApp` y `MainActivity` en Kotlin, sin bloqueo de orientación, `windowSoftInputMode=adjustResize`, `<uses-native-library libOpenCL.so / libvndksupport.so required=false>` (GPU), splash con `Theme.SplashScreen` de `core-splashscreen`, el icono de la app y el fondo de marca, y sin permiso `INTERNET` hasta la Fase 5.
- Eliminados `app/src/main/java/**` (Java), los tests de plantilla, layouts, menús, drawables de botones y `activity_factor_values.xml` (etiquetas erróneas). Los iconos vectoriales de las pestañas y la flecha atrás se conservan como `ic_nav_*` e `ic_arrow_back` (sin `tint`, flecha con `autoMirrored` para RTL); el icono adaptativo no cambia.
- Creados `KaiFitApp`, `MainActivity`, `ui/theme/*` (esquemas claro/oscuro con los tokens de arriba, tipografía, formas y espaciado), `ui/navigation/*` (rutas tipadas, `NavigationBar` con cinco destinos y Resultados como destino de detalle con flecha atrás) y pantallas placeholder M3 con sus textos en `strings.xml`. Un botón temporal de la Calculadora abre Resultados con los valores de referencia (2716 kcal) hasta la Fase 4.
- Tests JVM con Robolectric + compose-ui-test: arranque de la app, navegación entre pestañas, Resultados y back; test de los tokens de color; capturas de humo con Roborazzi (ver Verificación).
- `local.properties.example` ya no es necesario (no hay claves); el README indica los requisitos de compilación.
- Commits: `build: upgrade Gradle wrapper to 9.8.1`, `build: migrate to AGP 9.4, Kotlin 2.4 and Compose BOM 2026.09`, `refactor(app): remove legacy Java UI and XML resources`, `feat(ui): add Material 3 theme with brand color schemes`, `feat(ui): add navigation scaffold and placeholder screens`, `test(ui): add Robolectric navigation tests and Roborazzi screenshots`, `docs: document build requirements and screenshot tooling`, `fix(ui): keep tab labels on one line and expose titles as headings`, `test(ui): cover back stack when switching tabs`, `docs: note JDK 21 requirement for unit tests`.

### Fase 2 — Dominio puro + tests
- `domain/*` en Kotlin puro (solo `kotlin.*`, sin Android), dentro del módulo `app`, según el modelo de arriba:
  - `BodyMeasurements`: sexo, edad, altura y peso obligatorios; cuello, cintura y cadera como `Double?` (`null` = no introducida, distinto de un `0` inválido). `validate(): List<ValidationError>` no lanza excepciones. Rangos inclusivos, públicos en el `companion` para la UI: edad 18–90, altura 100–250 cm, peso 30–300 kg, cuello 20–80 cm, cintura 40–200 cm y cadera 50–200 cm (solo se valida en mujer). Un valor no finito cuenta como fuera de rango, y una cintura ≤ cuello da `WAIST_NOT_ABOVE_NECK`. `ActivityLevel` va aparte, con los factores reales 1,2 / 1,375 / 1,55 / 1,725 / 1,9.
  - `BodyFatCalculator.usNavy`: ecuaciones US Navy de Hodgdon & Beckett en su variante en **pulgadas** (cada medida en cm ÷ 2,54), la de la app original y la única que reproduce los valores de referencia (la métrica da 16,11 / 24,33 / 11,13 / 27,61). Devuelve el % sin redondear, o `null` si falta un dato, si hay un valor ≤ 0 o no finito, si el argumento del logaritmo es ≤ 0 o si el resultado queda fuera de (0, 100).
  - `TdeeCalculator.calculate(medidas, actividad): TdeeResult?`: Katch-McArdle con el % graso US Navy cuando se puede estimar; si no, Mifflin-St Jeor, que usa la edad. `TdeeResult` lleva TDEE, BMR, método (`KATCH_MCARDLE` | `MIFFLIN_ST_JEOR`) y % graso. Con datos inválidos devuelve `null`: nunca 0, un negativo ni una excepción.
  - `Goal` con porcentajes enteros que suman 100, comprobado en `init`: Déficit 35/25/40, Mantenimiento 30/25/45 y Superávit 25/20/55. `GoalIntensity` (`NONE`, `LIGHT`, `MODERATE`, `MODERATE_HIGH`, `HIGH`) no depende del signo: la etiqueta visible es la combinación `Goal` × `GoalIntensity` (p. ej. Déficit × `MODERATE_HIGH` = "Déficit moderado-intenso") y sus textos van en `strings.xml` en la Fase 4.
  - `GoalModifier`: `value class` sobre un porcentaje entero de −20 a +20 en pasos de 5. El constructor rechaza cualquier otro valor; `increment()`/`decrement()` saturan en ±20; `applyTo(tdee)` multiplica en enteros y divide una sola vez.
  - `MacroPlan`: proteína y grasa redondeadas desde los porcentajes del objetivo; los carbohidratos son siempre el resto (nunca negativos), así que el total queda en objetivo ± 2 kcal. `withProteinGrams`/`withFatGrams` limitan los gramos a `0..maxProteinGrams`/`0..maxFatGrams` (lo que cabe en el objetivo junto al otro macro) y recalculan los carbohidratos.
- Redondeo con `roundToInt()` solo en los puntos de salida (TDEE, kcal del objetivo, gramos); nunca se trunca.
- `app/src/test/.../domain/*Test.kt` (JUnit 4 puro, sin Robolectric; un test por valor de referencia o caso límite, con el valor esperado literal) con valores de referencia calculados:
  - US Navy hombre 180/85/38 → **16,15 %**; mujer 165/70/33/95 → **24,56 %**; hombre 175/80/40 → 10,98 %; mujer 170/75/34/100 → 27,97 %; cintura ≤ cuello, ceros o mujer sin cadera → `null`.
  - Katch-McArdle 80 kg al 20 %, factor 1,55 → BMR 1752,4 → **2716 kcal**.
  - Mifflin-St Jeor hombre 30 a/180 cm/80 kg → **1780** BMR → ×1,55 = 2759; mujer 30/165/60 → 1320,25 → ×1,2 = 1584; selección de método según datos disponibles.
  - Macros a 2500 kcal: Mantenimiento 188/69/282 g (2501 kcal), Déficit 219/69/251, Superávit 156/56/343; cada `Goal` suma 1,00; `withProteinGrams(250)` → carbs 220 g; carbs nunca negativos.
  - `GoalModifier` desde 2716: −20→2173, −15→2309 ("Déficit moderado-intenso"), −10→2444, −5→2580, 0→2716, +5→2852, +10→2988, +15→3123, +20→3259; clamp en ±20; `GoalModifier(7)` rechazado.
  - Además: un test por regla de validación, selección del método en `calculate()` (incluido que la edad cambia el resultado y que los datos inválidos dan `null`) y barridos de invariantes de los macros de 1000 a 5000 kcal para los tres objetivos.
- Commits: `feat(domain): add body measurements model with validation`, `feat(domain): add US Navy body fat calculator`, `feat(domain): add TDEE calculator with Katch-McArdle and Mifflin-St Jeor`, `feat(domain): add goals and integer goal modifier`, `feat(domain): derive macro plan from target calories`, `docs(dev): document the domain model in the modernization plan`, `refactor(domain): map goal intensity without integer division`.

### Fase 3 — Datos: perfil y ajustes
- DataStore Preferences 1.2.1 (última estable; la 1.3.0 solo existe en alpha) en dos ficheros dentro de `files/datastore/`: `settings` y `profile`. Así, borrar el perfil nunca toca los ajustes y guardar un perfil puede vaciar su fichero entero. Todos se crean con `createPreferencesDataStore` (`data/PreferencesDataStoreFactory.kt`), que usa `ReplaceFileCorruptionHandler`: un fichero dañado se sustituye por preferencias vacías en lugar de hacer fallar la app. No hay migraciones, porque la app original no guardaba nada. Los valores se leen con un acceso tolerante al tipo: un valor guardado con otro tipo cuenta como ausente.
- `SettingsRepository` expone `Flow<AppSettings>` (`distinctUntilChanged`): tema `ThemeMode` (`SYSTEM`, `LIGHT`, `DARK`), color dinámico, backend `InferenceBackend` (`GPU`, `CPU`; no se llama `Backend` para no chocar con LiteRT-LM) y descarga solo por WiFi. Valores por defecto: `SYSTEM`, color dinámico desactivado (marca verde), `GPU` y solo WiFi activado (el modelo ocupa unos 2,6 GB). Un setter por ajuste que escribe solo su clave. Los enums se guardan por nombre; una clave ausente, un nombre desconocido o un tipo distinto dan el valor por defecto de ese campo, y un error de E/S al leer da los valores por defecto. Cualquier otra excepción llega al colector.
- `ProfileRepository` guarda la última medición y su resultado como una instantánea atómica, `SavedProfile`: medidas, nivel de actividad, `TdeeResult` (TDEE, BMR, método y % graso), `GoalModifier`, `MacroPlan` y `savedAt: Instant` (reloj inyectable, truncado a milisegundos para que lo devuelto sea igual a lo releído). Se expone como `Flow<SavedProfile?>`.
  - `save` comprueba con `require`, antes de escribir, que las medidas validan, que el objetivo de kcal es el `GoalModifier` aplicado al TDEE y que hay % graso si y solo si el método es Katch-McArdle. Después escribe todas las claves en un único `edit` que empieza con `clear()`, de modo que una circunferencia o un % graso de un guardado anterior nunca sobreviven a uno sin ellos. Las claves opcionales nulas no se escriben. Incluye `schema_version = 1`.
  - Los gramos de los macros se guardan y se releen tal cual, sin recalcular con `forGoal`, para conservar los ajustes manuales de proteína y grasa. El BMR y el % graso también se guardan, para reconstruir el resultado sin depender de las fórmulas.
  - La lectura nunca falla por datos guardados: falta una clave obligatoria, otra versión de esquema, un nombre de enum desconocido, un tipo distinto, un número no finito, un `GoalModifier` o `MacroPlan` inválidos o una instantánea incoherente dan `null`, igual que no tener perfil. `clear()` borra el perfil.
- `AppContainer` (paquete raíz) es la DI manual: crea los repositorios de forma perezosa con el contexto de la aplicación y es el único sitio donde se construyen los DataStore, de modo que hay una sola instancia por fichero en el proceso. Su `CoroutineScope` es inyectable para que los tests cierren los ficheros. `KaiFitApp.container` también es perezoso: los ViewModels de la Fase 4 obtendrán los repositorios con `(application as KaiFitApp).container`. La Fase 5 añadirá aquí `ModelManager` y `LlmRepository`.
- Tests de los repositorios en JUnit 4 puro con `kotlinx-coroutines-test` y DataStore real en una carpeta temporal (`TemporaryFolder`): valores por defecto, cada setter, persistencia al reabrir el fichero, ida y vuelta de perfiles Katch-McArdle y Mifflin-St Jeor con valores literales (2819, 2396, 1584, 1617, 2759), ausencia de datos obsoletos, gramos ajustados a mano (carbohidratos 220), datos corruptos o de otra versión, errores de E/S y ficheros dañados. `AppContainerTest` usa Robolectric para comprobar las instancias únicas y que borrar el perfil deja los ajustes en su propio fichero.
- Commits: `build(deps): add DataStore Preferences 1.2.1`, `feat(data): add settings repository backed by DataStore`, `feat(data): add profile repository with atomic snapshots`, `feat(app): provide repositories through a manual app container`, `docs(dev): document the data layer in the modernization plan`.

### Fase 4 — Pantallas Calculadora, Resultados, Perfil, Ayuda, Ajustes
- ViewModels con `StateFlow` + `collectAsStateWithLifecycle`; `NumberField` con `NumberFormat` del locale (coma/punto); `@Preview` claro/oscuro para cada pantalla.
- Commits por pantalla: `feat(calculator): ...`, `feat(results): ...`, `feat(profile): ...`, `feat(help): ...`, `feat(settings): ...`.

### Fase 5 — Kai-Q con LiteRT-LM + Gemma 4 E2B
- `ModelManager`: URL `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm` (confirmar nombre de fichero y si está "gated" al implementar; si exige token, la vía principal pasa a ser "Importar archivo" con `OpenDocument` y copia a `getExternalFilesDir("models")`); `DownloadManager` con `setDestinationInExternalFilesDir`, solo-WiFi opcional, progreso por `ContentObserver`; verificación de tamaño/`ETag` al terminar.
- `LlmRepository`: `EngineConfig(modelPath, backend = Backend.GPU() o CPU() según ajustes, cacheDir = context.cacheDir.path)`; `engine.initialize()` en `Dispatchers.IO`; `ConversationConfig(systemInstruction = Contents.of(prompt), samplerConfig = SamplerConfig(topK=40, topP=0.95, temperature=0.3), maxOutputToken=1024)`; respuestas con `sendMessageAsync(contents): Flow<Message>`; cancelación cancelando el `Job`; `close()` de conversación en "Nueva conversación" y del motor en `onTrimMemory`/`onCleared`; errores `LiteRtLmJniException` → mensaje amable y opción de reintentar/cambiar a CPU.
- `AssistantViewModel`: estados `ModelState` × `ChatState`; historial en memoria del ViewModel (sobrevive rotación); límite de turnos para no desbordar contexto.
- Commits: `feat(assistant): on-device model download and lifecycle`, `feat(assistant): Kai-Q chat on LiteRT-LM with streaming`.

### Fase 6 — Pulido, documentación, CI
- Splash M3, icono adaptativo (se conserva el actual, colores revisados), accesibilidad (TalkBack labels, `semantics`), `values-en` opcional.
- README reescrito (qué hace, capturas, cómo funciona el asistente local, requisitos de dispositivo, cómo compilar, estructura, licencias); `CHANGELOG.md` con v2.0.0.
- `.github/workflows/android.yml`: `assembleDebug`, `testDebugUnitTest`, `lintDebug` (y screenshot tests si quedan activos). Vale la pena: el bug nº 10 (build roto sin `local.properties`) habría saltado en CI.
- Commits: `chore(ci): GitHub Actions build/test/lint`, `docs: rewrite README for v2`.

---

## Verificación

**Entorno de build (Linux, sin emulador; red a dl.google.com confirmada):**
1. Instalar Android cmdline-tools en `$HOME/android-sdk`, aceptar licencias, `platforms;android-37`, `build-tools` compatibles; exportar `ANDROID_HOME`.
2. En cada fase: `./gradlew assembleDebug testDebugUnitTest lintDebug` (lint con `HardcodedText`, `SpUsage`, `ContentDescription` como error).
3. Tests de UI sin emulador: Robolectric + `compose-ui-test` (`createComposeRule`) para navegación entre pestañas, formulario (botón deshabilitado con datos inválidos, cadera solo en Mujer), slider de objetivo.
4. Capturas de pantalla sin emulador: Roborazzi 1.76.0 sobre Robolectric (modo gráfico nativo) captura las pantallas desde los tests JVM. `./gradlew recordRoborazziDebug` genera las PNG en `app/build/outputs/roborazzi/` y `./gradlew verifyRoborazziDebug` las compara con las de referencia. Sustituye al plugin Compose Preview Screenshot Testing, que solo existe en versiones alpha. En la Fase 1 hay capturas de humo del armazón; en la Fase 4 se capturan los `@Preview` claro/oscuro de cada pantalla y se revisan las PNG para validar el diseño antes de la Fase 5. Si se versionan capturas de referencia y se verifican en CI se decide en la Fase 6.
5. Se genera `app-debug.apk` para probar en un dispositivo real (el asistente local no se puede probar en el entorno de build: no hay emulador ni GPU).

**Checklist manual en dispositivo real:** descarga del modelo (WiFi, progreso, reanudar tras cerrar la app), primer arranque del motor en GPU y en CPU, respuesta en streaming en español, detener generación, nueva conversación, contexto del perfil en la respuesta, rotación y modo oscuro sin perder estado, campos vacíos/coma decimal, −20 % alcanzable con etiqueta correcta, macros suman el objetivo, guardar/leer perfil, memoria en un móvil de 6 GB.

---

## Riesgos y mitigaciones

- **Requisitos del modelo** (2,6 GB de almacenamiento, ≥ 6 GB RAM recomendados, 5–20 tok/s en CPU de gama media): comprobación previa de RAM/espacio y mensaje claro; backend GPU por defecto con fallback a CPU.
- **Descarga desde Hugging Face** (posible gating/cambio de URL): vía alternativa "Importar archivo .litertlm".
- **API de LiteRT-LM en evolución** (0.18.0): se fija la versión; nombres (`Engine`, `EngineConfig`, `Backend`, `ConversationConfig`, `sendMessageAsync`) confirmados en la doc oficial, se ajustan al compilar.
- **AGP 9.x** (Kotlin integrado, Compose 1.12 exige compileSdk 37): resolver al configurar; si la plataforma 37 no estuviera disponible en el SDK del entorno de build, usar compileSdk 36 + Compose BOM 2026.06.01.
- **Reescritura completa**: el historial queda en `backup/original-java` y el tag `v1.0.0`; la nueva app conserva `applicationId`, así que se instala como actualización.

## Fuera de alcance (fases posteriores sugeridas)

- Pase de identidad visual a partir de capturas reales.
- Modo híbrido con ML Kit GenAI Prompt API (Gemini Nano) cuando el dispositivo lo soporte.
- Historial de mediciones con gráfica, exportación, multiperfil.
