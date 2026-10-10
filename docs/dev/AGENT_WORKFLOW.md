# Flujo de trabajo multiagente

Cómo se ejecutó la modernización 2.0.0 (octubre de 2026) y cómo reutilizar el mismo esquema en trabajo futuro. El plan técnico está en [`MODERNIZATION_PLAN.md`](MODERNIZATION_PLAN.md); el estado al cierre, en [`../../HANDOFF.md`](../../HANDOFF.md).

## Requisitos del operador

El operador (autor de la app) pidió expresamente que el trabajo grande lo ejecute una arquitectura coordinada de agentes:

- **Implementador**: Claude Opus 5.5 con esfuerzo `xhigh`. Es el único que modifica código.
- **Coordinador**: un agente Opus que media entre todos los demás y decide.
- **Resto de roles**: agentes Opus con la arquitectura que mejor encaje.
- **Asesor experto**: Claude Fable. No implementa ni trabaja: solo razona y asesora. Se recurre a él como último recurso y **siempre avisando antes al operador humano**.

## Mecanismo

- Los subagentes no pueden lanzar otros agentes, así que un "coordinador subagente" no puede orquestar. Se usa la herramienta **Workflow** de Claude Code: un guion JavaScript determinista que lanza agentes con modelo y esfuerzo propios, salidas estructuradas (JSON Schema), bucles y paralelismo, y que se puede **reanudar** con `resumeFromRunId` reutilizando los agentes ya terminados.
- **Una ejecución del workflow por fase.** Entre fases, la sesión principal revisa el resultado, actualiza el estado y lanza la siguiente.
- El guion es el plano de control; las decisiones técnicas las toma el agente coordinador y el guion ejecuta sus órdenes.
- Guion: [`agent-workflow/kaifit-phase.js`](agent-workflow/kaifit-phase.js).

## Roles

| Rol | Modelo y esfuerzo | Hace | No hace |
|---|---|---|---|
| Coordinador | Opus, `high` | Redacta la orden de trabajo de la fase; después consolida QA y revisiones, media entre revisor e implementador y decide `ACCEPT`, `REQUEST_FIX`, `ESCALATE_ADVISOR` o `ESCALATE_OPERATOR`. Mantiene `STATE.md` | No toca código |
| Implementador | Opus, `xhigh` | Ejecuta la orden, corre las comprobaciones, hace commits pequeños, escribe su informe | No hace push ni amplía el alcance |
| QA/Build | Opus, `medium` | Prepara el entorno (SDK) y verifica de forma independiente: build desde limpio, tests, lint, capturas, clon limpio, cada commit por separado, mutaciones | No corrige código |
| Revisores ×3 | Opus, `high` | Lentes independientes en paralelo: corrección, UI y plataforma, conformidad con la orden y el plan | Solo lectura |
| Investigador | Opus, `medium` | En las fases 1 y 5: hechos verificados (toolchain en un proyecto desechable, firmas reales de LiteRT-LM con `javap`) | No implementa |
| Documentador | Opus, `medium` | Fase 6: README, CHANGELOG y docs | No toca código fuente |
| Publicador | Opus, `low` | Tras `ACCEPT`: push de la rama de trabajo con reintentos de red | Nada más |
| Asesor experto | Fable, esfuerzo según la consulta | Asesoría escrita cuando el coordinador escala; revisión final independiente | No edita, no compila, no ejecuta |

## Protocolo de una fase

```
[Investigador]  (solo si la fase lo pide)   -> facts/fase-N.md
[Coordinador]   orden de trabajo            -> work-orders/fase-N.md
[Implementador] implementa + commits        -> reports/fase-N-impl-rK.md
   en paralelo, solo lectura:
   [QA/Build]  PASS/FAIL estructurado        -> reports/fase-N-qa-rK.md
   [Revisores] hallazgos con severidad       -> reports/fase-N-review-<lente>-rK.md
[Coordinador]   ACCEPT | REQUEST_FIX | ESCALATE_ADVISOR | ESCALATE_OPERATOR
   REQUEST_FIX -> implementador corrige con commits nuevos -> QA + revisores señalados -> coordinador
   ACCEPT      -> [Publicador] push de la rama de trabajo
   ESCALATE_*  -> el workflow termina y devuelve la pregunta a la sesión principal
```

Reglas que impone el guion, sin depender del criterio de ningún agente:

- Un solo implementador activo; QA y revisores son de solo lectura y corren en paralelo.
- `ESCALATE_ADVISOR` solo se acepta desde la ronda 2, si la decisión queda fuera del plan (`outsidePlan`) o ante un bloqueo de toolchain (`toolchainBlock`). Si no, se convierte en corrección. Tras 4 rondas sin aceptar, escala solo.
- Al escalar, la sesión principal avisa al operador, consulta al asesor y reanuda el mismo run pasando la asesoría en `args.advice` (o la respuesta del operador en `args.operatorAnswers`).
- Solo el publicador hace push, y solo tras `ACCEPT`. Nunca a `main`, nunca force-push, nunca tags.
- Las decisiones que corresponden al operador (producto, textos, privacidad, alcance) **no se aplican por defecto ni se le preguntan durante las fases**: se anotan con contexto, opciones y propuesta, y se le presentan todas juntas al final.

