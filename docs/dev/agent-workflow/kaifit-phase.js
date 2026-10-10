// KaiFit-Pal - phase workflow used during the 2026-10 modernization (see docs/dev/AGENT_WORKFLOW.md).
// Run it with the Workflow tool: {scriptPath: "docs/dev/agent-workflow/kaifit-phase.js",
//   args: {phase: N, coord: "<coordination dir>", repo?, branch?, trailer?, advice?, operatorAnswers?, resumeNote?}}.
// PHASES below holds the phase definitions of that modernization (0-6). For new work, replace PHASES
// with your own phase notes and acceptance criteria; the role prompts and the decision loop are generic.
export const meta = {
  name: 'kaifit-phase',
  description: 'Ejecuta una fase del plan de modernizacion de KaiFit-Pal con coordinador, implementador, QA, revisores y publicador',
  whenToUse: 'Una ejecucion por fase (0-6) del plan de KaiFit-Pal; args = {phase, coord, advice?, operatorAnswers?}',
  phases: [
    { title: 'Preparar', detail: 'entorno e investigacion (fases 1 y 5)' },
    { title: 'Ordenar', detail: 'orden de trabajo del coordinador' },
    { title: 'Implementar', detail: 'implementador con esfuerzo xhigh' },
    { title: 'Verificar', detail: 'QA/Build y revisores en paralelo' },
    { title: 'Decidir', detail: 'aceptar, corregir o escalar' },
    { title: 'Publicar', detail: 'push tras ACCEPT' },
  ],
}

const N = args.phase
const COORD = args.coord
const REPO = args.repo || '/home/user/KaiFit-Pal'
const BRANCH = args.branch || 'feat/ui-modernization'
const ADVICE = args.advice || []
const ANSWERS = args.operatorAnswers || []
const OPUS = 'opus'
// Attribution lines required at the end of every commit message; pass the ones your session uses.
const TRAILER = args.trailer || 'Co-Authored-By: <attribution line of your session>'

