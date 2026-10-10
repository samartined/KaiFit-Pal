package com.tfg.kaifit_pal.testing

import com.tfg.kaifit_pal.data.llm.LlmException
import com.tfg.kaifit_pal.data.llm.LlmRepository
import com.tfg.kaifit_pal.data.llm.loadWithFallback
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File

/**
 * Language model for tests: programmable replies, load errors and token count, and a log of
 * every call in order.
 *
 * Loading goes through the real [loadWithFallback], so a GPU that fails falls back to the CPU as
 * in the app. A reply is either [replyDeltas], sent at once, or a [Reply] the test controls with
 * [holdNextReply].
 *
 * @param calls shared call log, so that a test can check the order against other fakes.
 */
class FakeLlmRepository(val calls: MutableList<String> = mutableListOf()) : LlmRepository {

    private val loaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = loaded.asStateFlow()

    /** Arguments of every [initialize] call. */
    val initializeCalls = mutableListOf<Pair<File, InferenceBackend>>()

    /** System prompt of every conversation started. */
    val conversationPrompts = mutableListOf<String>()

    /** Text of every message sent. */
    val sentMessages = mutableListOf<String>()

    /** Collectors of [sendMessage] that were cancelled before the reply ended. */
    var cancelledReplies = 0
        private set

    var gpuLoadFails = false
    var cpuLoadFails = false
    var unsupportedDevice = false
    var startConversationError: LlmException? = null

    /** Deltas of every reply not held with [holdNextReply]. */
    var replyDeltas: List<String> = listOf("Vale.")

    /** What [usedTokens] returns. */
    var tokens = 0

    /**
     * When set, every [initialize] call is logged at once and then waits for it to complete, as a
     * slow load would. Cancelling the caller meanwhile cancels the load.
     */
    var initializeGate: CompletableDeferred<Unit>? = null

    private val heldReplies = ArrayDeque<Reply>()

    /** A reply that streams only what the test sends and ends when the test says. */
    class Reply {
        internal val channel = Channel<String>(Channel.UNLIMITED)

        fun send(delta: String) {
            channel.trySend(delta)
        }

        fun finish() {
            channel.close()
        }

        fun fail(error: LlmException) {
            channel.close(error)
        }
    }

    /** The next message gets a reply controlled by the returned [Reply]. */
    fun holdNextReply(): Reply = Reply().also { heldReplies.addLast(it) }

    /** Simulates the engine being released behind the app's back, as on low memory. */
    fun release() {
        loaded.value = false
    }

    override suspend fun initialize(modelFile: File, preferred: InferenceBackend): InferenceBackend {
        calls += "llm.initialize($preferred)"
        initializeCalls += modelFile to preferred
        initializeGate?.await()
        if (unsupportedDevice) throw LlmException.UnsupportedDevice("no native library")
        val (backend, _) = loadWithFallback(preferred) { candidate ->
            val fails = if (candidate == InferenceBackend.GPU) gpuLoadFails else cpuLoadFails
            if (fails) throw LlmException.LoadFailed("Status Code: 13. Message: cannot load on $candidate")
            candidate
        }
        loaded.value = true
        return backend
    }

    override suspend fun startConversation(systemPrompt: String) {
        calls += "llm.startConversation"
        conversationPrompts += systemPrompt
        startConversationError?.let { throw it }
    }

    override fun sendMessage(text: String): Flow<String> {
        calls += "llm.sendMessage"
        sentMessages += text
        val held = heldReplies.removeFirstOrNull()
        val deltas = held?.channel?.receiveAsFlow() ?: flowOf(*replyDeltas.toTypedArray())
        return deltas.onCompletion { cause -> if (cause is CancellationException) cancelledReplies++ }
    }

    override suspend fun usedTokens(): Int = tokens

    override suspend fun close() {
        calls += "llm.close"
        loaded.value = false
    }
}
