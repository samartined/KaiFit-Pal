# HANDOFF · Modernización 2.0.0

Estado al cierre de la sesión del 9 y 10 de octubre de 2026. Este documento es el punto de partida para cualquier agente o persona que continúe el trabajo. Las reglas de trabajo están en [`AGENTS.md`](AGENTS.md).

## Resumen

La app original (Java + XML, del TFG) se ha reescrito entera en Kotlin + Jetpack Compose + Material 3. Kai-Q ya no usa OpenAI: ejecuta Gemma 4 E2B en el móvil con LiteRT-LM 0.18.0. Todo está publicado en la rama `feat/ui-modernization` y la CI de GitHub pasa.

Falta:
- probarlo en un dispositivo real;
- corregir los dos hallazgos «mayor» de la revisión del asesor (ver al final);
- que el operador tome las decisiones pendientes;
- fusionar en `main`.

## Estado del repositorio

| Referencia | Commit | Notas |
|---|---|---|
| `main` (remoto) | `50e5e23` | Versión original, sin tocar |
| `backup/original-java` (remoto) | `50e5e23` | Respaldo de la versión original |
| tag `v1.0.0` | anotado sobre `50e5e23` | **Solo en el clon local de la sesión**: el proxy rechazó el push de tags |
| `feat/ui-modernization` (remoto) | `9208a82` + documentación de agentes | 78 commits de código y docs sobre `50e5e23` |