const PHASES = {
  0: {
    name: 'Ramas de respaldo e higiene del repo', env: false, research: null, docs: false, build: false,
    lenses: ['conformidad'],
    notes: [
      '- Crear localmente la rama backup/original-java apuntando a 50e5e23eca50ded1b7aa95f264ccb2ce9b521566 (estado original de main) y el tag ANOTADO v1.0.0 en ese mismo commit, con mensaje "Original TFG version (Java + XML)".',
      '- Crear feat/ui-modernization desde 50e5e23 y hacer checkout. Todo el trabajo de esta y siguientes fases va ahi.',
      '- Borrar los ficheros basura de la raiz hqq, ot, taqqq, tatq (volcados de git log).',
      '- Dejar .idea/ fuera de git: anadir /.idea/ a .gitignore y git rm -r --cached .idea (sin borrar la carpeta local).',
      '- README.md: anadir al principio una seccion breve "Estado: en modernizacion" que explique que la app se esta reescribiendo (Kotlin + Jetpack Compose + Material 3, asistente con modelo local) y que la version original en Java esta en la rama backup/original-java y en el tag v1.0.0. No reescribir el resto del README (eso es la fase 6).',
      '- Crear docs/dev/MODERNIZATION_PLAN.md: version tecnica del PLAN (en espanol) SIN la seccion "Arquitectura de ejecucion multiagente", SIN la fila "Ejecucion" de la tabla de decisiones, SIN la nota sobre la rama de sesion asignada, y sin ninguna mencion a modelos de IA, agentes, sesiones ni herramientas de asistencia.',
      '- En esta fase NO se compila (el build antiguo necesita un SDK que aun no existe). La verificacion es solo de git y contenido.',
    ].join('\n'),
    qa: 'Verificaciones de git y contenido (no se ejecuta Gradle en esta fase): backup/original-java y el tag v1.0.0 (anotado, git cat-file -t v1.0.0 = tag) apuntan a 50e5e23eca50ded1b7aa95f264ccb2ce9b521566; feat/ui-modernization deriva de 50e5e23 y esta en checkout; hqq, ot, taqqq y tatq ya no estan versionados; .idea/ no esta versionado y figura en .gitignore; README tiene la seccion de estado; docs/dev/MODERNIZATION_PLAN.md existe, es fiel al plan tecnico y no contiene la arquitectura de agentes ni menciones a modelos de IA o asistentes.',
  },
  1: {
    name: 'Toolchain y esqueleto Compose', env: true, research: 'toolchain', docs: false, build: true,
    lenses: ['correccion', 'ui', 'conformidad'],
    notes: [
      '- El entorno (Android SDK) lo deja preparado el agente de entorno: ver COORD/facts/entorno-sdk.md y COORD/env.sh. Los hechos de toolchain verificados (con un proyecto de prueba que compila) estan en COORD/facts/fase-1.md: usalos como referencia exacta para los ficheros de build.',
      '- Alcance del plan, Fase 1: Gradle wrapper 9.8.1, version catalog, AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37 (la variante que exija AGP), targetSdk 36, minSdk 26, JVM 17, sin dataBinding/viewBinding, sin OkHttp ni BuildConfig de claves; release con minify y reglas necesarias.',
      '- Borrar todo el codigo Java y los layouts/menus/drawables XML de la UI antigua; conservar el icono adaptativo y los mipmaps. versionCode 2, versionName "2.0.0".',
      '- Crear KaiFitApp, MainActivity (edge-to-edge), ui/theme (tokens claro/oscuro EXACTOS del plan, tipografia, formas, color dinamico opcional), ui/navigation con rutas tipadas: 5 destinos en NavigationBar (Perfil, Calculadora, Kai-Q, Ayuda, Ajustes; inicio en Calculadora) + destino Resultados de detalle con flecha atras. Pantallas placeholder Material 3 con textos en strings.xml (espanol).',
      '- Dejar lista la infraestructura de tests: JUnit4, Robolectric, compose-ui-test, y la herramienta de capturas recomendada en facts/fase-1.md, con al menos un test de humo de UI y una captura de humo que se generen headless.',
      '- AndroidManifest segun el plan (sin bloqueo de orientacion, adjustResize, uses-native-library opcionales para GPU, splash M3).',
    ].join('\n'),
    qa: 'Desde limpio: ./gradlew --console=plain clean assembleDebug testDebugUnitTest lintDebug en verde, mas la tarea de capturas configurada; ademas assembleRelease si la orden lo pide. Copiar app-debug.apk y las capturas PNG de humo a COORD/artifacts/fase-1/.',
  },
  2: {
    name: 'Dominio puro y tests de referencia', env: false, research: null, docs: false, build: true,
    lenses: ['correccion', 'conformidad'],
    notes: '- Implementar domain/* exactamente con el modelo y los valores de referencia de la Fase 2 del PLAN (US Navy, Katch-McArdle, Mifflin-St Jeor, Goal, GoalModifier entero, MacroPlan). Kotlin puro sin dependencias Android, redondeo con roundToInt. Tests JUnit con todos los valores de referencia y casos limite del plan.',
    qa: 'Desde limpio: ./gradlew --console=plain testDebugUnitTest lintDebug assembleDebug en verde; listar el numero de tests de dominio ejecutados y comprobar que cada valor de referencia del PLAN (Fase 2) tiene un test que lo afirma.',
  },
  3: {
    name: 'Repositorios de perfil y ajustes', env: false, research: null, docs: false, build: true,
    lenses: ['correccion', 'conformidad'],
    notes: '- ProfileRepository y SettingsRepository sobre DataStore Preferences (ultima version estable), AppContainer manual en KaiFitApp, Flows tipados, tests (DataStore en directorio temporal o Robolectric).',
    qa: 'Desde limpio: ./gradlew --console=plain testDebugUnitTest lintDebug assembleDebug en verde.',
  },
  4: {
    name: 'Pantallas Material 3', env: false, research: null, docs: false, build: true,
    lenses: ['correccion', 'ui', 'conformidad'],
    notes: [
      '- Calculadora, Resultados, Perfil, Ayuda y Ajustes segun la seccion "Pantallas" del PLAN, con ViewModels (StateFlow + collectAsStateWithLifecycle) y los repositorios de la Fase 3.',
      '- Entrada numerica que acepta coma y punto; validacion por campo; boton Calcular habilitado solo con datos validos; cadera solo con Mujer; % graso en vivo; objetivo -20..+20 con etiqueta; macros que suman el objetivo.',
      '- Ajustes: tema (sistema/claro/oscuro) y color dinamico funcionales; la seccion "Modelo" queda como placeholder que se completa en la Fase 5. Kai-Q sigue siendo placeholder.',
      '- @Preview claro y oscuro de cada pantalla; capturas PNG headless de todas las pantallas en ambos temas.',
      '- Tests de UI (Robolectric + compose-ui-test): navegacion entre pestanas, formulario de calculadora (boton deshabilitado con datos invalidos, cadera solo con Mujer), slider de objetivo y guardado en perfil.',
      '- Commits por pantalla.',
    ].join('\n'),
    qa: 'Desde limpio: ./gradlew --console=plain clean assembleDebug testDebugUnitTest lintDebug en verde y tarea de capturas. Copiar TODAS las capturas PNG (claro y oscuro) a COORD/artifacts/fase-4/screenshots/ y el APK a COORD/artifacts/fase-4/. Revisar visualmente varias capturas (Read de la imagen) y senalar defectos evidentes de layout.',
  },
  5: {
    name: 'Asistente Kai-Q con LiteRT-LM y Gemma 4 E2B', env: false, research: 'litertlm', docs: false, build: true,
    lenses: ['correccion', 'ui', 'conformidad'],
    notes: [
      '- Seguir la Fase 5 del PLAN y las firmas REALES de LiteRT-LM 0.18.0 documentadas en COORD/facts/fase-5.md.',
      '- Modelo: litert-community/gemma-4-E2B-it-litert-lm en Hugging Face, no restringido, apache-2.0. Fichero gemma-4-E2B-it.litertlm (2.588.147.712 bytes). Ver COORD/facts/entorno.md.',
      '- ModelManager (descarga con DownloadManager a getExternalFilesDir("models"), progreso, verificacion de tamano, borrar, importar archivo .litertlm por SAF), LlmRepository detras de una interfaz (Engine app-scoped, initialize en IO, conversacion con systemInstruction que incluye el perfil, streaming con Flow, cancelacion, close), AssistantViewModel y AssistantScreen con todos los estados del plan, y seccion Modelo de Ajustes.',
      '- No descargar el modelo de 2,6 GB en el contenedor. Los tests usan una implementacion falsa de la interfaz del LLM.',
      '- Comprobacion de RAM y espacio antes de descargar; mensajes claros; aviso de que no sustituye consejo medico.',
    ].join('\n'),
    qa: 'Desde limpio: ./gradlew --console=plain clean assembleDebug testDebugUnitTest lintDebug en verde y tarea de capturas (incluidos los estados del asistente). Copiar app-debug.apk a COORD/artifacts/fase-5/ y las capturas del asistente a COORD/artifacts/fase-5/screenshots/.',
  },
  6: {
    name: 'Pulido, documentacion y CI', env: false, research: null, docs: true, build: true,
    lenses: ['correccion', 'conformidad'],
    notes: [
      '- Implementador: .github/workflows/android.yml (assembleDebug, testDebugUnitTest, lintDebug; JDK 17 o 21; setup del SDK), revision de accesibilidad (contentDescription, semantics, objetivos tactiles), splash M3, values-en solo si no compromete calidad.',
      '- Documentador (agente aparte, despues del implementador): README.md reescrito en espanol (que hace, capturas de COORD/artifacts/fase-4 y fase-5 copiadas a docs/screenshots/, como funciona el asistente local, requisitos de dispositivo, como compilar, estructura, licencias Gemma/LiteRT-LM) y CHANGELOG.md con 2.0.0.',
    ].join('\n'),
    qa: 'Desde limpio: ./gradlew --console=plain clean assembleDebug testDebugUnitTest lintDebug en verde; validar el YAML del workflow de GitHub Actions (actionlint si se puede instalar, si no parseo YAML y revision manual); comprobar que los enlaces e imagenes del README existen. Copiar el APK final a COORD/artifacts/fase-6/.',
  },
}

