package com.tfg.kaifit_pal.data.llm

import com.tfg.kaifit_pal.data.settings.InferenceBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The on-device language model: one engine for the whole app and one conversation at a time.
 *
 * The conversation keeps its own history, so each call to [sendMessage] only carries the new
 * user message.
 */
interface LlmRepository {

    /** True while an engine is loaded; becomes false after close() or a release on low memory. */
    val isLoaded: StateFlow<Boolean>

    /** Loads the model (no-op if the same file and backend are already loaded). Returns the backend in use. Throws LlmException. */
    suspend fun initialize(modelFile: File, preferred: InferenceBackend): InferenceBackend

    /** Closes the current conversation, if any, and opens a new one with this system prompt. Throws LlmException. */
    suspend fun startConversation(systemPrompt: String)

    /** Streams the reply as text deltas. Cancelling the collector stops the native generation. Errors arrive as LlmException. */
    fun sendMessage(text: String): Flow<String>

    /** Tokens used by the current conversation, or 0. */
    suspend fun usedTokens(): Int

    /** Stops any generation and closes the conversation, then the engine. Safe to call twice. */
    suspend fun close()
}

/** Why the language model could not do what was asked. */
sealed class LlmException(message: String) : Exception(message) {

    /** The inference library cannot run on this device (missing native library or ABI). */
    class UnsupportedDevice(detail: String) : LlmException(detail)

    /** The engine or the conversation could not be created with this model and backend. */
    class LoadFailed(detail: String) : LlmException(detail)

    /** The reply failed halfway, for a reason other than being stopped. */
    class GenerationFailed(detail: String) : LlmException(detail)

    /** Used before loading the engine or starting a conversation; a bug, never shown as is. */
    class NotReady : LlmException("No engine or conversation")
}