## Directorio de coordinación

Vive fuera del repositorio (en la sesión original, en el scratchpad, que es temporal) y se pasa en `args.coord`:

| Ruta | Contenido |
|---|---|
| `PLAN.md` | Plan aprobado, fuente de verdad |
| `STATE.md` | Fase actual, decisiones por fase, pendientes, decisiones del operador, historial de push |
| `facts/` | Hechos verificados: `entorno.md`, `entorno-sdk.md`, investigaciones |
| `work-orders/fase-N.md` | Orden de trabajo de cada fase |
| `reports/` | Informes de implementador, QA, revisores y decisiones, por ronda |
| `artifacts/fase-N/` | Logs, APK, capturas, mutaciones |
| `env.sh`, `gradle-init/` | Entorno del SDK y espejo de Maven Central |

Lo que debe sobrevivir a la sesión ya está en el repositorio: los hechos de investigación en [`reference/`](reference/), el espejo en [`env/`](env/), las decisiones en [`DECISIONS.md`](DECISIONS.md) y el estado en `HANDOFF.md`.

## Cómo reutilizarlo

1. Crea el directorio de coordinación con `PLAN.md`, un `STATE.md` inicial y `facts/entorno.md` con los hechos verificados del entorno.
2. Sustituye `PHASES` en el guion por las fases nuevas: nombre, si prepara entorno o investiga, lentes de revisión, notas de la fase y criterios de QA.
3. Lanza cada fase con la herramienta Workflow: `scriptPath` al guion y `args = {phase, coord, trailer, repo?, branch?}`. `trailer` son las líneas de atribución que exige tu sesión al final de cada commit.
4. Si un run se corta, relánzalo con `resumeFromRunId`. Si el implementador dejó trabajo sin commitear, pásale un `resumeNote` que lo describa y guarda antes una copia (`git diff` + tar de los no versionados).
5. Entre fases, lee el resultado, comprueba el remoto y las capturas, y actualiza `STATE.md` con lo que deban saber las órdenes siguientes.

## Lo aprendido en la sesión

| Fase | Duración aprox. | Rondas | Tokens de subagentes aprox. |
|---|---|---|---|
| 0 Higiene y ramas | 8 min | 1 | 0,5 M |
| 1 Toolchain y esqueleto | 75 min | 2 | 1,9 M |
| 2 Dominio | 30 min | 1 | 1,0 M |
| 3 Datos | 50 min | 2 | 1,4 M |
| 4 Pantallas | 95 min + reinicio | 2 | 2,2 M + el run interrumpido |
| 5 Asistente local | 135 min | 2 | 2,8 M |
| 6 Pulido, docs, CI | 135 min | 2 | 2,3 M |

- **El rigor de QA domina el tiempo.** Compilar cada commit por separado, builds sin caché y mutaciones dan mucha confianza, pero cada build de Android tarda minutos. Para fases pequeñas, basta con el build final y las mutaciones clave.
- **Los criterios de la orden deben ser exactos.** Dos fallos de QA vinieron de la propia orden (longitud de un asunto copiado literal, lista de ABIs esperada). Medir asuntos y valores al redactar la orden.
- **El contenedor puede reiniciarse.** Se perdió el run de la Fase 4 a mitad. Se recuperó reanudando el run (el coordinador quedó en caché) y pasando al implementador un `resumeNote` con el trabajo sin commitear.
- **El paralelismo real es limitado** por el número de CPU: los revisores a veces arrancan en serie.
- **Entorno de la nube**: el proxy rechaza el push de tags (403, no reintentar); Maven Central responde 429 a ratos (usar el espejo de `env/` con `-I`); la herramienta de envío de ficheros al operador limita a 30 MB (el APK completo pesa 61 MB: se envió una copia solo arm64 comprimida).
- **El hook de parada avisa de commits sin push** mientras una fase está en revisión. Es esperado: el push lo hace el publicador tras `ACCEPT`.
- **Revisión final con el asesor**: al terminar todas las fases, el operador pidió una verificación independiente de Fable con esfuerzo `xhigh`. Tardó unos 14 minutos y unos 0,4 M tokens. Informe completo en [`agent-workflow/final-advisor-review.md`](agent-workflow/final-advisor-review.md); estado de los hallazgos en `HANDOFF.md`.
- **Hace falta una lente que cuestione la orden.** Los dos hallazgos «mayor» del asesor se escaparon a todas las rondas por motivos distintos:
  - el error de cierre nativo venía del ejemplo de la investigación, y los revisores comprobaron la conformidad con la orden, no con la semántica de la librería;
  - el de `onTrimMemory` venía de suponer el comportamiento clásico de Android sin contrastarlo con Android 14+.

  En trabajo futuro, al menos un revisor debe preguntarse si la orden misma es correcta frente a la documentación y el código fuente reales.