const P = PHASES[N]
if (!P) throw new Error('Fase desconocida: ' + N)

const CTX = [
  'Proyecto: KaiFit-Pal, app Android. Repositorio en ' + REPO + ' (remoto origin = https://github.com/samartined/KaiFit-Pal).',
  'Se esta ejecutando el plan de modernizacion aprobado por el operador humano mediante varios agentes coordinados. Tu eres uno de ellos; tu rol se indica abajo.',
  'Directorio de coordinacion compartido (fuera del repo): ' + COORD,
  '- PLAN.md: plan aprobado (fuente de verdad). STATE.md: estado y decisiones vigentes (lo mantiene el coordinador).',
  '- facts/: hechos verificados (entorno, investigacion). work-orders/fase-N.md: orden de trabajo. reports/: informes de cada agente. artifacts/fase-N/: logs, APK, capturas.',
  'Fase actual: ' + N + ' - ' + P.name + '.',
  'Rama de trabajo: ' + BRANCH + '. Nunca modificar main, backup/original-java ni el tag v1.0.0. Nunca reescribir historia ya publicada (sin rebase, amend ni force-push sobre commits empujados). Solo el publicador hace push.',
  'Regla del repositorio: ningun fichero de codigo, recurso, comentario ni documento de producto (README, CHANGELOG, plan tecnico) puede mencionar modelos o asistentes de IA usados para desarrollar (Claude, Opus, Fable, Sonnet, Haiku) ni esta arquitectura de agentes. Los unicos ficheros que si la documentan son AGENTS.md, CLAUDE.md, HANDOFF.md, docs/dev/AGENT_WORKFLOW.md y docs/dev/agent-workflow/. Excepcion en los commits: cada mensaje de commit DEBE terminar exactamente con estas dos lineas (precedidas de una linea en blanco):',
  TRAILER,
  'Idiomas: textos de la app en espanol (strings.xml), codigo e identificadores en ingles, mensajes de commit en ingles con Conventional Commits, informes en COORD en espanol.',
  'Entorno: las variables de entorno no persisten entre llamadas a Bash. El SDK se referencia con local.properties (sdk.dir, ignorado por git); si existe COORD/env.sh, haz "source ' + COORD + '/env.sh" en cada llamada que lo necesite. Los builds de Gradle pueden superar el timeout por defecto de Bash: usa timeout 600000 o ejecucion en segundo plano y espera a que terminen.',
].join('\n')

const LENS_DESC = {
  correccion: 'CORRECCION: bugs, casos limite, ciclo de vida (ViewModel, recomposicion, rotacion, muerte de proceso), concurrencia y coroutines, aritmetica y redondeo frente a los valores de referencia del PLAN, calidad y suficiencia de los tests, y que build y tests pasen de verdad.',
  ui: 'UI Y PLATAFORMA: Compose idiomatico (estado elevado, remember/rememberSaveable, collectAsStateWithLifecycle, estabilidad), Material 3 (componentes, tokens de color/tipografia/forma del PLAN, modo oscuro, color dinamico), accesibilidad (contentDescription, semantics, objetivos tactiles de 48 dp, sp, contraste), edge-to-edge e insets, textos en strings.xml, rendimiento.',
  conformidad: 'CONFORMIDAD: el trabajo cumple la orden de trabajo y el PLAN (alcance completo, nada fuera de alcance, estructura de paquetes y ficheros, versiones de dependencias), higiene de commits (Conventional Commits, trailer exacto al final, ninguna mencion a modelos de IA o agentes en el repo), y veracidad de la documentacion tocada.',
}

