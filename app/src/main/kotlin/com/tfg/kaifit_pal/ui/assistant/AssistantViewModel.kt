package com.tfg.kaifit_pal.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.llm.ImportSource
import com.tfg.kaifit_pal.data.llm.KaiQPrompt
import com.tfg.kaifit_pal.data.llm.LlmConfig
import com.tfg.kaifit_pal.data.llm.LlmException
import com.tfg.kaifit_pal.data.llm.LlmRepository
import com.tfg.kaifit_pal.data.llm.ModelManager
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * State machine of the Kai-Q tab: model → engine → chat.
 *
 * - The model state comes from [modelManager]; download and import go on in the app scope.
 * - The engine is loaded only while the screen is shown ([onScreenStarted]), never at app start.
 * - Each conversation starts with the Kai-Q persona and the profile read again from
 *   [profileRepository]. Its history lives only here: it survives a rotation and is lost with
 *   the process.
 * - The engine is closed in [releaseScope] when this ViewModel is cleared.
 */
class AssistantViewModel(
    private val modelManager: ModelManager,
    private val llmRepository: LlmRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val releaseScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var screenStarted = false
    private var loadJob: Job? = null
    private var conversationJob: Job? = null
    private var generationJob: Job? = null
    private var userTurns = 0
    private var nextMessageId = 0L
    private var lastBackend: InferenceBackend? = null

    init {
        viewModelScope.launch { modelManager.state.collect { onModelState(it) } }
        viewModelScope.launch { settingsRepository.settings.collect { onSettings(it) } }
        viewModelScope.launch { llmRepository.isLoaded.collect { onEngineLoadedChange(it) } }
    }

    /** The screen is visible: reconciles the model with the disk and loads the engine if needed. */
    fun onScreenStarted() {
        screenStarted = true
        viewModelScope.launch {
            modelManager.refresh()
            val device = modelManager.deviceCheck()
            _uiState.update { it.copy(device = device) }
            val engine = _uiState.value.engine
            if (engine !is EngineState.Ready && engine != EngineState.Loading) loadEngine()
        }
    }

    /** The screen is no longer visible: a model that finishes downloading waits to be loaded. */
    fun onScreenStopped() {
        screenStarted = false
    }

    fun onRetry() {
        loadEngine()
    }

    /** Saves the CPU as the backend and loads the model on it. */
    fun onUseCpu() {
        viewModelScope.launch {
            lastBackend = InferenceBackend.CPU
            try {
                settingsRepository.setInferenceBackend(InferenceBackend.CPU)
            } catch (e: IOException) {
                // The setting could not be saved: the CPU is still used for this load.
            }
            loadEngine(preferred = InferenceBackend.CPU)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(chat = it.chat.copy(input = text.take(LlmConfig.MAX_INPUT_CHARS))) }
    }

    fun onSend() {
        val chat = _uiState.value.chat
        if (chat.canSend) send(chat.input.trim())
    }

    fun onSuggestionClick(text: String) {
        send(text)
    }

    /** Stops the reply being written; the text so far stays, marked as stopped. */
    fun onStop() {
        val job = generationJob ?: return
        // Cancelling the collector makes the repository stop the engine too.
        job.cancel()
        generationJob = null
        _uiState.update { state ->
            state.copy(
                chat = state.chat.copy(
                    messages = state.chat.messages.map {
                        if (it.status == MessageStatus.STREAMING) it.copy(status = MessageStatus.STOPPED) else it
                    },
                    isGenerating = false,
                ),
            )
        }
        viewModelScope.launch { updateContextFull() }
    }

    /** Empties the chat and starts a conversation with the profile as it is now. */
    fun onNewConversation() {
        val ready = _uiState.value.engine as? EngineState.Ready ?: return
        generationJob?.cancel()
        generationJob = null
        conversationJob?.cancel()
        clearChat()
        conversationJob = viewModelScope.launch {
            try {
                startConversation()
            } catch (e: LlmException) {
                _uiState.update { it.copy(engine = failedState(e, cpuNotTried = ready.backend == InferenceBackend.GPU)) }
            }
        }
    }

    fun onDownload() {
        viewModelScope.launch {
            modelManager.startDownload(wifiOnly = settingsRepository.settings.first().downloadOnWifiOnly)
        }
    }

    fun onImportModel(source: ImportSource) {
        viewModelScope.launch { modelManager.importModel(source) }
    }

    fun onCancelModelTransfer() {
        viewModelScope.launch { modelManager.cancel() }
    }

    fun onDismissModelError() {
        modelManager.dismissError()
    }

    fun onNoticeShown() {
        _uiState.update { it.copy(notice = null) }
    }

    override fun onCleared() {
        generationJob?.cancel()
        releaseScope.launch { llmRepository.close() }
    }

    private fun onModelState(model: ModelState) {
        _uiState.update { it.copy(model = model) }
        val state = _uiState.value
        if (model is ModelState.Downloaded) {
            // A download or import that ends while the screen is shown is loaded right away.
            if (screenStarted && state.engine == EngineState.Idle) loadEngine()
        } else if (state.engine != EngineState.Idle || state.chat.messages.isNotEmpty()) {
            resetEngineState(notice = null)
        }
    }

    private fun onSettings(settings: AppSettings) {
        val previous = lastBackend
        lastBackend = settings.inferenceBackend
        _uiState.update { it.copy(downloadOnWifiOnly = settings.downloadOnWifiOnly) }
        if (previous != null && previous != settings.inferenceBackend && _uiState.value.engine is EngineState.Ready) {
            loadEngine(preferred = settings.inferenceBackend)
        }
    }

    private fun onEngineLoadedChange(loaded: Boolean) {
        // Only a Ready engine can be released behind our back; reloads go through Loading first.
        if (!loaded && _uiState.value.engine is EngineState.Ready) {
            resetEngineState(notice = AssistantNotice.ENGINE_RELEASED)
        }
    }

    /** Loads the downloaded model on [preferred] (or the saved backend) and starts a conversation. */
    private fun loadEngine(preferred: InferenceBackend? = null) {
        val model = modelManager.state.value as? ModelState.Downloaded ?: return
        generationJob?.cancel()
        generationJob = null
        conversationJob?.cancel()
        loadJob?.cancel()
        clearChat()
        _uiState.update { it.copy(engine = EngineState.Loading) }
        loadJob = viewModelScope.launch {
            val backend = preferred ?: settingsRepository.settings.first().inferenceBackend
            var loadedOn: InferenceBackend? = null
            try {
                loadedOn = llmRepository.initialize(model.file, backend)
                startConversation()
                val fellBack = backend == InferenceBackend.GPU && loadedOn == InferenceBackend.CPU
                _uiState.update { it.copy(engine = EngineState.Ready(loadedOn, fellBackToCpu = fellBack)) }
            } catch (e: LlmException) {
                // With the GPU preferred, a failed load has already tried the CPU: only a failure
                // after loading on the GPU leaves the CPU untried.
                val cpuNotTried = backend == InferenceBackend.GPU && loadedOn == InferenceBackend.GPU
                _uiState.update { it.copy(engine = failedState(e, cpuNotTried)) }
            }
        }
    }

    /** Opens a conversation with the persona and the saved profile, read now. Throws LlmException. */
    private suspend fun startConversation() {
        val profile = profileRepository.profile.first()
        llmRepository.startConversation(KaiQPrompt.systemInstruction(profile))
        _uiState.update { it.copy(chat = it.chat.copy(hasProfile = profile != null)) }
    }

    private fun send(text: String) {
        val state = _uiState.value
        if (state.engine !is EngineState.Ready || state.chat.isGenerating || state.chat.contextFull || text.isBlank()) return
        val question = ChatMessage(nextMessageId++, ChatRole.USER, text, MessageStatus.DONE)
        val reply = ChatMessage(nextMessageId++, ChatRole.ASSISTANT, "", MessageStatus.STREAMING)
        userTurns++
        _uiState.update {
            it.copy(chat = it.chat.copy(messages = it.chat.messages + question + reply, input = "", isGenerating = true))
        }
        // A new conversation may still be opening: the message must go to that one.
        val opening = conversationJob
        generationJob = viewModelScope.launch {
            opening?.join()
            var failed = false
            llmRepository.sendMessage(text)
                .catch { error -> if (error is LlmException) failed = true else throw error }
                .collect { delta -> updateMessage(reply.id) { it.copy(text = it.text + delta) } }
            val full = isContextFull()
            updateMessage(reply.id) { it.copy(status = if (failed) MessageStatus.FAILED else MessageStatus.DONE) }
            _uiState.update { it.copy(chat = it.chat.copy(isGenerating = false, contextFull = full)) }
            generationJob = null
        }
    }

    private suspend fun updateContextFull() {
        val full = isContextFull()
        _uiState.update { it.copy(chat = it.chat.copy(contextFull = full)) }
    }

    private suspend fun isContextFull(): Boolean =
        userTurns >= LlmConfig.MAX_USER_TURNS || llmRepository.usedTokens() >= LlmConfig.CONTEXT_FULL_TOKENS

    private fun updateMessage(id: Long, change: (ChatMessage) -> ChatMessage) {
        _uiState.update { state ->
            state.copy(chat = state.chat.copy(messages = state.chat.messages.map { if (it.id == id) change(it) else it }))
        }
    }

    /** Empties the conversation on screen; the text being typed stays. */
    private fun clearChat() {
        userTurns = 0
        _uiState.update {
            it.copy(chat = it.chat.copy(messages = emptyList(), isGenerating = false, contextFull = false))
        }
    }

    /** Back to no engine and an empty chat, after the model or the engine went away. */
    private fun resetEngineState(notice: AssistantNotice?) {
        generationJob?.cancel()
        generationJob = null
        conversationJob?.cancel()
        loadJob?.cancel()
        clearChat()
        _uiState.update { it.copy(engine = EngineState.Idle, notice = notice ?: it.notice) }
    }

    private fun failedState(error: LlmException, cpuNotTried: Boolean): EngineState.Failed {
        val unsupported = error is LlmException.UnsupportedDevice
        return EngineState.Failed(unsupportedDevice = unsupported, canUseCpu = !unsupported && cpuNotTried)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KaiFitApp).container
                AssistantViewModel(
                    modelManager = container.modelManager,
                    llmRepository = container.llmRepository,
                    settingsRepository = container.settingsRepository,
                    profileRepository = container.profileRepository,
                    releaseScope = container.applicationScope,
                )
            }
        }
    }
}
