package com.tfg.kaifit_pal.ui.assistant

import com.tfg.kaifit_pal.data.llm.DeviceCheck
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.settings.InferenceBackend

/**
 * What the Kai-Q tab shows: the model first, then the engine, then the chat.
 *
 * @property model where the model file is.
 * @property device RAM, free space and ABI, once checked.
 * @property downloadOnWifiOnly current setting, for the note under the download button.
 * @property engine whether the model is loaded.
 * @property chat the conversation, kept only in memory.
 * @property notice one-shot message for a snackbar, consumed with `onNoticeShown()`.
 */
data class AssistantUiState(
    val model: ModelState = ModelState.Checking,
    val device: DeviceCheck? = null,
    val downloadOnWifiOnly: Boolean = true,
    val engine: EngineState = EngineState.Idle,
    val chat: ChatState = ChatState(),
    val notice: AssistantNotice? = null,
)

/** The language model engine, as the chat sees it. */
sealed interface EngineState {

    /** Not loaded: there is no model, or it has not been needed yet. */
    data object Idle : EngineState

    /** Loading the model and starting a conversation; takes a few seconds. */
    data object Loading : EngineState

    /**
     * Ready to chat.
     *
     * @property backend hardware in use.
     * @property fellBackToCpu the GPU was preferred but failed, so the CPU is used.
     */
    data class Ready(val backend: InferenceBackend, val fellBackToCpu: Boolean) : EngineState

    /**
     * The model could not be loaded.
     *
     * @property unsupportedDevice no backend can run on this device; retrying will not help.
     * @property canUseCpu the CPU has not been tried yet and may work.
     */
    data class Failed(val unsupportedDevice: Boolean, val canUseCpu: Boolean) : EngineState
}

/**
 * The conversation on screen.
 *
 * @property input text being typed, kept here so it survives a rotation.
 * @property isGenerating a reply is streaming.
 * @property contextFull the conversation has used up its context; a new one must be started.
 * @property hasProfile the conversation was started with the saved profile.
 */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val contextFull: Boolean = false,
    val hasProfile: Boolean = false,
) {

    /** There is something to send and nothing in the way. */
    val canSend: Boolean
        get() = input.isNotBlank() && !isGenerating && !contextFull
}

/** One message of the chat; [id] is unique within the conversations of the screen. */
data class ChatMessage(val id: Long, val role: ChatRole, val text: String, val status: MessageStatus)

/** Who wrote a message. */
enum class ChatRole { USER, ASSISTANT }

/** State of a message: replies stream first and may end stopped or failed. */
enum class MessageStatus { DONE, STREAMING, STOPPED, FAILED }

/** One-shot messages of the assistant screen. */
enum class AssistantNotice {
    /** The engine was closed, to free memory or because the model was deleted, and the chat with it. */
    ENGINE_RELEASED,
}