- **CI**: el workflow `Android CI` pasó en `9208a82`: [run 38028710108](https://github.com/samartined/KaiFit-Pal/actions/runs/38028710108). Cada run publica el APK de depuración como artefacto.
- **Métricas al cierre**: 409 tests (JUnit + Robolectric) sin fallos, lint con 0 errores, 49 capturas Roborazzi y build de release con R8 correcto.
- **Sin pull request abierta** y nada fusionado en `main`: lo decide el operador.

## Qué se hizo

| Fase | Commits | Rango | Contenido |
|---|---|---|---|
| 0 | 4 | `50e5e23..d606c88` | Rama de respaldo, limpieza del repo, plan técnico en `docs/dev` |
| 1 | 10 | `..9de192e` | Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00; tema Material 3 claro y oscuro; navegación con cinco pestañas; infraestructura de tests y capturas |
| 2 | 7 | `..c5d8a3e` | Dominio en Kotlin puro: US Navy, Katch-McArdle, Mifflin-St Jeor, objetivo entero, macros; 101 tests con valores de referencia |
| 3 | 7 | `..a24ae7f` | DataStore para perfil y ajustes, contenedor de dependencias manual |
| 4 | 13 | `..bea0a9e` | Calculadora, Resultados, Perfil, Ayuda y Ajustes con ViewModels, tests de UI y capturas |
| 5 | 14 | `..c3da316` | Kai-Q local: descarga e importación del modelo, motor LiteRT-LM con paso de GPU a CPU, chat en streaming, gestión del modelo en Ajustes |
| 6 | 23 | `..9208a82` | CI de GitHub Actions, accesibilidad, splash e icono con colores de marca, README, CHANGELOG |

Los bugs de la versión original quedan corregidos y cubiertos por tests: crashes al recrear la Activity y al calcular con campos vacíos, pila de navegación creciente, macros al 105 %, etiqueta de −15 % errónea, edad sin usar y build roto sin `local.properties`. La lista completa está en `CHANGELOG.md`.

Cada fase la ejecutó un equipo coordinado de agentes: implementador, QA, tres revisores y coordinador. Hubo una o dos rondas de corrección por fase. El proceso está en [`docs/dev/AGENT_WORKFLOW.md`](docs/dev/AGENT_WORKFLOW.md).

## Qué está verificado y qué no

- **Verificado en la JVM y la CI**: fórmulas con valores de referencia, validación, repositorios, ViewModels, navegación y restauración de estado, pantallas con Robolectric, accesibilidad básica (48 dp y encabezados), build de release con R8 y reglas de conservación.
- **No verificado**: nada se ha ejecutado en un móvil ni en emulador. En particular, el motor LiteRT-LM real (la JVM no carga sus librerías nativas), la descarga real de 2,6 GB, la importación por el selector de archivos, la GPU, TalkBack real y el APK de release minificado.
- Las respuestas que aparecen en las capturas de Kai-Q son de un modelo simulado de los tests.

## Checklist de dispositivo (pendiente)

Prioridad de arriba abajo. Usa un móvil arm64 con 6 GB de RAM o más.

1. Kai-Q descarga el modelo por Wi-Fi, muestra el progreso, lo verifica y carga el motor en GPU. Responde en español y en streaming, y «Detener» corta la respuesta.
2. Forzar CPU en Ajustes: carga y responde. Cambiar de GPU a CPU mientras Kai-Q muestra «Preparando Kai-Q…» termina en CPU.
3. Importar un `.litertlm` con el selector de archivos. Matar la app a mitad de la importación y volver: el espacio libre vuelve al de antes.
4. Rotar y cambiar de tema con el chat abierto y durante una respuesta. Volver de segundo plano tras presión de memoria: el chat se vacía con aviso y el motor recarga.
5. Calculadora, Resultados, Perfil y Ajustes con coma y punto decimal, en claro, oscuro y color dinámico.
6. TalkBack: encabezados, sliders y el aviso «Kai-Q ha terminado de responder». Compose no emite un evento propio cuando aparece un nodo `liveRegion` nuevo. Si TalkBack no lo lee, cambia `ReplyFinishedAnnouncement` (en `ui/assistant/AssistantScreen.kt`) por un nodo persistente cuyo texto cambie al terminar, y ajusta su test.
7. APK de release firmado: navegación tipada y Kai-Q funcionan con R8.
8. En un móvil de 32 bits, Kai-Q muestra «no compatible» sin cerrarse.
9. Hallazgo M1 del asesor: «Detener» e inmediatamente «Nueva conversación»; cambiar de procesador, borrar el modelo o pasar a segundo plano con una respuesta en curso. Buscar cierres nativos en logcat (`SIGSEGV`, `litertlm`).
10. Hallazgo M2 del asesor: en Android 14 o superior, salir a otra app 10–30 segundos y volver a Kai-Q. ¿Se vacía el chat y recarga el motor?
11. Tocar la notificación del sistema «Descarga completada» al terminar la descarga (hallazgo m8).

## Decisiones pendientes del operador

No se ha aplicado ninguna. El detalle con opciones y propuestas está en [`docs/dev/DECISIONS.md`](docs/dev/DECISIONS.md#decisiones-pendientes-del-operador-no-aplicadas).

1. Crear el tag `v1.0.0` en GitHub sobre `50e5e23`.
2. Etiqueta corta «Calcular» en la pestaña (se trunca a 360 dp).
3. Copia de seguridad del perfil en la nube de Google.
4. Autoría de los commits de la rama de trabajo.
5. Licencia del proyecto (no hay `LICENSE`).
6. Icono propio en lugar del robot de la plantilla.
7. Proveedor de caché de `setup-gradle` en la CI.

## Deuda técnica conocida

- **Splash y tema elegido**: el splash y el fondo de ventana siguen el modo noche del sistema, no el tema elegido en Ajustes. Con temas distintos hay un salto de color al abrir la app. Opción: `UiModeManager.setApplicationNightMode` en API 31+.
- **Capturas de referencia**: no hay capturas versionadas para `verifyRoborazziDebug`. Conviene versionarlas cuando el diseño se estabilice, incluida una a 360 dp con fuente ×1,3.
- **Flecha atrás de Resultados**: si algún día se abre desde otro origen (Perfil o deep link), revisar `navigateUp()` frente a `popBackStack()`.
- **Animación de la barra inferior** al entrar en Resultados y salir: cosmético.
- **Firma de release**: no hay configuración de firma. Para instalar encima de la 1.0.0 hace falta la clave original.
- **Asunto del commit `53348d7`** de 73 caracteres: aceptado; no se reescribe la historia.

## Siguientes pasos sugeridos

1. El operador revisa las decisiones pendientes y prueba el APK con la checklist de dispositivo.
2. Una fase de corrección con la misma arquitectura para M1 y M2, y de paso los menores baratos: m4 (`assembleRelease` y caché básica en la CI), m7, m8 y la sugerencia de borrar el modelo de m1.
3. Aplicar las decisiones que tome el operador en esta misma rama antes de fusionar, o en una rama nueva (por ejemplo `chore/operator-decisions`).
4. Corregir lo que salga de la prueba en dispositivo.
5. Abrir la pull request de `feat/ui-modernization` a `main` cuando el operador lo pida.
6. Después, el pase de identidad visual (icono y personalidad) que quedó fuera de esta versión.

## Cómo retomar

- **Entorno**: sigue la sección de entorno de `AGENTS.md`. El SDK del contenedor de esta sesión no persiste.
- **Referencias técnicas**: `docs/dev/reference/` (API real de LiteRT-LM 0.18.0 y toolchain) y `docs/dev/env/` (espejo de Maven Central para el contenedor).
- **Directorio de coordinación**: en la sesión original estaba en un directorio temporal que ya no existirá (órdenes de trabajo, informes, logs). Lo esencial está volcado en `docs/dev/`.
- **Contexto de la conversación con el operador**, en orden:
  - Pidió un lavado de cara completo y moderno, resolver bugs y trabajar en una rama nueva con nombre estandarizado, más una rama de respaldo del estado original.
  - Descartó OpenAI y pidió un modelo local tipo Gemma, investigando las versiones más recientes.
  - Aceptó Kotlin + Compose + Material 3, LiteRT-LM + Gemma 4 E2B, el Perfil con datos reales y los nombres de rama sin "tfg".
  - Exigió la ejecución multiagente descrita en `AGENT_WORKFLOW.md`.
  - Pidió no ir narrando las decisiones pendientes y dejarlas para el final, y aclaró que no se aplican por defecto.
  - Al terminar, pidió este fichero, `AGENTS.md` y una verificación independiente con el asesor Fable.

## Revisión del asesor experto

Al cierre, el operador pidió una verificación independiente al asesor experto (Claude Fable, esfuerzo `xhigh`, solo lectura). El informe completo está en [`docs/dev/agent-workflow/final-advisor-review.md`](docs/dev/agent-workflow/final-advisor-review.md).

**Veredicto**: trabajo sólido. El dominio, recalculado de forma independiente, es correcto. Los tests prueban lo que dicen y no hay nada bloqueante. Listo para probar en dispositivo. Para fusionar en `main` recomienda antes la primera prueba real de Kai-Q y corregir M1 y M2.

**Hallazgos abiertos** (ninguno corregido todavía; los dos primeros se confirmaron leyendo el código):

| Id | Severidad | Resumen | Dónde |
|---|---|---|---|
| M1 | Mayor | Se cierra la conversación nativa justo después de `cancelProcess()`, que es asíncrono: posible uso tras liberar si se pulsa «Nueva conversación», se cambia de procesador o se borra el modelo durante una respuesta | `data/llm/LiteRtLmRepository.kt`, `closeConversationLocked` |
| M2 | Mayor | En Android 14+ llega `TRIM_MEMORY_BACKGROUND` poco después de salir de la app: se libera el motor y se borra el chat casi siempre | `KaiFitApp.kt`, `ui/assistant/AssistantViewModel.kt` |
| m1 | Menor | Sin SHA-256 del modelo; ante un modelo corrupto, Kai-Q no sugiere borrarlo | `data/llm/DownloadModelManager.kt` |
| m2 | Menor | 6 GB de RAM recomendados frente a los 8 GB de AI Edge Gallery | `data/llm/ModelSpec.kt` |
| m3 | Menor | El APK de release incluye la librería `x86_64` (26 MB, solo emuladores) | `app/build.gradle.kts` |
| m4 | Menor | La CI no compila release; caché comercial por defecto; acciones por tag mayor | `.github/workflows/android.yml` |
| m5 | Menor | Sin `signingConfig`: el APK de la CI no se instala encima de una 1.0.0 firmada con otra clave | documentación |
| m6 | Menor | Sin tests de los adaptadores Android reales (descargas, importación, dispositivo) | `data/llm/SystemDownloads.kt`, `ImportSources.kt`, `DeviceInfo.kt` |
| m7 | Menor | Cambiar de procesador mientras carga encadena dos cargas completas | `ui/assistant/AssistantViewModel.kt` |
| m8 | Menor | Tocar la notificación «Descarga completada» intenta abrir el `.part` | `data/llm/SystemDownloads.kt` |

Las cinco notas menores (aviso duplicado al borrar el modelo, Markdown en las respuestas, mensaje genérico sin gestor de descargas, etc.) están en el informe.

**Lección de proceso**: M1 venía del ejemplo de la investigación y los revisores comprobaron la conformidad con la orden, no la semántica nativa. En trabajo futuro, una lente de revisión debe preguntarse si la orden misma es correcta.