const IMPL = { type: 'object', required: ['summary', 'commits', 'checks', 'outOfScope', 'blockers'], properties: {
  summary: { type: 'string' }, commits: { type: 'array', items: { type: 'string' } }, checks: { type: 'array', items: { type: 'string' } },
  outOfScope: { type: 'array', items: { type: 'string' } }, blockers: { type: 'array', items: { type: 'string' } } } }
const QA = { type: 'object', required: ['pass', 'summary', 'failures', 'artifacts'], properties: {
  pass: { type: 'boolean' }, summary: { type: 'string' }, failures: { type: 'array', items: { type: 'string' } }, artifacts: { type: 'array', items: { type: 'string' } } } }
const REVIEW = { type: 'object', required: ['verdict', 'findings'], properties: {
  verdict: { type: 'string', enum: ['approve', 'changes'] },
  findings: { type: 'array', items: { type: 'object', required: ['severity', 'file', 'issue', 'proposal'], properties: {
    severity: { type: 'string', enum: ['blocker', 'major', 'minor', 'nit'] }, file: { type: 'string' }, line: { type: 'string' },
    issue: { type: 'string' }, proposal: { type: 'string' } } } } } }
const DECISION = { type: 'object', required: ['decision', 'summary', 'fixes', 'question', 'outsidePlan', 'toolchainBlock'], properties: {
  decision: { type: 'string', enum: ['ACCEPT', 'REQUEST_FIX', 'ESCALATE_ADVISOR', 'ESCALATE_OPERATOR'] },
  summary: { type: 'string' }, fixes: { type: 'array', items: { type: 'string' } }, question: { type: 'string' },
  outsidePlan: { type: 'boolean' }, toolchainBlock: { type: 'boolean' },
  rejectedFindings: { type: 'array', items: { type: 'string' } }, deferred: { type: 'array', items: { type: 'string' } } } }
const PUBLISH = { type: 'object', required: ['pushed', 'refs', 'details'], properties: {
  pushed: { type: 'boolean' }, refs: { type: 'array', items: { type: 'string' } }, details: { type: 'string' } } }

function envPrompt() {
  return CTX + '\n\nROL: QA/Build - preparacion del entorno. No toques el codigo del repo.\n' + [
    '1. Lee ' + COORD + '/facts/entorno.md (hechos ya verificados).',
    '2. Descarga commandlinetools-linux-9862592_latest.zip de https://dl.google.com/android/repository/ e instalalo en /root/android-sdk/cmdline-tools/latest.',
    '3. Acepta licencias e instala platform-tools, la plataforma estable para compileSdk 37 (lista con sdkmanager --list; platforms;android-37.x estable, nunca beta ni canary) y la build-tools estable mas reciente compatible con AGP 9.4.1 (consulta las notas de version de AGP 9.4 si hace falta).',
    '4. Escribe ' + REPO + '/local.properties con sdk.dir=/root/android-sdk y confirma con git check-ignore que esta ignorado.',
    '5. Escribe ' + COORD + '/env.sh exportando ANDROID_HOME, ANDROID_SDK_ROOT y PATH (cmdline-tools/latest/bin y platform-tools).',
    '6. Documenta en ' + COORD + '/facts/entorno-sdk.md: rutas, salida de sdkmanager --list_installed, versiones y cualquier problema de red o certificados y como se resolvio.',
    'Devuelve un resumen breve de lo instalado.',
  ].join('\n')
}

