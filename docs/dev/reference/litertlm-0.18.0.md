<!--
Referencia verificada de la API de LiteRT-LM 0.18.0 y del modelo Gemma 4 E2B.
Origen: investigación de la Fase 5 (2026-10-10) con `javap -public` sobre el AAR de Google Maven,
el código fuente del tag v0.18.0 y la app AI Edge Gallery. Las rutas `coord/sandbox/...` que se citan
eran de un directorio temporal de la sesión que ya no existe: para repetir la inspección, descarga
de nuevo el AAR de https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/0.18.0/
Si se actualiza LiteRT-LM, vuelve a verificar estas firmas antes de tocar `data/llm/LiteRtLmRepository.kt`.
-->

# Fase 5 - Hechos verificados: LiteRT-LM 0.18.0 + Gemma 4 E2B (2026-10-10)

Fuentes inspeccionadas (todo en `coord/sandbox/`):
- AAR y POM de Google Maven: `sandbox/litertlm/litertlm-android-0.18.0.{aar,pom}` (0.18.0 es la ultima version publicada; metadata lastUpdated 2026-10-06).
- `javap -public` de TODAS las clases: `sandbox/litertlm/javap-public-full.txt` (1157 lineas). Extracto literal mas abajo.
- Fuentes Kotlin del tag `v0.18.0` (commit b2f686e2): `sandbox/src/litertlm-src/kotlin/java/com/google/ai/edge/litertlm/` (Engine.kt, Conversation.kt, Config.kt, Message.kt, jni/*.cc). Los nombres de parametros Kotlin y los valores por defecto citados salen de ahi y coinciden con el bytecode.
- AI Edge Gallery (HEAD 6f572e32, 2026-10-09, usa litertlm 0.18.0): `sandbox/src/gallery/Android/src/app/.../ui/llmchat/LlmChatModelHelper.kt`, `model_allowlists/1_0_20.json`, `worker/DownloadWorker.kt`.
- Release notes (pagina de releases): 0.18.0 sin cambios rompedores en la API Kotlin; aporta EmbeddingGemma 2, `ModelInfo`, poda de mascara de atencion en GPU. 0.17.0 anadio MTP (speculative decoding) y contexto extendido para Gemma 4. Ninguna nota sobre cancelacion ni "known issues".

## 1. API publica real (resumen en Kotlin, verificado con fuentes + javap)

```kotlin
// Config.kt
data class EngineConfig @JvmOverloads constructor(
  val modelPath: String,
  val backend: Backend = Backend.CPU(),
  val visionBackend: Backend? = null,      // null = no carga encoder de vision (ahorra memoria)
  val audioBackend: Backend? = null,       // null = no carga audio
  val maxNumTokens: Int? = null,           // tamano de contexto/KV cache; null = default del modelo; >0
  val maxNumImages: Int? = null,
  val cacheDir: String? = null,            // dir escribible; acelera la 2a carga (context.cacheDir.path)
  val activationDataType: ActivationDataType? = null,
)
sealed class Backend(val name: String) {
  data class CPU(val threadCount: Int? = null, @Deprecated val numOfThreads: Int? = null) : Backend("CPU")
  class GPU : Backend("GPU")                 // OJO: clase, no object -> Backend.GPU()
  data class NPU(val nativeLibraryDir: String = "") : Backend("NPU")
  class GOOGLE_TENSOR : Backend("GOOGLE_TENSOR_ARTISAN")
}
data class ConversationConfig @JvmOverloads constructor(
  val systemInstruction: Contents? = null,
  val initialMessages: List<Message> = listOf(),
  val tools: List<ToolProvider> = listOf(),
  val samplerConfig: SamplerConfig? = null,     // null = sampler por defecto del modelo
  val automaticToolCalling: Boolean = true,
  val channels: List<Channel>? = null,
  val extraContext: Map<String, Any> = emptyMap(),
  val loraConfig: LoraConfig? = null,
  val prefillPrefaceOnInit: Boolean = false,
  val maxOutputToken: Int? = null,              // >0
  val thinkingConfig: ThinkingConfig? = null,
  val enableResponseFormat: Boolean = false,
  val enableSpeculativeDecoding: Boolean? = null,
  val chatTemplate: String? = null,
)
data class SamplerConfig(val topK: Int, val topP: Double, val temperature: Double, val seed: Int = 0)
// require: topK > 0, 0 <= topP <= 1, temperature >= 0. SIN @JvmOverloads (seed tiene default Kotlin).
data class ThinkingConfig @JvmOverloads constructor(val enableThinking: Boolean = true, val thinkingTokenBudget: Int = -1)

// Engine.kt
class Engine(val engineConfig: EngineConfig) : AutoCloseable {
  fun isInitialized(): Boolean
  fun initialize()                     // BLOQUEANTE (no suspend), ~segundos; IllegalStateException si ya inicializado
  override fun close()                 // IllegalStateException si NO esta inicializado
  fun createConversation(conversationConfig: ConversationConfig = ConversationConfig()): Conversation  // ISE si no inicializado
  fun createSession(sessionConfig: SessionConfig = SessionConfig()): Session   // API de bajo nivel, no usar
  companion object { fun setNativeMinLogSeverity(level: LogSeverity) }
}

// Conversation.kt
class Conversation(...) : AutoCloseable {     // se obtiene solo via engine.createConversation()
  val isAlive: Boolean
  fun sendMessage(text|contents|message, extraContext = emptyMap(), ...): Message           // BLOQUEANTE
  fun sendMessageAsync(text|contents|message, callback: MessageCallback, extraContext = emptyMap(), ...)  // NO bloquea; callbacks en hilo nativo
  fun sendMessageAsync(text|contents|message, extraContext = emptyMap(), ...): Flow<Message>  // NO suspend; Flow frio (callbackFlow)
  fun cancelProcess()                  // cancela la generacion en curso; no-op si no hay; ISE si conversacion cerrada
  fun getTokenCount(): Int             // tokens usados en el KV de la conversacion (no experimental)
  override fun close()                 // ISE si ya estaba cerrada
}
interface MessageCallback { fun onMessage(message: Message); fun onDone(); fun onError(throwable: Throwable) }

// Message.kt
class Contents { val contents: List<Content>; override fun toString() = contents.joinToString("")
  companion object { fun of(text: String); fun of(vararg contents: Content); fun of(contents: List<Content>) } }
sealed class Content { data class Text(val text: String); ImageBytes; ImageFile; AudioBytes; AudioFile; ToolResponse }
class Message(role: Role, contents: Contents, toolCalls: List<ToolCall> = ..., channels: Map<String,String> = ...) {
  override fun toString() = contents.toString()
  companion object { system(..); user(..); model(..); tool(..); of(..) } }
enum class Role { SYSTEM, USER, MODEL, TOOL }

// ExperimentalFlags.kt  -> @ExperimentalApi (RequiresOptIn level ERROR): object mutable GLOBAL
object ExperimentalFlags { enableBenchmark; enableSpeculativeDecoding: Boolean?; enableConversationConstrainedDecoding;
  convertCamelToSnakeCaseInToolDescription; filterChannelContentFromKvCache: Boolean?; overwritePromptTemplate (deprecated); visualTokenBudget: Int? }

class LiteRtLmJniException(message: String) : RuntimeException(message)   // "Status Code: N. Message: ..."
class ToolException(message: String, cause: Throwable? = null) : RuntimeException
```

Nombres que NO existen (no inventar): no hay `Engine.create`, ni `Backend.GPU` como object, ni `SamplerConfig` con defaults en topK/topP/temperature, ni `Conversation.stop()`/`cancel()`, ni `suspend` en ninguna API. El metodo `Contents.empty()` es `internal`.

### Extracto literal de `javap -public` (sin `componentN`, `copy$default`, `equals`, `hashCode`; Conversation filtrado a los overloads relevantes)

```
Compiled from "Engine.kt"
public final class com.google.ai.edge.litertlm.Engine implements java.lang.AutoCloseable {
  public static final com.google.ai.edge.litertlm.Engine$Companion Companion;
  public com.google.ai.edge.litertlm.Engine(com.google.ai.edge.litertlm.EngineConfig);
  public final com.google.ai.edge.litertlm.EngineConfig getEngineConfig();
  public final boolean isInitialized();
  public final void initialize();
  public void close();
  public final com.google.ai.edge.litertlm.Conversation createConversation(com.google.ai.edge.litertlm.ConversationConfig);
  public final com.google.ai.edge.litertlm.Session createSession(com.google.ai.edge.litertlm.SessionConfig);
}
Compiled from "Config.kt"
public final class com.google.ai.edge.litertlm.EngineConfig {
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, java.lang.Integer, java.lang.Integer, java.lang.String, com.google.ai.edge.litertlm.ActivationDataType);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, java.lang.Integer, java.lang.Integer, java.lang.String, com.google.ai.edge.litertlm.ActivationDataType, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public final java.lang.String getModelPath();
  public final com.google.ai.edge.litertlm.Backend getBackend();
  public final com.google.ai.edge.litertlm.Backend getVisionBackend();
  public final com.google.ai.edge.litertlm.Backend getAudioBackend();
  public final java.lang.Integer getMaxNumTokens();
  public final java.lang.Integer getMaxNumImages();
  public final java.lang.String getCacheDir();
  public final com.google.ai.edge.litertlm.ActivationDataType getActivationDataType();
  public final com.google.ai.edge.litertlm.EngineConfig copy(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, java.lang.Integer, java.lang.Integer, java.lang.String, com.google.ai.edge.litertlm.ActivationDataType);
  public java.lang.String toString();
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, java.lang.Integer, java.lang.Integer, java.lang.String);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, java.lang.Integer, java.lang.Integer);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, java.lang.Integer);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend, com.google.ai.edge.litertlm.Backend);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String, com.google.ai.edge.litertlm.Backend);
  public com.google.ai.edge.litertlm.EngineConfig(java.lang.String);
}
Compiled from "Config.kt"
public final class com.google.ai.edge.litertlm.Backend$CPU extends com.google.ai.edge.litertlm.Backend {
  public com.google.ai.edge.litertlm.Backend$CPU(java.lang.Integer, java.lang.Integer);
  public com.google.ai.edge.litertlm.Backend$CPU(java.lang.Integer, java.lang.Integer, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public final java.lang.Integer getThreadCount();
  public final java.lang.Integer getNumOfThreads();
  public static void getNumOfThreads$annotations();
  public final com.google.ai.edge.litertlm.Backend$CPU copy(java.lang.Integer, java.lang.Integer);
  public java.lang.String toString();
  public com.google.ai.edge.litertlm.Backend$CPU();
}
Compiled from "Config.kt"
public final class com.google.ai.edge.litertlm.Backend$GPU extends com.google.ai.edge.litertlm.Backend {
  public com.google.ai.edge.litertlm.Backend$GPU();
}
Compiled from "Config.kt"
public final class com.google.ai.edge.litertlm.ConversationConfig {
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer, com.google.ai.edge.litertlm.ThinkingConfig, boolean, java.lang.Boolean, java.lang.String);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List, java.util.List, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List, java.util.Map, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer, com.google.ai.edge.litertlm.ThinkingConfig, boolean, java.lang.Boolean, java.lang.String, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public final com.google.ai.edge.litertlm.Contents getSystemInstruction();
  public final java.util.List<com.google.ai.edge.litertlm.Message> getInitialMessages();
  public final java.util.List<com.google.ai.edge.litertlm.ToolProvider> getTools();
  public final com.google.ai.edge.litertlm.SamplerConfig getSamplerConfig();
  public final boolean getAutomaticToolCalling();
  public final java.util.List<com.google.ai.edge.litertlm.Channel> getChannels();
  public final java.util.Map<java.lang.String, java.lang.Object> getExtraContext();
  public final com.google.ai.edge.litertlm.LoraConfig getLoraConfig();
  public final boolean getPrefillPrefaceOnInit();
  public final java.lang.Integer getMaxOutputToken();
  public final com.google.ai.edge.litertlm.ThinkingConfig getThinkingConfig();
  public final boolean getEnableResponseFormat();
  public final java.lang.Boolean getEnableSpeculativeDecoding();
  public final java.lang.String getChatTemplate();
  public final com.google.ai.edge.litertlm.ConversationConfig copy(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer, com.google.ai.edge.litertlm.ThinkingConfig, boolean, java.lang.Boolean, java.lang.String);
  public java.lang.String toString();
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer, com.google.ai.edge.litertlm.ThinkingConfig, boolean, java.lang.Boolean);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer, com.google.ai.edge.litertlm.ThinkingConfig, boolean);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer, com.google.ai.edge.litertlm.ThinkingConfig);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean, java.lang.Integer);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig, boolean);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>, com.google.ai.edge.litertlm.LoraConfig);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>, java.util.Map<java.lang.String, ? extends java.lang.Object>);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean, java.util.List<com.google.ai.edge.litertlm.Channel>);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig, boolean);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>, com.google.ai.edge.litertlm.SamplerConfig);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>, java.util.List<? extends com.google.ai.edge.litertlm.ToolProvider>);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.Message>);
  public com.google.ai.edge.litertlm.ConversationConfig(com.google.ai.edge.litertlm.Contents);
  public com.google.ai.edge.litertlm.ConversationConfig();
}
Compiled from "Config.kt"
public final class com.google.ai.edge.litertlm.SamplerConfig {
  public com.google.ai.edge.litertlm.SamplerConfig(int, double, double, int);
  public com.google.ai.edge.litertlm.SamplerConfig(int, double, double, int, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public final int getTopK();
  public final double getTopP();
  public final double getTemperature();
  public final int getSeed();
  public final com.google.ai.edge.litertlm.SamplerConfig copy(int, double, double, int);
  public java.lang.String toString();
}
Compiled from "Conversation.kt"
public interface com.google.ai.edge.litertlm.MessageCallback {
  public abstract void onMessage(com.google.ai.edge.litertlm.Message);
  public abstract void onDone();
  public abstract void onError(java.lang.Throwable);
}
Compiled from "LiteRtLmJniException.kt"
public final class com.google.ai.edge.litertlm.LiteRtLmJniException extends java.lang.RuntimeException {
  public com.google.ai.edge.litertlm.LiteRtLmJniException(java.lang.String);
}
public final class com.google.ai.edge.litertlm.Conversation implements java.lang.AutoCloseable {
  public final boolean isAlive();
  public final void cancelProcess();
  public final int getTokenCount();
  public void close();
  public final void sendMessageAsync(com.google.ai.edge.litertlm.Contents, com.google.ai.edge.litertlm.MessageCallback, java.util.Map<java.lang.String, ? extends java.lang.Object>);
  public final void sendMessageAsync(com.google.ai.edge.litertlm.Contents, com.google.ai.edge.litertlm.MessageCallback);
  public final kotlinx.coroutines.flow.Flow<com.google.ai.edge.litertlm.Message> sendMessageAsync(com.google.ai.edge.litertlm.Contents, java.util.Map<java.lang.String, ? extends java.lang.Object>);
  public final kotlinx.coroutines.flow.Flow<com.google.ai.edge.litertlm.Message> sendMessageAsync(com.google.ai.edge.litertlm.Contents);
  public final kotlinx.coroutines.flow.Flow<com.google.ai.edge.litertlm.Message> sendMessageAsync(java.lang.String, java.util.Map<java.lang.String, ? extends java.lang.Object>);
  public final kotlinx.coroutines.flow.Flow<com.google.ai.edge.litertlm.Message> sendMessageAsync(java.lang.String);
}
Compiled from "Message.kt"
public final class com.google.ai.edge.litertlm.Contents$Companion {
  public final com.google.ai.edge.litertlm.Contents empty$third_party_odml_litert_lm_kotlin_java_com_google_ai_edge_litertlm_litertlm_android();
  public final com.google.ai.edge.litertlm.Contents of(java.lang.String);
  public final com.google.ai.edge.litertlm.Contents of(com.google.ai.edge.litertlm.Content...);
  public final com.google.ai.edge.litertlm.Contents of(java.util.List<? extends com.google.ai.edge.litertlm.Content>);
  public com.google.ai.edge.litertlm.Contents$Companion(kotlin.jvm.internal.DefaultConstructorMarker);
}
Compiled from "Message.kt"
public final class com.google.ai.edge.litertlm.Message$Companion {
  public final com.google.ai.edge.litertlm.Message system(java.lang.String);
  public final com.google.ai.edge.litertlm.Message system(com.google.ai.edge.litertlm.Contents);
  public final com.google.ai.edge.litertlm.Message user(java.lang.String);
  public final com.google.ai.edge.litertlm.Message user(com.google.ai.edge.litertlm.Contents);
  public final com.google.ai.edge.litertlm.Message model(java.lang.String);
  public final com.google.ai.edge.litertlm.Message model(com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.ToolCall>, java.util.Map<java.lang.String, java.lang.String>);
  public final com.google.ai.edge.litertlm.Message tool(com.google.ai.edge.litertlm.Contents);
  public final com.google.ai.edge.litertlm.Message of(java.lang.String);
  public final com.google.ai.edge.litertlm.Message of(com.google.ai.edge.litertlm.Content...);
  public final com.google.ai.edge.litertlm.Message of(java.util.List<? extends com.google.ai.edge.litertlm.Content>);
  public com.google.ai.edge.litertlm.Message$Companion(kotlin.jvm.internal.DefaultConstructorMarker);
}
Compiled from "Message.kt"
public final class com.google.ai.edge.litertlm.Message {
  public static final com.google.ai.edge.litertlm.Message$Companion Companion;
  public com.google.ai.edge.litertlm.Message(com.google.ai.edge.litertlm.Role, com.google.ai.edge.litertlm.Contents, java.util.List<com.google.ai.edge.litertlm.ToolCall>, java.util.Map<java.lang.String, java.lang.String>);
  public com.google.ai.edge.litertlm.Message(com.google.ai.edge.litertlm.Role, com.google.ai.edge.litertlm.Contents, java.util.List, java.util.Map, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public final com.google.ai.edge.litertlm.Role getRole();
  public final com.google.ai.edge.litertlm.Contents getContents();
  public final java.util.List<com.google.ai.edge.litertlm.ToolCall> getToolCalls();
  public final java.util.Map<java.lang.String, java.lang.String> getChannels();
  public java.lang.String toString();
}
Compiled from "Message.kt"
public final class com.google.ai.edge.litertlm.Content$Text extends com.google.ai.edge.litertlm.Content {
  public com.google.ai.edge.litertlm.Content$Text(java.lang.String);
  public final java.lang.String getText();
  public java.lang.String toString();
  public final com.google.ai.edge.litertlm.Content$Text copy(java.lang.String);
}
Compiled from "ExperimentalFlags.kt"
public final class com.google.ai.edge.litertlm.ExperimentalFlags {
  public static final com.google.ai.edge.litertlm.ExperimentalFlags INSTANCE;
  public final boolean getEnableBenchmark();
  public final void setEnableBenchmark(boolean);
  public final java.lang.Boolean getEnableSpeculativeDecoding();
  public final void setEnableSpeculativeDecoding(java.lang.Boolean);
  public final boolean getEnableConversationConstrainedDecoding();
  public final void setEnableConversationConstrainedDecoding(boolean);
  public final boolean getConvertCamelToSnakeCaseInToolDescription();
  public final void setConvertCamelToSnakeCaseInToolDescription(boolean);
  public final java.lang.Boolean getFilterChannelContentFromKvCache();
  public final void setFilterChannelContentFromKvCache(java.lang.Boolean);
  public final java.lang.String getOverwritePromptTemplate();
  public final void setOverwritePromptTemplate(java.lang.String);
  public static void getOverwritePromptTemplate$annotations();
  public final java.lang.Integer getVisualTokenBudget();
  public final void setVisualTokenBudget(java.lang.Integer);
}
Compiled from "Config.kt"
public final class com.google.ai.edge.litertlm.Backend$NPU extends com.google.ai.edge.litertlm.Backend {
  public com.google.ai.edge.litertlm.Backend$NPU(java.lang.String);
  public com.google.ai.edge.litertlm.Backend$NPU(java.lang.String, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public final java.lang.String getNativeLibraryDir();
  public final com.google.ai.edge.litertlm.Backend$NPU copy(java.lang.String);
  public java.lang.String toString();
  public com.google.ai.edge.litertlm.Backend$NPU();
}
Compiled from "Config.kt"
public abstract class com.google.ai.edge.litertlm.Backend {
  public final java.lang.String getName();
  public com.google.ai.edge.litertlm.Backend(java.lang.String, kotlin.jvm.internal.DefaultConstructorMarker);
}
```
Listado completo literal: `sandbox/litertlm/javap-public-full.txt`.

## 2. Semantica clave (leida del codigo fuente 0.18.0)

- **Hilos**: `Engine.initialize()` es sincrono y pesado (doc: "hasta ~10 s") -> llamarlo en `Dispatchers.IO`. `createConversation` tambien hace trabajo nativo (llamar fuera de Main). `sendMessageAsync(...)` devuelve enseguida; los callbacks llegan en un hilo nativo del motor. Nada es `suspend`.
- **Streaming**: cada `onMessage` / elemento del Flow es un **delta** (trozo nuevo), no el acumulado: Gallery hace `content + partial`. Texto: `message.toString()`. Si se activa "thinking", el razonamiento llega en `message.channels["thought"]` y `toString()` solo da el contenido.
- **Flow de la libreria**: implementado como `callbackFlow { sendMessageAsync(msg, callback); awaitClose {} }` con `trySend` sobre el buffer por defecto (64).
  - Consecuencia 1: **cancelar el Job del colector NO detiene la generacion nativa** (`awaitClose {}` esta vacio). El motor sigue generando y la conversacion sigue ocupada. Hay que llamar `conversation.cancelProcess()` explicitamente.
  - Consecuencia 2: si el colector se queda atras mas de 64 trozos, `trySend` descarta trozos en silencio.
  - Recomendacion: envolver la variante con callback en un `callbackFlow` propio con `awaitClose { if (!terminado) conversation.cancelProcess() }` y `.buffer(Channel.UNLIMITED)` (ver ejemplo).
- **Cancelacion**: `cancelProcess()` -> el hilo nativo termina y llama `onError(java.util.concurrent.CancellationException(msg))` (statusCode 1 = kCancelled). Con el Flow de la libreria eso se convierte en `close(CancellationException)` -> el colector ve una CancellationException (indistinguible de una cancelacion de corrutina). Tras cancelar, el estado del KV no se revierte (comentario b/450903294): el turno parcial queda en la conversacion; la conversacion sigue usable.
- **Errores**: fallos del motor llegan como `LiteRtLmJniException("Status Code: N. Message: ...")` en `onError` (o lanzados por `initialize`/`createConversation`/`sendMessage`). El mensaje puede traer una traza nativa: Gallery recorta desde `"=== Source Location Trace"`. Uso indebido de ciclo de vida -> `IllegalStateException` ("Engine is not initialized.", "Engine is already initialized.", "Conversation is not alive.", "Conversation is closed already.").
- **Que cerrar y en que orden**: `Conversation.close()` (antes) y luego `Engine.close()`. Ambos lanzan ISE si se cierran dos veces / sin inicializar -> proteger con flags o `runCatching`. Si `initialize()` falla, el `Engine` queda sin handle: NO llamar `close()` (lanzaria ISE); basta con descartarlo. Gallery, para "nueva conversacion": `cancelProcess()` si genera -> `conversation.close()` -> `engine.createConversation(cfg)` (el motor se reutiliza; recargarlo cuesta segundos).
- **Prompt / plantilla**: el `.litertlm` incluye la plantilla de chat Jinja de Gemma 4 (`<|turn>model ...`); el runtime la aplica. `systemInstruction = Contents.of(texto)` se envia como mensaje con rol SYSTEM. No hay que formatear tokens a mano ni anteponer el system al mensaje del usuario. El historial lo mantiene la propia `Conversation` (KV cache): enviar solo el mensaje nuevo en cada turno.
- **Thinking (Gemma 4)**: se controla con `extraContext = mapOf("enable_thinking" to true)` en `sendMessageAsync` (Gallery) o `ConversationConfig.thinkingConfig`. La plantilla solo lo activa si `enable_thinking` esta definido y es true -> por defecto desactivado. Recomendado: no activarlo (gasta tokens y latencia).
- **Contexto**: Gemma 4 E2B admite hasta 32K; Gallery usa `maxNumTokens = 4000` (allowlist) y `maxOutputToken` no lo fija. `maxNumTokens` del EngineConfig es el tamano total del contexto (KV cache, memoria). `getTokenCount()` permite saber cuanto lleva la conversacion -> reiniciar/resumir al acercarse al limite. `ModelInfo.from(path).use { it.maxContextTokens() }` lee el maximo del fichero sin cargar el motor (requiere nativo).
- **NPU/sampler**: Gallery pasa `samplerConfig = null` solo para NPU. Para CPU/GPU pasa `SamplerConfig(topK, topP, temperature)`; defaults del modelo en Gallery: topK 64, topP 0.95, temperature 1.0. El plan (topK 40, topP 0.95, temperature 0.3) es valido para respuestas mas deterministas.

## 3. Contenido del AAR

| Aspecto | Valor |
|---|---|
| minSdk manifiesto AAR | 24 (la app usa 26: OK) |
| Entradas de manifiesto | ninguna (solo `<uses-sdk minSdkVersion=24>` y `<application/>` vacio). NO aporta `uses-native-library`: la app debe declararlas |
| ABIs | `arm64-v8a` (liblitertlm_jni.so 22.155.680 B) y `x86_64` (26.353.160 B). Sin armeabi-v7a ni x86 |
| Alineacion | segmentos LOAD a 0x4000 (16 KB): compatible con dispositivos de paginas de 16 KB |
| NEEDED | libandroid, libz, libGLESv2, libEGL, libGLESv3, liblog, libdl, libm, libc. OpenCL/vndksupport se cargan dinamicamente (por eso `uses-native-library required=false`) |
| Reglas consumer ProGuard | **NINGUNA** (no hay proguard.txt en el AAR ni META-INF/proguard en classes.jar) |
| classes.jar | 191 KB, paquete `com.google.ai.edge.litertlm` |
| Dependencias transitivas (POM, scope compile) | `com.google.code.gson:gson:2.14.0`, `org.jetbrains.kotlin:kotlin-reflect:2.4.0`, `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0` |

- ProGuard/R8: el JNI busca por nombre clases/metodos Kotlin (`LiteRtLmJniException`, `BenchmarkInfo`, `InputData$*`, `getTopK/getTopP/getTemperature/getSeed` de SamplerConfig, `getEnableThinking/getThinkingTokenBudget`, `onMessage/onDone/onError` de `LiteRtLmJni$JniMessageCallback`...) y usa Gson/reflexion. La app tiene `isMinifyEnabled = true` en release -> **anadir a `app/proguard-rules.pro`**:
  ```
  -keep class com.google.ai.edge.litertlm.** { *; }
  -keep interface com.google.ai.edge.litertlm.** { *; }
  -dontwarn com.google.ai.edge.litertlm.**
  ```
- Manifiesto de la app: ya contiene `uses-native-library` `libOpenCL.so` y `libvndksupport.so` con `required=false` (exactamente lo que pide la guia oficial para GPU). Gallery anade ademas `libcdsprpc.so` y libs `libedgetpu_*` (solo NPU: no necesarios).
- Tamano APK: +~22 MB por arm64 (+26 MB si se incluye x86_64). Opcional: `ndk { abiFilters += listOf("arm64-v8a", "x86_64") }` no reduce nada (solo hay esas dos); para release podria limitarse a arm64-v8a. Gallery anade `packaging { jniLibs { pickFirsts.add("**/liblitertlm_jni.so") } }` solo porque mezcla varias libs: no necesario aqui.
- `kotlin-reflect` 2.4.0 entra como transitiva (~3 MB antes de R8); compatible con Kotlin 2.4.21 de la app.

## 4. Tests JVM / Robolectric (probado)

Prueba en JVM pura (JDK 21, classes.jar + kotlin-stdlib 2.4.21 + coroutines 1.11.0 + gson): `sandbox/litertlm/jvmtest/Probe.java`. Resultado:
```
EngineConfig OK: EngineConfig(modelPath=/nonexistent/model.litertlm, backend=CPU(threadCount=null, numOfThreads=null), ...)
Engine constructed, isInitialized=false
Contents: hola / Message: USER
SamplerConfig: SamplerConfig(topK=40, topP=0.95, temperature=0.3, seed=0)
initialize() threw java.lang.UnsatisfiedLinkError: Failed to load native library litertlm_jni. Tried system path, com/google/ai/edge/litertlm/jni/liblitertlm_jni.so, and com/google/ai/edge/litertlm/jni/linux-x86_64/liblitertlm_jni.so
2nd initialize() threw java.lang.NoClassDefFoundError: Could not initialize class com.google.ai.edge.litertlm.LiteRtLmJni
```
- Las clases Kotlin cargan y los data classes (EngineConfig, ConversationConfig, SamplerConfig, Contents, Message) se pueden construir en tests. Cualquier llamada nativa (`initialize`, `createConversation`, `ModelInfo.from`) lanza `UnsatisfiedLinkError` (un `Error`, no `Exception`) y luego `NoClassDefFoundError`. Robolectric no cambia esto: el `.so` x86_64 del AAR es para Android (bionic), no carga en el host.
- **Estrategia**: interfaz propia (p. ej. `LlmEngine`/`LlmRepository`) con implementacion `LiteRtLmEngine` (no testeada en unit tests) y `FakeLlmEngine` (Flow de deltas programable, errores y cancelacion simulados) para ViewModel/Compose tests. Mapear errores en la implementacion real: `LiteRtLmJniException`, `IllegalStateException`, y tambien `UnsatisfiedLinkError`/`NoClassDefFoundError` (catch `Throwable` acotado alrededor de initialize) -> estado "no compatible".
- Para tests de la clase real no hay alternativa sin dispositivo; queda en el checklist manual.

## 5. Modelo Gemma 4 E2B en Hugging Face (HEAD 2026-10-10)

Repo `litert-community/gemma-4-E2B-it-litert-lm`, `gated=false`, licencia apache-2.0, sha main `b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1` (lastModified 2026-08-31).

| Fichero | Tamano (bytes) | x-linked-etag (sha256 LFS) | Contenido (cabecera .litertlm) |
|---|---|---|---|
| `gemma-4-E2B-it.litertlm` | **2.588.147.712** | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` | generico: prefill/decode TFLite, embedder + per-layer embedder, drafter MTP (speculative decoding), encoders de vision y audio; funciona en CPU y GPU. Creado 2026-04-28 |
| `gemma-4-E2B-it-gpu.litertlm` | 2.008.432.640 | `a53a59001894c58e6bdb5b9b227709f91a2e3e556baa7d85acf9c55402ba5cf5` | una sola seccion `tf_lite_artisan_text_decoder` con `backend_constraint` = `gpu_artisan`: **solo texto, solo GPU, sin MTP**; no documentado en el model card; Gallery NO lo usa. No sirve para fallback a CPU |
| `gemma-4-E2B-it-web.litertlm` / `.task` | ~2,0 GB | | Web/MediaPipe |
| `_Google_Tensor_G5/G6`, `_qualcomm_sm8750/qcs8275`, `_intel_*` | 2,9-3,3 GB | | NPU por SoC |

- URL: `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm` -> 302 a `us.aws.cdn.hf.co/xet-bridge-us/...` (URL firmada, caduca). El CDN responde `accept-ranges: bytes` y 206 a peticiones Range (reanudable). El `ETag` del CDN (`"ee3c29ac..."`, hash xet) NO coincide con el `x-linked-etag` del 302 (sha256): para verificar, usar **tamano exacto 2.588.147.712** (y opcionalmente SHA-256 = x-linked-etag, ~10-30 s en movil).
- Para reproducibilidad se puede fijar la revision: `.../resolve/b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1/gemma-4-E2B-it.litertlm`.
- Gallery (allowlist 1_0_20) para Gemma-4-E2B-it: fichero generico, `minDeviceMemoryInGb: 8`, `accelerators: "gpu,cpu"` (GPU por defecto), `maxTokens: 4000`, `maxContextLength: 32000`, topK 64/topP 0.95/temp 1.0, capacidades `llm_thinking` y `speculative_decoding`.
- Rendimiento (model card, S26 Ultra): CPU 46,9 tok/s decode, TTFT 1,8 s, 1.733 MB RAM; GPU 52,1 tok/s, TTFT 0,3 s, 676 MB RAM. El texto usa memoria mapeada (~0,8 GB de pesos) y vision/audio se cargan bajo demanda (y no se cargan si `visionBackend/audioBackend = null`).
- Descarga en Gallery: WorkManager + foreground service `dataSync` + `HttpURLConnection` con cabecera `Range` sobre un `.tmp`, renombrado al final. El plan usa `DownloadManager` (sigue redirecciones HTTPS->HTTPS entre hosts y reanuda solo): valido; verificar tamano al terminar.

## 6. Ejemplo Kotlin minimo correcto para 0.18.0

```kotlin
import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LiteRtLmEngine(private val context: Context) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    /** Loads the model; tries GPU first and falls back to CPU. Returns the backend in use. */
    suspend fun initialize(modelPath: String, preferGpu: Boolean): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeLocked()
            val candidates = if (preferGpu) listOf(Backend.GPU(), Backend.CPU()) else listOf(Backend.CPU())
            var lastError: Throwable? = null
            for (backend in candidates) {
                val candidate = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = backend,
                        maxNumTokens = 4096,              // context window (KV cache)
                        cacheDir = context.cacheDir.path, // faster second load
                    )
                )
                try {
                    candidate.initialize()               // blocking, seconds
                    engine = candidate
                    return@withLock backend.name
                } catch (e: LiteRtLmJniException) {
                    lastError = e                        // e.g. no OpenCL -> try next backend
                } catch (e: UnsatisfiedLinkError) {
                    throw e                              // native lib unusable on this device
                }
                // A failed initialize() leaves no native handle: do NOT call close() here.
            }
            throw lastError ?: IllegalStateException("No backend available")
        }
    }

    /** Creates (or recreates) the conversation with the system prompt. */
    suspend fun newConversation(systemPrompt: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val e = checkNotNull(engine) { "Engine not initialized" }
            conversation?.let { c -> if (c.isAlive) { runCatching { c.cancelProcess() }; c.close() } }
            conversation = e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.3),
                    maxOutputToken = 1024,
                )
            )
        }
    }

    /** Streams text deltas. Cancelling the collector stops native generation. */
    fun send(text: String): Flow<String> = callbackFlow {
        val conv = checkNotNull(conversation) { "No conversation" }
        val finished = AtomicBoolean(false)
        conv.sendMessageAsync(
            Contents.of(text),
            object : MessageCallback {
                override fun onMessage(message: Message) { trySend(message.toString()) } // delta
                override fun onDone() { finished.set(true); close() }
                override fun onError(throwable: Throwable) {
                    finished.set(true)
                    // cancelProcess() ends with java.util.concurrent.CancellationException: treat as normal end
                    if (throwable is java.util.concurrent.CancellationException) close() else close(throwable)
                }
            },
        )
        awaitClose { if (!finished.get() && conv.isAlive) runCatching { conv.cancelProcess() } }
    }.buffer(Channel.UNLIMITED)   // never drop deltas if the UI is slow

    fun tokenCount(): Int = conversation?.takeIf { it.isAlive }?.getTokenCount() ?: 0

    /** Call from onCleared()/onTrimMemory(); conversation first, then engine. */
    suspend fun close() = withContext(Dispatchers.IO) { mutex.withLock { closeLocked() } }

    private fun closeLocked() {
        conversation?.let { c -> if (c.isAlive) { runCatching { c.cancelProcess() }; runCatching { c.close() } } }
        conversation = null
        engine?.let { e -> if (e.isInitialized()) runCatching { e.close() } }
        engine = null
    }
}

