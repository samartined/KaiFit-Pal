package com.tfg.kaifit_pal.ui.assistant

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.data.llm.LlmException
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.testing.FakeImportSource
import com.tfg.kaifit_pal.testing.FakeLlmRepository
import com.tfg.kaifit_pal.testing.FakeModelManager
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import com.tfg.kaifit_pal.testing.ReferenceData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The Kai-Q state machine with a fake model manager and language model, and the real settings and
 * profile repositories on temporary files. Waits use `uiState.first { … }`, which also lets the
 * DataStores run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val calls = mutableListOf<String>()
    private val llm = FakeLlmRepository(calls)
    private val models = FakeModelManager(calls = calls)

    private fun TestScope.newSettings() = SettingsRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))

    private fun TestScope.newProfiles() = ProfileRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))

    private fun TestScope.newViewModel(
        settings: SettingsRepository = newSettings(),
        profiles: ProfileRepository = newProfiles(),
        releaseScope: CoroutineScope = backgroundScope,
    ) = AssistantViewModel(models, llm, settings, profiles, releaseScope)

    /** A ViewModel shown with the model downloaded, once its engine is ready. */
    private suspend fun TestScope.readyViewModel(
        settings: SettingsRepository = newSettings(),
        profiles: ProfileRepository = newProfiles(),
    ): AssistantViewModel {
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel(settings, profiles)
        viewModel.onScreenStarted()
        viewModel.awaitState { it.engine is EngineState.Ready }
        return viewModel
    }

    private suspend fun AssistantViewModel.awaitState(predicate: (AssistantUiState) -> Boolean): AssistantUiState =
        uiState.first(predicate)

    /** Sends [text] and waits for the reply to end. */
    private suspend fun AssistantViewModel.ask(text: String): AssistantUiState {
        val before = uiState.value.chat.messages.size
        onInputChange(text)
        onSend()
        return awaitState { !it.chat.isGenerating && it.chat.messages.size == before + 2 }
    }

    /** Saves the reference man at −15 %: TDEE 2819 and 2396 kcal/day. */
    private suspend fun ProfileRepository.saveReferenceMan() {
        val tdee = checkNotNull(TdeeCalculator.calculate(ReferenceData.man, ReferenceData.manActivity))
        val modifier = GoalModifier(-15)
        save(ReferenceData.man, ReferenceData.manActivity, tdee, modifier, MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal))
    }

    private fun AssistantUiState.messageSummary() = chat.messages.map { Triple(it.role, it.text, it.status) }

    // A1
    @Test
    fun withoutModelTheEngineIsNeverLoaded() = runTest {
        val viewModel = newViewModel()

        viewModel.onScreenStarted()
        val state = viewModel.awaitState { it.device != null }
        advanceUntilIdle()

        assertEquals(ModelState.NotDownloaded, state.model)
        assertEquals(EngineState.Idle, viewModel.uiState.value.engine)
        assertTrue(llm.initializeCalls.isEmpty())
        assertEquals(listOf("model.refresh"), calls)
    }

    // A2
    @Test
    fun shownWithTheModelLoadsTheGpuAndStartsAConversationWithTheProfile() = runTest {
        val profiles = newProfiles()
        profiles.saveReferenceMan()
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel(profiles = profiles)
        viewModel.awaitState { it.model is ModelState.Downloaded }
        advanceUntilIdle()
        // Not loaded with the app or the ViewModel: only when the screen is shown.
        assertTrue(llm.initializeCalls.isEmpty())

        viewModel.onScreenStarted()
        val state = viewModel.awaitState { it.engine is EngineState.Ready }

        assertEquals(listOf(FakeModelManager.downloaded.file to InferenceBackend.GPU), llm.initializeCalls)
        assertTrue(llm.conversationPrompts.single().contains("2819 kcal/día"))
        assertEquals(EngineState.Ready(InferenceBackend.GPU, fellBackToCpu = false), state.engine)
        assertTrue(state.chat.hasProfile)
    }

    // A3
    @Test
    fun whenGpuAndCpuFailTheCpuIsNotOfferedAndRetryLoadsAgain() = runTest {
        llm.gpuLoadFails = true
        llm.cpuLoadFails = true
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel()

        viewModel.onScreenStarted()
        val failed = viewModel.awaitState { it.engine is EngineState.Failed }
        assertEquals(EngineState.Failed(unsupportedDevice = false, canUseCpu = false), failed.engine)

        llm.cpuLoadFails = false
        viewModel.onRetry()
        val ready = viewModel.awaitState { it.engine is EngineState.Ready }

        assertEquals(2, llm.initializeCalls.size)
        assertEquals(EngineState.Ready(InferenceBackend.CPU, fellBackToCpu = true), ready.engine)
    }

    // A3
    @Test
    fun whenOnlyTheGpuFailsTheCpuIsUsedAndShown() = runTest {
        llm.gpuLoadFails = true
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel()

        viewModel.onScreenStarted()
        val state = viewModel.awaitState { it.engine !is EngineState.Idle && it.engine != EngineState.Loading }

        assertEquals(EngineState.Ready(InferenceBackend.CPU, fellBackToCpu = true), state.engine)
        assertEquals(listOf(InferenceBackend.GPU), llm.initializeCalls.map { it.second })
    }

    // A3
    @Test
    fun conversationFailingOnTheGpuOffersTheCpu() = runTest {
        llm.startConversationError = LlmException.LoadFailed("Status Code: 13. Message: GPU")
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel()

        viewModel.onScreenStarted()
        val state = viewModel.awaitState { it.engine is EngineState.Failed }

        assertEquals(EngineState.Failed(unsupportedDevice = false, canUseCpu = true), state.engine)
    }

    // A4
    @Test
    fun useCpuSavesTheSettingAndLoadsOnTheCpu() = runTest {
        val settings = newSettings()
        llm.startConversationError = LlmException.LoadFailed("Status Code: 13. Message: GPU")
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel(settings = settings)
        viewModel.onScreenStarted()
        viewModel.awaitState { it.engine is EngineState.Failed }

        llm.startConversationError = null
        viewModel.onUseCpu()
        val state = viewModel.awaitState { it.engine is EngineState.Ready }

        assertEquals(InferenceBackend.CPU, settings.settings.first().inferenceBackend)
        assertEquals(InferenceBackend.CPU, llm.initializeCalls.last().second)
        assertEquals(EngineState.Ready(InferenceBackend.CPU, fellBackToCpu = false), state.engine)
    }

    // A5
    @Test
    fun unsupportedDeviceFailsWithoutOfferingTheCpu() = runTest {
        llm.unsupportedDevice = true
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel()

        viewModel.onScreenStarted()
        val state = viewModel.awaitState { it.engine is EngineState.Failed }

        assertEquals(EngineState.Failed(unsupportedDevice = true, canUseCpu = false), state.engine)
    }

    // A6
    @Test
    fun replyStreamsIntoTheLastMessageAndEndsDone() = runTest {
        val viewModel = readyViewModel()
        val reply = llm.holdNextReply()

        viewModel.onInputChange("hola")
        viewModel.onSend()
        assertTrue(viewModel.uiState.value.chat.isGenerating)
        assertEquals("", viewModel.uiState.value.chat.input)
        reply.send("Ho")
        reply.send("la")
        viewModel.awaitState { it.chat.messages.last().text == "Hola" }
        reply.finish()
        val state = viewModel.awaitState { !it.chat.isGenerating }

        assertEquals(
            listOf(
                Triple(ChatRole.USER, "hola", MessageStatus.DONE),
                Triple(ChatRole.ASSISTANT, "Hola", MessageStatus.DONE),
            ),
            state.messageSummary(),
        )
        assertEquals(listOf("hola"), llm.sentMessages)
    }

    // A7
    @Test
    fun blankInputAndSendingWhileGeneratingAreIgnored() = runTest {
        val viewModel = readyViewModel()

        viewModel.onInputChange("   ")
        viewModel.onSend()
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())
        assertFalse(viewModel.uiState.value.chat.canSend)

        val reply = llm.holdNextReply()
        viewModel.onInputChange("hola")
        viewModel.onSend()
        viewModel.onInputChange("otra pregunta")
        viewModel.onSend()
        viewModel.onSuggestionClick("¿Cuánta proteína debería tomar al día?")

        assertEquals(listOf("hola"), llm.sentMessages)
        assertEquals(2, viewModel.uiState.value.chat.messages.size)
        assertEquals("otra pregunta", viewModel.uiState.value.chat.input)
        reply.finish()
    }

    // A7
    @Test
    fun inputIsCutAtTheLimit() = runTest {
        val viewModel = newViewModel()

        viewModel.onInputChange("a".repeat(1_200))

        assertEquals(1_000, viewModel.uiState.value.chat.input.length)
    }

    // A8
    @Test
    fun stopCancelsTheReplyAndKeepsItsTextAsStopped() = runTest {
        val viewModel = readyViewModel()
        val reply = llm.holdNextReply()
        viewModel.onInputChange("¿Y antes de entrenar?")
        viewModel.onSend()
        reply.send("Una o dos horas antes")
        viewModel.awaitState { it.chat.messages.last().text == "Una o dos horas antes" }

        viewModel.onStop()
        advanceUntilIdle()
        reply.send(", toma algo con hidratos.")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, llm.cancelledReplies)
        assertEquals(Triple(ChatRole.ASSISTANT, "Una o dos horas antes", MessageStatus.STOPPED), state.messageSummary().last())
        assertFalse(state.chat.isGenerating)
    }

    // A9
    @Test
    fun failedReplyIsMarkedAndTheUserCanSendAgain() = runTest {
        val viewModel = readyViewModel()
        val reply = llm.holdNextReply()
        viewModel.onInputChange("hola")
        viewModel.onSend()
        reply.send("Ho")
        reply.fail(LlmException.GenerationFailed("Status Code: 13. Message: decode"))
        val failed = viewModel.awaitState { !it.chat.isGenerating }
        assertEquals(Triple(ChatRole.ASSISTANT, "Ho", MessageStatus.FAILED), failed.messageSummary().last())
        assertTrue(failed.engine is EngineState.Ready)

        val state = viewModel.ask("¿Me lo repites?")

        assertEquals(listOf("hola", "¿Me lo repites?"), llm.sentMessages)
        assertEquals(Triple(ChatRole.ASSISTANT, "Vale.", MessageStatus.DONE), state.messageSummary().last())
    }

    // A10
    @Test
    fun newConversationEmptiesTheChatAndReadsTheProfileAgain() = runTest {
        val profiles = newProfiles()
        val viewModel = readyViewModel(profiles = profiles)
        assertFalse(viewModel.uiState.value.chat.hasProfile)
        viewModel.ask("hola")
        profiles.saveReferenceMan()

        viewModel.onNewConversation()
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())
        val state = viewModel.awaitState { it.chat.hasProfile }

        assertEquals(2, llm.conversationPrompts.size)
        assertFalse(llm.conversationPrompts[0].contains("2819"))
        assertTrue(llm.conversationPrompts[1].contains("2819 kcal/día"))
        assertTrue(state.chat.messages.isEmpty())
        assertEquals(1, llm.initializeCalls.size)
    }

    // A11
    @Test
    fun conversationIsFullAt2560TokensUntilANewOneStarts() = runTest {
        val viewModel = readyViewModel()
        llm.tokens = 2_559
        assertFalse(viewModel.ask("hola").chat.contextFull)

        llm.tokens = 2_560
        val full = viewModel.ask("¿y la grasa?")
        viewModel.onInputChange("otra pregunta")

        assertTrue(full.chat.contextFull)
        assertFalse(viewModel.uiState.value.chat.canSend)
        viewModel.onSend()
        assertEquals(2, llm.sentMessages.size)

        llm.tokens = 0
        viewModel.onNewConversation()
        assertFalse(viewModel.uiState.value.chat.contextFull)
        assertTrue(viewModel.uiState.value.chat.canSend)
    }

    // A12
    @Test
    fun conversationIsFullAfterTwentyUserTurns() = runTest {
        val viewModel = readyViewModel()

        repeat(19) { turn -> assertFalse(viewModel.ask("pregunta ${turn + 1}").chat.contextFull) }
        val state = viewModel.ask("pregunta 20")

        assertTrue(state.chat.contextFull)
        assertEquals(40, state.chat.messages.size)
    }

    // A13
    @Test
    fun changingTheBackendReloadsTheEngineAndEmptiesTheChat() = runTest {
        val settings = newSettings()
        val viewModel = readyViewModel(settings = settings)
        viewModel.ask("hola")

        settings.setInferenceBackend(InferenceBackend.CPU)
        val state = viewModel.awaitState { it.engine == EngineState.Ready(InferenceBackend.CPU, fellBackToCpu = false) }

        assertEquals(listOf(InferenceBackend.GPU, InferenceBackend.CPU), llm.initializeCalls.map { it.second })
        assertTrue(state.chat.messages.isEmpty())
    }

    // A13b
    @Test
    fun changingTheBackendWhileLoadingLoadsOnTheNewBackend() = runTest {
        val settings = newSettings()
        val gate = CompletableDeferred<Unit>()
        llm.initializeGate = gate
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel(settings = settings)
        viewModel.onScreenStarted()
        runCurrent()
        assertEquals(listOf(FakeModelManager.downloaded.file to InferenceBackend.GPU), llm.initializeCalls)
        assertEquals(EngineState.Loading, viewModel.uiState.value.engine)

        settings.setInferenceBackend(InferenceBackend.CPU)
        runCurrent()
        // The GPU load has not ended yet: the change must not wait for it.
        assertEquals(listOf(InferenceBackend.GPU, InferenceBackend.CPU), llm.initializeCalls.map { it.second })
        assertEquals(EngineState.Loading, viewModel.uiState.value.engine)
        gate.complete(Unit)
        val state = viewModel.awaitState { it.engine is EngineState.Ready }

        assertEquals(FakeModelManager.downloaded.file to InferenceBackend.CPU, llm.initializeCalls.last())
        assertEquals(EngineState.Ready(InferenceBackend.CPU, fellBackToCpu = false), state.engine)
        assertEquals(1, llm.conversationPrompts.size)
    }

    // A14
    @Test
    fun modelGoneEmptiesTheChatAndUnloadsTheEngineState() = runTest {
        val viewModel = readyViewModel()
        viewModel.ask("hola")

        models.state.value = ModelState.NotDownloaded
        val state = viewModel.awaitState { it.model == ModelState.NotDownloaded }

        assertEquals(EngineState.Idle, state.engine)
        assertTrue(state.chat.messages.isEmpty())
    }

    // A15
    @Test
    fun suggestionIsSentAsTheQuestion() = runTest {
        val viewModel = readyViewModel()

        viewModel.onSuggestionClick("¿Cuánta proteína debería tomar al día?")
        val state = viewModel.awaitState { !it.chat.isGenerating && it.chat.messages.size == 2 }

        assertEquals(listOf("¿Cuánta proteína debería tomar al día?"), llm.sentMessages)
        assertEquals(Triple(ChatRole.USER, "¿Cuánta proteína debería tomar al día?", MessageStatus.DONE), state.messageSummary().first())
    }

    // A16
    @Test
    fun engineReleasedOnLowMemoryEmptiesTheChatWithANoticeAndReloadsOnNextStart() = runTest {
        val viewModel = readyViewModel()
        viewModel.ask("hola")

        llm.release()
        val released = viewModel.uiState.value

        assertEquals(EngineState.Idle, released.engine)
        assertTrue(released.chat.messages.isEmpty())
        assertEquals(AssistantNotice.ENGINE_RELEASED, released.notice)

        viewModel.onNoticeShown()
        assertNull(viewModel.uiState.value.notice)
        viewModel.onScreenStarted()
        viewModel.awaitState { it.engine is EngineState.Ready }
        assertEquals(2, llm.initializeCalls.size)
    }

    // A17
    @Test
    fun modelActionsAreDelegatedWithTheWifiSetting() = runTest {
        val settings = newSettings()
        settings.setDownloadOnWifiOnly(false)
        val viewModel = newViewModel(settings = settings)
        val source = FakeImportSource(FakeImportSource.modelBytes(10))

        viewModel.onDownload()
        assertEquals(listOf(false), models.downloadRequests.first { it.isNotEmpty() })
        viewModel.onImportModel(source)
        viewModel.onCancelModelTransfer()
        viewModel.onDismissModelError()
        advanceUntilIdle()

        assertSame(source, models.imports.single())
        assertEquals(listOf("model.startDownload", "model.importModel", "model.cancel", "model.dismissError"), calls)
    }

    // A17
    @Test
    fun clearingTheViewModelClosesTheEngineInTheReleaseScope() = runTest {
        val settings = newSettings()
        val profiles = newProfiles()
        val releaseScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        models.state.value = FakeModelManager.downloaded
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(
            store,
            viewModelFactory { initializer { AssistantViewModel(models, llm, settings, profiles, releaseScope) } },
        )[AssistantViewModel::class.java]
        viewModel.onScreenStarted()
        viewModel.awaitState { it.engine is EngineState.Ready }
        assertFalse(calls.contains("llm.close"))

        store.clear()

        assertEquals("llm.close", calls.last())
    }

    // A18
    @Test
    fun modelThatArrivesWhileTheScreenIsShownIsLoaded() = runTest {
        val viewModel = newViewModel()
        viewModel.onScreenStarted()
        viewModel.awaitState { it.device != null }
        assertEquals(EngineState.Idle, viewModel.uiState.value.engine)

        // A download or an import ends with Kai-Q on screen.
        models.state.value = FakeModelManager.downloaded
        val state = viewModel.awaitState { it.engine is EngineState.Ready }

        assertEquals(listOf(FakeModelManager.downloaded.file to InferenceBackend.GPU), llm.initializeCalls)
        assertEquals(EngineState.Ready(InferenceBackend.GPU, fellBackToCpu = false), state.engine)
    }

    // A19
    @Test
    fun modelThatArrivesAfterTheScreenIsStoppedWaitsForTheNextStart() = runTest {
        val viewModel = newViewModel()
        viewModel.onScreenStarted()
        viewModel.awaitState { it.device != null }
        viewModel.onScreenStopped()

        models.state.value = FakeModelManager.downloaded
        val state = viewModel.awaitState { it.model is ModelState.Downloaded }
        runCurrent()

        assertEquals(EngineState.Idle, state.engine)
        assertEquals(EngineState.Idle, viewModel.uiState.value.engine)
        assertTrue(llm.initializeCalls.isEmpty())

        viewModel.onScreenStarted()
        viewModel.awaitState { it.engine is EngineState.Ready }
        assertEquals(1, llm.initializeCalls.size)
    }
}