function researchPrompt(kind) {
  if (kind === 'toolchain') {
    return CTX + '\n\nROL: Investigador (Fase 1). Objetivo: hechos verificados para que el implementador configure el build sin tanteos. No modifiques el repo.\n' + [
      'Lee ' + COORD + '/PLAN.md (Fase 1), ' + COORD + '/facts/entorno.md y ' + COORD + '/facts/entorno-sdk.md (SDK ya instalado; usa source ' + COORD + '/env.sh).',
      'Averigua y VERIFICA (maven-metadata.xml en https://dl.google.com/dl/android/maven2/ y https://repo1.maven.org/maven2/, notas de version oficiales con WebFetch/WebSearch):',
      '- Configuracion correcta de AGP 9.4.1 con Kotlin 2.4.21: Kotlin integrado en AGP 9 (si NO debe aplicarse org.jetbrains.kotlin.android y como se configura jvmTarget/compilerOptions), plugin org.jetbrains.kotlin.plugin.compose, plugin de kotlinx-serialization, DSL de compileSdk 37 (incluida la variante minor 37.x si aplica), build-tools requerido, compatibilidad con Gradle 9.8.1.',
      '- Ultimas versiones ESTABLES (no alpha/beta/rc) de: activity-compose, core-ktx, core-splashscreen, lifecycle-runtime-compose, lifecycle-viewmodel-compose, navigation-compose (2.10.2), datastore-preferences, kotlinx-serialization-json, kotlinx-coroutines, junit 4, robolectric (4.17), androidx.test (core, ext-junit), compose-ui-test-junit4 (via BOM).',
      '- Herramienta de capturas headless sin emulador compatible con AGP 9.4: compara Roborazzi y Compose Preview Screenshot Testing; recomienda UNA.',
      'Luego VALIDA todo con un proyecto minimo desechable en ' + COORD + '/sandbox/toolchain-check (NUNCA en el repo): genera el wrapper Gradle 9.8.1 con /opt/gradle-8.14.3/bin/gradle wrapper --gradle-version 9.8.1, version catalog, una Activity Compose con un Text de Material 3, un test Robolectric + compose-ui-test, y una captura con la herramienta elegida. Ejecuta assembleDebug, testDebugUnitTest, lintDebug y la tarea de capturas hasta que pasen.',
      'Escribe ' + COORD + '/facts/fase-1.md con: versiones exactas, contenido COMPLETO de los ficheros de build que funcionaron (settings, build raiz, app, libs.versions.toml, gradle.properties, wrapper properties), comandos exactos, tiempos aproximados y trampas encontradas.',
      'Devuelve un resumen de 10 lineas.',
    ].join('\n')
  }
  return CTX + '\n\nROL: Investigador (Fase 5). Objetivo: la API REAL de LiteRT-LM 0.18.0 para Android y como integrar Gemma 4 E2B. No modifiques el repo.\n' + [
    'Lee ' + COORD + '/PLAN.md (Fase 5) y ' + COORD + '/facts/entorno.md.',
    '1. Descarga el AAR y el POM de com.google.ai.edge.litertlm:litertlm-android:0.18.0 desde https://dl.google.com/dl/android/maven2/ a ' + COORD + '/sandbox/litertlm. Extrae classes.jar y usa javap -public (o inspeccion de los .class/metadata Kotlin) para listar las firmas publicas reales de Engine, EngineConfig, Backend, ConversationConfig, Conversation, Contents/Content, Message, SamplerConfig, MessageCallback, ExperimentalFlags y excepciones. Determina como se cancela una generacion en curso (metodo o cancelacion del Flow), que es suspend o bloqueante, y que hay que cerrar.',
    '2. Del AAR: minSdk del manifiesto, ABIs y tamano de las librerias nativas, entradas de manifiesto, reglas consumer de proguard, dependencias transitivas.',
    '3. Revisa el codigo de la app AI Edge Gallery (https://github.com/google-ai-edge/gallery) y las release notes de https://github.com/google-ai-edge/LiteRT-LM para el patron recomendado: descarga del modelo, inicializacion del motor, backend GPU con fallback a CPU, formato del prompt de sistema, limites de contexto, errores habituales.',
    '4. Comprueba si la clase puede cargarse en tests JVM/Robolectric (librerias nativas) para decidir la estrategia de tests (interfaz + fake).',
    '5. Confirma con una peticion HEAD la URL y tamano del fichero gemma-4-E2B-it.litertlm y si existe una variante recomendada para GPU (gemma-4-E2B-it-gpu.litertlm): diferencias.',
    'Escribe ' + COORD + '/facts/fase-5.md con firmas exactas (copia literal de javap), un ejemplo Kotlin minimo correcto para 0.18.0 (init en IO, conversacion con systemInstruction, streaming, cancelacion, close), y recomendaciones. Devuelve un resumen de 10 lineas.',
  ].join('\n')
}

function orderPrompt() {
  return CTX + '\n\nROL: Coordinador - orden de trabajo. Tu no implementas codigo.\n' + [
    'Lee ' + COORD + '/PLAN.md, ' + COORD + '/STATE.md, todo ' + COORD + '/facts/, los informes de fases anteriores en ' + COORD + '/reports/ y el estado del repo (git status, git log --oneline -20).',
    'Notas especificas de esta fase:',
    P.notes,
    'Escribe ' + COORD + '/work-orders/fase-' + N + '.md con estas secciones:',
    '1. Base: SHA del commit de partida para revisar el diff (en Fase 0 es 50e5e23eca50ded1b7aa95f264ccb2ce9b521566; en otras fases, git rev-parse HEAD de la rama de trabajo).',
    '2. Objetivo.',
    '3. Alcance exacto: ficheros a crear, modificar y borrar (rutas relativas al repo).',
    '4. Decisiones tecnicas: lo que el plan deja abierto, decidelo tu y justifica en una linea.',
    '5. Criterios de aceptacion verificables: comandos exactos y comportamientos esperados. Incluye como minimo: ' + P.qa,
    '6. Plan de commits: commits pequenos y logicos con mensajes Conventional Commits en ingles.',
    '7. Fuera de alcance.',
    '8. Riesgos y como mitigarlos.',
    'Respeta la seccion "Instruccion del operador" de STATE.md: no apliques decisiones del operador pendientes; si la fase toca algo afectado, deja el estado actual y anotalo.',
    'Actualiza ' + COORD + '/STATE.md poniendo "Fase actual: ' + N + ' (en curso)".',
    'Devuelve un resumen de 5 a 10 lineas de la orden.',
  ].join('\n')
}