// ViewModel usage:
// private var generation: Job? = null
// generation = viewModelScope.launch {
//     engine.send(userText)
//         .catch { e -> showError(e) }                       // LiteRtLmJniException etc.
//         .collect { delta -> appendToLastMessage(delta) }   // accumulate deltas
// }
// Stop button: generation?.cancel()   -> awaitClose calls conversation.cancelProcess()
```
Notas del ejemplo: `send` no es suspend y no necesita `flowOn` (los callbacks vienen de un hilo nativo; el `trySend` es thread-safe). `onCleared()` no puede suspender: lanzar el cierre en un scope de aplicacion o hacer `runBlocking(Dispatchers.IO)` corto; lo importante es cerrar conversacion antes que motor y no cerrar nada dos veces.

## 7. Recomendaciones para la orden de trabajo

1. Mantener la version `0.18.0` (ya en `libs.versions.toml`). No usar `ExperimentalFlags` (objeto global, opt-in ERROR).
2. Fichero: **`gemma-4-E2B-it.litertlm` generico** (2.588.147.712 B). Descartar `-gpu.litertlm`: solo GPU y solo texto, sin fallback a CPU posible y sin documentar.
3. Backend: GPU por defecto con fallback automatico a CPU si `initialize()` lanza `LiteRtLmJniException`; ajuste para forzar CPU. `visionBackend`/`audioBackend` = null (solo texto, menos memoria). `cacheDir = context.cacheDir.path`.
4. `maxNumTokens = 4096` (Gallery usa 4000) y `maxOutputToken = 1024`; limite de turnos + `getTokenCount()` para avisar/reiniciar antes de llenar el contexto.
5. Cancelacion: NO confiar en cancelar el Job con el Flow de la libreria; usar el wrapper `callbackFlow` con `cancelProcess()` en `awaitClose` y `buffer(UNLIMITED)`. Tratar `java.util.concurrent.CancellationException` en `onError` como fin normal.
6. Ciclo de vida: "Nueva conversacion" = cancelProcess -> conversation.close() -> createConversation (reutilizar Engine). Cerrar Engine en `onCleared`/`onTrimMemory` (siempre conversacion primero). Proteger dobles cierres (ISE). Serializar init/close con `Mutex`.
7. System prompt solo via `ConversationConfig.systemInstruction = Contents.of(...)` (la plantilla Jinja del modelo la aplica). Incluir el contexto del perfil en el system prompt al crear la conversacion; no reenviar el historial (lo guarda la Conversation). Thinking desactivado (no pasar `enable_thinking`).
8. Errores amables: mapear `LiteRtLmJniException` (recortar desde "=== Source Location Trace"), `IllegalStateException`, `UnsatisfiedLinkError`/`NoClassDefFoundError` (dispositivo/ABI no soportado, p. ej. armeabi-v7a de 32 bits: el AAR no trae esa ABI).
9. R8: anadir las reglas `-keep class com.google.ai.edge.litertlm.** { *; }` (el AAR no trae reglas consumer y el release minifica). Manifiesto: los dos `uses-native-library` ya estan.
10. Requisitos: Gallery exige 8 GB de RAM para este modelo; el plan dice >= 6 GB recomendados -> comprobar `ActivityManager.MemoryInfo.totalMem` y espacio libre (>= 2,6 GB + margen) y avisar sin bloquear por debajo de 8 GB.
11. Tests: interfaz + fake; la clase real solo se compila (los data classes de config se pueden testear en JVM).
12. Descarga: `DownloadManager` a `getExternalFilesDir("models")`, verificar tamano exacto al terminar (el ETag del CDN no es el sha256). Via alternativa "Importar archivo" sigue siendo util pero no obligatoria (no gated).
