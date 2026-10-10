# Registro de cambios

Todos los cambios relevantes de KaiFit-Pal se documentan en este fichero.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto sigue [Versionado Semántico](https://semver.org/lang/es/).

## [2.0.0] - 2026-10-10

Reescritura completa de la app. Conserva el `applicationId` (`com.tfg.kaifit_pal`), así que se instala como actualización de la 1.0.0.

### Añadido

- Interfaz nueva en Kotlin con Jetpack Compose y Material 3, en español, con tema claro y oscuro, color dinámico opcional en Android 12 o superior, pantalla de inicio y icono adaptativo con los colores de la marca.
- Las cinco pestañas con contenido real: Perfil, Calculadora, Kai-Q, Ayuda (preguntas frecuentes) y Ajustes. Resultados es una pantalla de detalle con flecha atrás.
- Cálculo con Mifflin-St Jeor (usa la edad) cuando no hay medidas para estimar la grasa corporal; con ellas, US Navy + Katch-McArdle como antes.
- Validación de los datos mientras se escriben, con mensajes de error en cada campo y coma o punto decimal.
- Resultados con objetivo de −20 % a +20 % en pasos de 5 % y macros ajustables: la proteína y la grasa se eligen y los carbohidratos son el resto, de modo que el total coincide con el objetivo.
- Perfil guardado (medición y resultado) y ajustes con DataStore, con opción de borrar los datos.
- Kai-Q como asistente local: Gemma 4 E2B ejecutado en el dispositivo con LiteRT-LM 0.18.0, con descarga verificada desde Hugging Face (opción solo Wi-Fi) o importación de un fichero `.litertlm`, GPU con paso automático a CPU, respuesta en streaming, detener, nueva conversación, preguntas sugeridas y el perfil guardado como contexto.
- Gestión del modelo en Ajustes: descargar, importar, cancelar, borrar, elegir procesador y descarga solo con Wi-Fi; licencias del modelo y del motor en «Acerca de».
- Accesibilidad: encabezados, descripciones para TalkBack, estado legible de los sliders y de las preguntas desplegables, objetivos táctiles de al menos 48 dp, etiqueta y contador en el campo del chat y aviso cuando Kai-Q termina de responder.
- Tests unitarios del dominio con valores de referencia, de los repositorios, del gestor del modelo, de los ViewModels y de las pantallas (Robolectric), más capturas de pantalla con Roborazzi.
- Integración continua con GitHub Actions: compilación, tests y lint en cada push y pull request.

### Cambiado

- Arquitectura por capas (dominio en Kotlin puro, datos y UI con ViewModels y `StateFlow`) con inyección de dependencias manual.
- Navegación con Navigation Compose y rutas tipadas, en lugar de fragments que se ocultaban y mostraban a mano.
- Toolchain actualizada: Gradle 9.8.1, Android Gradle Plugin 9.4.1, Kotlin 2.4.21, `compileSdk` 37, `targetSdk` 36. El `minSdk` sube de 24 a 26: la app necesita Android 8.0 o superior.
- Todos los textos están en `strings.xml` y los tamaños de letra en `sp`.

### Corregido

- Cierre de la app al tocar una pestaña después de recrear la Activity (cambio de tema o de tamaño de letra, o muerte del proceso), y fragments duplicados.
- Pila de navegación que crecía con cada cambio de pestaña.
- Cierre de la app al calcular con campos vacíos o con valores que daban un TDEE nulo o negativo.
- La edad se pedía pero no se usaba en ningún cálculo.
- El objetivo a −15 % se mostraba como «Déficit intenso» por la acumulación de errores de coma flotante.
- El reparto de macros de Mantenimiento sumaba el 105 %.
- Editar los macros cambiaba el TDEE de referencia.
- La compilación fallaba en cualquier clon sin `local.properties`.

### Eliminado

- La interfaz anterior en Java + XML (Activity, fragments y layouts).
- La integración con la API de OpenAI y la dependencia de OkHttp: Kai-Q ya no necesita ninguna clave ni servidor externo.
- Ficheros sin uso en la raíz del repositorio y los iconos `.webp` que con `minSdk` 26 no se usaban.

## [1.0.0]

Versión original del Trabajo de Fin de Grado, escrita en Java + XML, con calculadora de TDEE y de grasa corporal y un asistente que usaba la API de OpenAI. Se conserva en la rama [`backup/original-java`](https://github.com/samartined/KaiFit-Pal/tree/backup/original-java) (tag local `v1.0.0`).