function implementPrompt(round, fixes) {
  const head = CTX + '\n\nROL: Implementador. Eres el unico agente que modifica el codigo en esta fase.\n'
  const common = [
    'Reglas:',
    '- Trabaja en la rama ' + BRANCH + ' (en la Fase 0 la creas tu segun la orden).',
    '- Commits pequenos y logicos con Conventional Commits en ingles; cada mensaje termina con las dos lineas obligatorias indicadas arriba. Ningun commit puede dejar el arbol sin compilar si la fase compila.',
    '- No hagas push. No toques main, backup/original-java ni v1.0.0.',
    '- Antes de terminar, ejecuta tu mismo los criterios de aceptacion de la orden (comandos exactos) y corrige hasta que pasen. Deja el arbol de trabajo limpio (todo commiteado, nada sin seguimiento salvo ficheros ignorados).',
    '- Si algo de la orden es imposible o incorrecto, no lo fuerces: haz lo mas cercano correcto y explicalo en blockers u outOfScope.',
    '- Escribe tu informe en ' + COORD + '/reports/fase-' + N + '-impl-r' + round + '.md: que hiciste, decisiones, comandos ejecutados con su resultado, lo que quedo fuera y por que.',
    'Devuelve: summary, commits (sha corto + asunto), checks (comando -> resultado), outOfScope, blockers.',
  ].join('\n')
  if (round === 0) {
    return head + 'Lee tu orden de trabajo ' + COORD + '/work-orders/fase-' + N + '.md, ademas de PLAN.md, STATE.md y facts/. Implementala completa.\n' + (args.resumeNote ? args.resumeNote + '\n' : '') + common
  }
  return head + 'RONDA DE CORRECCION ' + round + '. Lee tu orden ' + COORD + '/work-orders/fase-' + N + '.md, tus informes anteriores ' + COORD + '/reports/fase-' + N + '-impl-r*.md y la decision del coordinador ' + COORD + '/reports/fase-' + N + '-decision-r' + (round - 1) + '.md.\nEl coordinador pide estas correcciones, en orden de prioridad:\n' + fixes.map((f, i) => (i + 1) + '. ' + f).join('\n') + '\nAplicalas todas. Commits nuevos (sin reescribir los anteriores).\n' + common
}

function docsPrompt() {
  return CTX + '\n\nROL: Documentador. Solo tocas documentacion (README.md, CHANGELOG.md, docs/), nunca codigo fuente.\n' + [
    'Lee la orden ' + COORD + '/work-orders/fase-' + N + '.md (parte de documentacion), PLAN.md, STATE.md y el codigo actual para que todo lo que escribas sea veraz.',
    'Reescribe README.md en espanol y crea CHANGELOG.md (2.0.0) segun la orden. Copia capturas representativas de ' + COORD + '/artifacts/fase-4/screenshots y ' + COORD + '/artifacts/fase-5/screenshots a docs/screenshots/ (pocas y bien elegidas) y enlazalas.',
    'Commits con Conventional Commits en ingles (docs: ...) y las dos lineas finales obligatorias. Sin push.',
    'Escribe tu informe en ' + COORD + '/reports/fase-' + N + '-docs.md y devuelve un resumen breve.',
  ].join('\n')
}

function qaPrompt(round) {
  return CTX + '\n\nROL: QA/Build. Verificas de forma independiente. No modificas codigo fuente ni haces commits (solo puedes crear local.properties o ficheros dentro de ' + COORD + ').\n' + [
    'Lee la orden ' + COORD + '/work-orders/fase-' + N + '.md (criterios de aceptacion y Base) y el informe del implementador ' + COORD + '/reports/fase-' + N + '-impl-r' + round + '.md.',
    'Ejecuta los criterios de aceptacion tal cual. En particular: ' + P.qa,
    'Comprueba tambien: arbol limpio y rama ' + BRANCH + '; todos los mensajes de commit desde Base terminan exactamente con las dos lineas obligatorias; git grep -niE "claude|opus|fable|sonnet|haiku" -- . ":(exclude)AGENTS.md" ":(exclude)CLAUDE.md" ":(exclude)HANDOFF.md" ":(exclude)docs/dev/AGENT_WORKFLOW.md" ":(exclude)docs/dev/agent-workflow" no da resultados (justifica cualquier falso positivo); ningun secreto ni clave en el repo.',
    'Guarda logs en ' + COORD + '/artifacts/fase-' + N + '/r' + round + '/ y copia ahi los artefactos que pida la orden (APK, capturas).',
    'Escribe ' + COORD + '/reports/fase-' + N + '-qa-r' + round + '.md. pass=true solo si TODOS los criterios pasan. En failures pon cada fallo con el extracto de error relevante. En artifacts las rutas absolutas de los artefactos copiados.',
  ].join('\n')
}

