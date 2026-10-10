package com.tfg.kaifit_pal.data.llm

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
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [LlmRepository] on LiteRT-LM: one [Engine] with the model and one [Conversation] on it.
 *
 * Loading and closing run on [ioDispatcher] under a [Mutex], because the engine calls block for
 * seconds and both objects throw if they are closed twice or used after closing. The order is
 * always the conversation first, then the engine.
 *
 * Only text is used: the vision and audio encoders of the model are never loaded, and thinking
 * mode stays off.
 *
 * @param cacheDir writable folder where the engine caches what makes the next load faster.
 */
class LiteRtLmRepository(
    private val cacheDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LlmRepository {

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var loadedFile: File? = null
    private var loadedPreference: InferenceBackend? = null
    private var backendInUse: InferenceBackend? = null

    /** Read without the lock by [sendMessage]; written under it. */
    @Volatile
    private var conversation: Conversation? = null

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    override suspend fun initialize(modelFile: File, preferred: InferenceBackend): InferenceBackend =
        withContext(ioDispatcher) {
            mutex.withLock {
                val current = backendInUse
                if (engine != null && current != null && loadedFile == modelFile && loadedPreference == preferred) {
                    return@withLock current
                }
                closeLocked()
                val (loaded, backend) = loadWithFallback(preferred) { candidate -> loadEngine(modelFile, candidate) }
                engine = loaded
                loadedFile = modelFile
                loadedPreference = preferred
                backendInUse = backend
                _isLoaded.value = true
                backend
            }
        }

    override suspend fun startConversation(systemPrompt: String) {
        withContext(ioDispatcher) {
            mutex.withLock {
                val current = engine ?: throw LlmException.NotReady()
                closeConversationLocked()
                conversation = try {
                    current.createConversation(conversationConfig(systemPrompt))
                } catch (e: LiteRtLmJniException) {
                    throw e.toLlmException()
                } catch (e: IllegalStateException) {
                    throw e.toLlmException()
                }
            }
        }
    }

    override fun sendMessage(text: String): Flow<String> = callbackFlow {
        val current = conversation?.takeIf { it.isAlive } ?: throw LlmException.NotReady()
        val finished = AtomicBoolean(false)
        val callback = object : MessageCallback {
            // Called on a native thread: only the thread-safe channel calls are used here.
            override fun onMessage(message: Message) {
                trySend(message.toString())
            }

            override fun onDone() {
                finished.set(true)
                close()
            }

            override fun onError(throwable: Throwable) {
                finished.set(true)
                // cancelProcess() ends the generation with this exception: a normal end, not an error.
                if (throwable is CancellationException) close() else close(throwable.toLlmException(duringGeneration = true))
            }
        }
        try {
            current.sendMessageAsync(Contents.of(text), callback)
        } catch (e: LiteRtLmJniException) {
            throw e.toLlmException(duringGeneration = true)
        } catch (e: IllegalStateException) {
            throw e.toLlmException()
        }
        // The collector went away before the end, for example after "Detener": stop the engine too.
        awaitClose { if (!finished.get()) current.cancelQuietly() }
    }.buffer(Channel.UNLIMITED)

    override suspend fun usedTokens(): Int = withContext(ioDispatcher) {
        mutex.withLock {
            val current = conversation?.takeIf { it.isAlive } ?: return@withLock 0
            try {
                current.getTokenCount()
            } catch (e: LiteRtLmJniException) {
                0
            } catch (e: IllegalStateException) {
                0
            }
        }
    }

    override suspend fun close() {
        withContext(ioDispatcher) {
            mutex.withLock { closeLocked() }
        }
    }

    private fun loadEngine(modelFile: File, backend: InferenceBackend): Engine {
        val candidate = Engine(engineConfig(modelFile.path, backend, cacheDir.path))
        // If initialize() fails, the engine holds nothing native and must not be closed.
        try {
            candidate.initialize()
        } catch (e: LiteRtLmJniException) {
            throw e.toLlmException()
        } catch (e: UnsatisfiedLinkError) {
            throw e.toLlmException()
        } catch (e: NoClassDefFoundError) {
            throw e.toLlmException()
        } catch (e: IllegalStateException) {
            throw e.toLlmException()
        }
        return candidate
    }

    private fun closeLocked() {
        closeConversationLocked()
        val current = engine
        engine = null
        loadedFile = null
        loadedPreference = null
        backendInUse = null
        if (current != null) {
            try {
                if (current.isInitialized()) current.close()
            } catch (e: IllegalStateException) {
                // Already closed: nothing left to release.
            }
        }
        _isLoaded.value = false
    }

    private fun closeConversationLocked() {
        val current = conversation ?: return
        conversation = null
        try {
            if (current.isAlive) {
                current.cancelProcess()
                current.close()
            }
        } catch (e: IllegalStateException) {
            // Closed meanwhile: nothing left to release.
        }
    }

    private fun Conversation.cancelQuietly() {
        try {
            if (isAlive) cancelProcess()
        } catch (e: IllegalStateException) {
            // Closed meanwhile: there is no generation left to stop.
        }
    }
}

/** Engine settings: the model file, the backend, no vision or audio, 4096 tokens of context. */
internal fun engineConfig(modelPath: String, backend: InferenceBackend, cacheDir: String): EngineConfig =
    EngineConfig(
        modelPath = modelPath,
        backend = when (backend) {
            InferenceBackend.GPU -> Backend.GPU()
            InferenceBackend.CPU -> Backend.CPU()
        },
        visionBackend = null,
        audioBackend = null,
        maxNumTokens = LlmConfig.MAX_NUM_TOKENS,
        cacheDir = cacheDir,
    )

/** Conversation settings: the system prompt, the sampler and the reply limit; no thinking mode. */
internal fun conversationConfig(systemPrompt: String): ConversationConfig =
    ConversationConfig(
        systemInstruction = Contents.of(systemPrompt),
        samplerConfig = SamplerConfig(topK = LlmConfig.TOP_K, topP = LlmConfig.TOP_P, temperature = LlmConfig.TEMPERATURE),
        maxOutputToken = LlmConfig.MAX_OUTPUT_TOKENS,
    )

/**
 * The [LlmException] for an error of the inference library.
 *
 * Engine errors ([LiteRtLmJniException]) become [LlmException.LoadFailed], or
 * [LlmException.GenerationFailed] when [duringGeneration], with their message cut before the
 * native stack trace. A native library that cannot be loaded means an unsupported device, and an
 * [IllegalStateException] a call on a closed or missing engine or conversation.
 */
internal fun Throwable.toLlmException(duringGeneration: Boolean = false): LlmException = when (this) {
    is LlmException -> this
    is UnsatisfiedLinkError, is NoClassDefFoundError -> LlmException.UnsupportedDevice(message.orEmpty())
    is IllegalStateException -> LlmException.NotReady()
    else -> {
        val detail = message.orEmpty().substringBefore(NATIVE_TRACE_MARKER).trim()
        if (duringGeneration) LlmException.GenerationFailed(detail) else LlmException.LoadFailed(detail)
    }
}

/** Where the native stack trace starts in the message of a [LiteRtLmJniException]. */
private const val NATIVE_TRACE_MARKER = "=== Source Location Trace"