function reviewPrompt(lens, round, prior) {
  const extra = prior && prior.length ? '\nComprueba especialmente si se resolvieron estos hallazgos previos:\n' + prior.map(f => '- [' + f.severity + '] ' + f.file + (f.line ? ':' + f.line : '') + ' ' + f.issue).join('\n') : ''
  return CTX + '\n\nROL: Revisor, lente ' + LENS_DESC[lens] + '\nSolo lectura: no modifiques ficheros del repo ni hagas commits.\n' + [
    'Lee la orden ' + COORD + '/work-orders/fase-' + N + '.md (Base y criterios), PLAN.md y el informe del implementador ' + COORD + '/reports/fase-' + N + '-impl-r' + round + '.md.',
    'Revisa el diff completo de la fase: git diff <Base>..HEAD en ' + REPO + ', y lee los ficheros enteros cuando el contexto lo requiera.' + extra,
    'Reporta solo problemas reales y verificados: fichero, linea, fallo concreto y correccion propuesta. Severidad: blocker (rompe build, tests o funcionalidad, o viola una decision del plan), major (bug real o desviacion importante), minor, nit. No inventes problemas para justificar la revision; si esta bien, verdict=approve con lista vacia o solo nits.',
    'Escribe tu informe en ' + COORD + '/reports/fase-' + N + '-review-' + lens + '-r' + round + '.md.',
  ].join('\n')
}

function decidePrompt(round, impl, qa, reviews) {
  return CTX + '\n\nROL: Coordinador - decision de la ronda ' + round + '. Medias entre implementador, QA y revisores. No implementas codigo.\n' + [
    'Informe del implementador (JSON): ' + JSON.stringify(impl),
    'Resultado de QA (JSON): ' + JSON.stringify(qa),
    'Revisiones (JSON): ' + JSON.stringify(reviews),
    'Lee tambien los informes completos en ' + COORD + '/reports/fase-' + N + '-*-r' + round + '.md y verifica tu mismo en el repo lo que dudes.',
    'Mediacion: si un hallazgo es incorrecto o fuera de alcance, rechazalo con su razon (rejectedFindings). Si revisor e implementador discrepan, decide tu y explica por que.',
    'Reglas de decision:',
    '- ACCEPT solo si QA pass=true y no queda ningun blocker ni major valido. Los minor/nit validos puedes pedirlos si son baratos o diferirlos (deferred) anotandolos en STATE.md, seccion Pendientes.',
    '- REQUEST_FIX: lista consolidada, priorizada y accionable en fixes; cada item autocontenido (fichero, problema, cambio esperado).',
    '- ESCALATE_ADVISOR (ultimo recurso): solo si (a) es la ronda 2 o posterior y el mismo bloqueo persiste, (b) hay una decision con compensaciones que el plan no cubre (outsidePlan=true), o (c) un bloqueo de toolchain sin via conocida (toolchainBlock=true). Formula en question una pregunta concreta y autocontenida con el contexto necesario.',
    '- Decisiones que corresponden al operador (producto, textos, alcance): NO las apliques ni escales; deja el estado actual o lo que diga el PLAN, anotalas en STATE.md seccion "Decisiones para revision final del operador (NO aplicadas)" con contexto, opciones y tu propuesta, y sigue. ESCALATE_OPERATOR solo para acciones destructivas o cambios de alcance graves que impidan continuar. Pregunta en question.',
    'Actualiza ' + COORD + '/STATE.md: historial de la fase con esta decision, decisiones nuevas, pendientes; si ACCEPT, "Fase ' + N + ' aceptada (pendiente de push)".',
    'Escribe ' + COORD + '/reports/fase-' + N + '-decision-r' + round + '.md con la decision y su razonamiento.',
    'Devuelve decision, summary (5-10 lineas para el operador), fixes (vacio si no aplica), question (vacia si no aplica), outsidePlan, toolchainBlock, rejectedFindings y deferred.',
  ].join('\n')
}

function publishPrompt() {
  const refs = N === 0
    ? 'la rama backup/original-java, el tag v1.0.0 y la rama ' + BRANCH + ' (git push -u origin backup/original-java; git push origin v1.0.0; git push -u origin ' + BRANCH + ')'
    : 'la rama ' + BRANCH + ' (git push -u origin ' + BRANCH + ')'
  return CTX + '\n\nROL: Publicador. Solo publicas; no cambias ficheros ni commits.\n' + [
    'Comprueba que estas en ' + BRANCH + ' con el arbol limpio. Publica ' + refs + '.',
    'Reintenta solo ante errores de red, hasta 4 veces con esperas de 2, 4, 8 y 16 segundos. Nunca force-push. Si el remoto rechaza por permisos o politica (403), no intentes rodearlo: informa del error exacto.',
    'Verifica con git ls-remote --heads --tags origin que las referencias remotas apuntan a los SHA locales.',
    'Actualiza ' + COORD + '/STATE.md: "Fase ' + N + ' completada y publicada" con los SHA. Devuelve pushed, refs (ref -> sha) y details.',
  ].join('\n')
}

function pickLenses(prev) {
  const flagged = prev.filter(r => r.verdict === 'changes' || (r.findings || []).some(f => f.severity === 'blocker' || f.severity === 'major')).map(r => r.lens)
  return flagged.length ? flagged : ['correccion']
}

// ---------------- ejecucion ----------------
log('Fase ' + N + ': ' + P.name)

if (P.env) {
  await agent(envPrompt(), { label: 'qa:entorno', phase: 'Preparar', model: OPUS, effort: 'medium' })
}
if (P.research) {
  await agent(researchPrompt(P.research), { label: 'investigador', phase: 'Preparar', model: OPUS, effort: 'medium' })
}

const order = await agent(orderPrompt(), { label: 'coordinador:orden', phase: 'Ordenar', model: OPUS, effort: 'high' })
if (!order) return { status: 'ERROR', phase: N, where: 'orden' }

let round = 0
let maxRounds = 4
let adviceIdx = 0
let answerIdx = 0
let fixes = []
let prevReviews = []
let lastQa = null
const history = []

while (true) {
  const tag = 'r' + round
  const impl = await agent(implementPrompt(round, fixes), { label: 'implementador:' + tag, phase: 'Implementar', model: OPUS, effort: 'xhigh', schema: IMPL })
  if (!impl) return { status: 'ERROR', phase: N, where: 'implementador ' + tag, history }
  if (round === 0 && P.docs) {
    await agent(docsPrompt(), { label: 'documentador', phase: 'Implementar', model: OPUS, effort: 'medium' })
  }

  const lenses = round === 0 ? P.lenses : pickLenses(prevReviews)
  const priorFor = l => { const r = prevReviews.find(x => x.lens === l); return r ? (r.findings || []).filter(f => f.severity !== 'nit') : [] }
  const results = await parallel([
    () => agent(qaPrompt(round), { label: 'qa:' + tag, phase: 'Verificar', model: OPUS, effort: 'medium', schema: QA }),
    ...lenses.map(l => () => agent(reviewPrompt(l, round, priorFor(l)), { label: 'revisor:' + l + ':' + tag, phase: 'Verificar', model: OPUS, effort: 'high', schema: REVIEW })
      .then(r => (r ? Object.assign({ lens: l }, r) : null))),
  ])
  lastQa = results[0] || { pass: false, summary: 'El agente de QA no devolvio resultado', failures: ['QA sin resultado'], artifacts: [] }
  const reviews = results.slice(1).filter(Boolean)
  log(tag + ': QA ' + (lastQa.pass ? 'OK' : 'FALLA') + '; revisiones: ' + reviews.map(r => r.lens + '=' + r.verdict).join(', '))

  const decision = await agent(decidePrompt(round, impl, lastQa, reviews), { label: 'coordinador:decision:' + tag, phase: 'Decidir', model: OPUS, effort: 'high', schema: DECISION })
  if (!decision) return { status: 'ERROR', phase: N, where: 'decision ' + tag, history }
  history.push({ round, decision: decision.decision, summary: decision.summary, qaPass: lastQa.pass })

  let d = decision.decision
  if (d === 'ESCALATE_ADVISOR' && round < 2 && !decision.outsidePlan && !decision.toolchainBlock) {
    log('Escalado al asesor rechazado por el guion (ultimo recurso): se convierte en correccion')
    d = 'REQUEST_FIX'
  }
  if (d === 'REQUEST_FIX' && round + 1 >= maxRounds) {
    log('Limite de rondas alcanzado sin ACCEPT: escalado automatico al asesor')
    d = 'ESCALATE_ADVISOR'
  }
  if (d === 'ACCEPT') break

  if (d === 'ESCALATE_ADVISOR') {
    if (adviceIdx < ADVICE.length) {
      log('Aplicando asesoria del experto #' + (adviceIdx + 1))
      fixes = (decision.fixes || []).concat(['ASESORIA DEL EXPERTO (prioritaria, aplicala): ' + ADVICE[adviceIdx]])
      adviceIdx++
      maxRounds = round + 1 + 2
      prevReviews = reviews
      round++
      continue
    }
    return { status: 'ESCALATE_ADVISOR', phase: N, round, question: decision.question || decision.summary, summary: decision.summary, fixes: decision.fixes, qa: lastQa, history }
  }
  if (d === 'ESCALATE_OPERATOR') {
    if (answerIdx < ANSWERS.length) {
      log('Aplicando respuesta del operador #' + (answerIdx + 1))
      fixes = (decision.fixes || []).concat(['DECISION DEL OPERADOR HUMANO (obligatoria): ' + ANSWERS[answerIdx]])
      answerIdx++
      maxRounds = Math.max(maxRounds, round + 3)
      prevReviews = reviews
      round++
      continue
    }
    return { status: 'ESCALATE_OPERATOR', phase: N, round, question: decision.question || decision.summary, summary: decision.summary, qa: lastQa, history }
  }

  fixes = (decision.fixes && decision.fixes.length) ? decision.fixes : ['Resuelve los problemas senalados en ' + COORD + '/reports/fase-' + N + '-decision-r' + round + '.md']
  prevReviews = reviews
  round++
}

const pub = await agent(publishPrompt(), { label: 'publicador', phase: 'Publicar', model: OPUS, effort: 'low', schema: PUBLISH })
if (!pub || !pub.pushed) return { status: 'PUBLISH_FAILED', phase: N, publish: pub, qa: lastQa, history }
return { status: 'DONE', phase: N, rounds: round + 1, publish: pub, artifacts: lastQa.artifacts, qaSummary: lastQa.summary, history }
