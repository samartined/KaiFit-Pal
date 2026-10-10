package com.tfg.kaifit_pal.ui.assistant

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.testing.assertTouchHeightIsAtLeast
import com.tfg.kaifit_pal.testing.assertTouchWidthIsAtLeast
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class AssistantScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    private fun setContent(state: AssistantUiState) {
        composeRule.setContent {
            KaiFitTheme {
                Content(state)
            }
        }
    }

    @Composable
    private fun Content(state: AssistantUiState) {
        AssistantContent(
            state = state,
            onDownload = { events += "download" },
            onImport = { events += "import" },
            onCancelModelTransfer = { events += "cancel" },
            onDismissModelError = { events += "dismiss" },
            onRetry = { events += "retry" },
            onUseCpu = { events += "useCpu" },
            onInputChange = { events += "input:$it" },
            onSend = { events += "send" },
            onStop = { events += "stop" },
            onSuggestionClick = { events += "suggestion:$it" },
            onNewConversation = { events += "newConversation" },
            onNoticeShown = { events += "noticeShown" },
        )
    }

    private fun string(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    // U17
    @Test
    fun notDownloadedExplainsTheSizeAndDownloads() {
        setContent(AssistantPreviewStates.notDownloaded)

        composeRule.onNodeWithText("(2,6\u00a0GB)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_ram_recommended)).assertExists()
        composeRule.onNodeWithText(string(R.string.model_wifi_only_note)).assertExists()
        composeRule.onNodeWithText(string(R.string.model_import)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.model_download)).assertIsEnabled().performClick()

        assertEquals(listOf("download"), events)
    }

    // U17
    @Test
    fun withoutSpaceDownloadIsDisabledAndLowRamIsOnlyAWarning() {
        setContent(AssistantPreviewStates.notDownloadedWithWarnings)

        composeRule.onNodeWithText(string(R.string.model_download)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.model_import)).assertIsEnabled()
        // The recommendation stays, with the warning under it.
        composeRule.onNodeWithText(string(R.string.model_ram_recommended)).assertExists()
        composeRule.onNodeWithText(string(R.string.model_ram_low, "4,0\u00a0GB")).assertExists()
        composeRule.onNodeWithText(string(R.string.model_free_space_missing, "1,1\u00a0GB")).assertExists()
        composeRule.onNodeWithText(string(R.string.model_metered_note)).assertExists()
    }

    // U17
    @Test
    fun deviceWithout64BitsGetsOnlyTheExplanation() {
        setContent(AssistantPreviewStates.unsupported)

        composeRule.onNodeWithText(string(R.string.model_unsupported_device)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_download)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.model_import)).assertDoesNotExist()
    }

    // U18
    @Test
    fun downloadingShowsPercentSizesAndCancel() {
        setContent(AssistantPreviewStates.downloading)

        composeRule.onNodeWithText("42\u00a0% · 1,1 de 2,6\u00a0GB").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_cancel)).performClick()

        assertEquals(listOf("cancel"), events)
    }

    // U18
    @Test
    fun downloadWaitingForWifiSaysSo() {
        setContent(AssistantPreviewStates.waitingForWifi)

        composeRule.onNodeWithText("10\u00a0%", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_waiting_wifi)).assertIsDisplayed()
    }

    // U18
    @Test
    fun modelErrorIsExplainedAndDismissed() {
        setContent(AssistantPreviewStates.downloadError)

        composeRule.onNodeWithText(string(R.string.model_error_verification_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_ok)).performClick()

        assertEquals(listOf("dismiss"), events)
    }

    // U19
    @Test
    fun loadingEngineSaysKaiQIsBeingPrepared() {
        setContent(AssistantPreviewStates.loading)

        composeRule.onNodeWithText("Preparando Kai-Q", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.assistant_preparing_hint)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.assistant_new_conversation)).assertDoesNotExist()
    }

    // U20
    @Test
    fun engineErrorOffersRetryAndCpu() {
        setContent(AssistantPreviewStates.engineError)

        composeRule.onNodeWithText(string(R.string.assistant_retry)).performClick()
        composeRule.onNodeWithText(string(R.string.assistant_use_cpu)).performClick()

        assertEquals(listOf("retry", "useCpu"), events)
    }

    // U20
    @Test
    fun unsupportedEngineOnlyExplains() {
        setContent(AssistantPreviewStates.loading.copy(engine = EngineState.Failed(unsupportedDevice = true, canUseCpu = false)))

        composeRule.onNodeWithText(string(R.string.assistant_engine_unsupported)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.assistant_retry)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.assistant_use_cpu)).assertDoesNotExist()
    }

    // U21
    @Test
    fun emptyChatSuggestsFourQuestionsAndShowsTheMedicalNotice() {
        setContent(AssistantPreviewStates.chatEmpty)
        val suggestions = listOf(
            R.string.assistant_suggestion_protein,
            R.string.assistant_suggestion_breakfast,
            R.string.assistant_suggestion_meals,
            R.string.assistant_suggestion_training,
        ).map { string(it) }

        suggestions.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText(string(R.string.assistant_disclaimer)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.assistant_uses_profile)).assertIsDisplayed()
        composeRule.onNodeWithText(suggestions[0]).performClick()

        assertEquals(listOf("suggestion:¿Cuánta proteína debería tomar al día?"), events)
    }

    // AX1
    @Test
    fun eachSuggestionHasATouchTargetOfAtLeast48Dp() {
        setContent(AssistantPreviewStates.chatEmpty)

        listOf(
            R.string.assistant_suggestion_protein,
            R.string.assistant_suggestion_breakfast,
            R.string.assistant_suggestion_meals,
            R.string.assistant_suggestion_training,
        ).forEach { id ->
            composeRule.onNode(hasClickAction() and hasText(string(id)))
                .assertTouchHeightIsAtLeast(48.dp)
                .assertTouchWidthIsAtLeast(48.dp)
        }
    }

    // AX2
    @Test
    fun chatFieldKeepsItsLabelWhileTyping() {
        var state by mutableStateOf(AssistantPreviewStates.chatEmpty)
        composeRule.setContent {
            KaiFitTheme {
                AssistantContent(
                    state = state,
                    onDownload = {},
                    onImport = {},
                    onCancelModelTransfer = {},
                    onDismissModelError = {},
                    onRetry = {},
                    onUseCpu = {},
                    onInputChange = { state = state.copy(chat = state.chat.copy(input = it)) },
                    onSend = {},
                    onStop = {},
                    onSuggestionClick = {},
                    onNewConversation = {},
                    onNoticeShown = {},
                )
            }
        }
        val label = string(R.string.assistant_input_label)
        composeRule.onNode(hasSetTextAction()).assert(hasText(label))

        composeRule.onNode(hasSetTextAction()).performTextInput("Hola")
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction())
            .assertTextContains("Hola")
            .assert(hasText(label))
    }

    // AX3
    @Test
    fun counterAppearsOnlyNearTheMessageLimit() {
        var state by mutableStateOf(withInput(length = 950))
        composeRule.setContent {
            KaiFitTheme {
                Content(state)
            }
        }

        composeRule.onNodeWithText("950/1000", useUnmergedTree = true).assertIsDisplayed()

        state = withInput(length = 10)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("/1000", substring = true, useUnmergedTree = true).assertCountEquals(0)

        // From 900 characters on.
        state = withInput(length = 899)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("/1000", substring = true, useUnmergedTree = true).assertCountEquals(0)

        state = withInput(length = 900)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("900/1000", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun withInput(length: Int): AssistantUiState =
        AssistantPreviewStates.chatEmpty.let { it.copy(chat = it.chat.copy(input = "a".repeat(length))) }

    // AX4
    @Test
    fun finishedReplyIsAnnouncedOnceByASingleLiveRegion() {
        var state by mutableStateOf(AssistantPreviewStates.chatStreaming)
        composeRule.setContent {
            KaiFitTheme {
                Content(state)
            }
        }
        val liveRegions = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

        // While the reply streams, nothing is announced.
        composeRule.onAllNodes(liveRegions, useUnmergedTree = true).assertCountEquals(0)

        state = state.copy(
            chat = state.chat.copy(
                messages = state.chat.messages.map { message ->
                    if (message.status == MessageStatus.STREAMING) {
                        message.copy(text = message.text + " un yogur.", status = MessageStatus.DONE)
                    } else {
                        message
                    }
                },
                isGenerating = false,
            ),
        )
        composeRule.waitForIdle()

        composeRule.onAllNodes(liveRegions, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNode(liveRegions, useUnmergedTree = true)
            .assertContentDescriptionEquals(string(R.string.assistant_reply_ready))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    // U22
    @Test
    fun whileGeneratingStopReplacesSend() {
        setContent(AssistantPreviewStates.chatStreaming)

        composeRule.onNodeWithContentDescription(string(R.string.assistant_send)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.assistant_stop)).performClick()
        composeRule.onNodeWithText(string(R.string.assistant_disclaimer)).assertIsDisplayed()

        assertEquals(listOf("stop"), events)
    }

    // U23
    @Test
    fun sendIsEnabledOnlyWithText() {
        var state by mutableStateOf(AssistantPreviewStates.chatEmpty)
        composeRule.setContent {
            KaiFitTheme {
                Content(state)
            }
        }
        composeRule.onNodeWithContentDescription(string(R.string.assistant_send)).assertIsNotEnabled()

        state = state.copy(chat = state.chat.copy(input = "¿Qué ceno hoy?"))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(string(R.string.assistant_send)).assertIsEnabled().performClick()
        assertEquals(listOf("send"), events)
    }

    // U24
    @Test
    fun fullConversationAsksForANewOneAndLocksTheField() {
        setContent(AssistantPreviewStates.chatContextFull)

        composeRule.onNodeWithText(string(R.string.assistant_context_full)).assertIsDisplayed()
        // A disabled field has no SetText action, but it keeps its editable text.
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.assistant_new_conversation)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.assistant_new_conversation)).assertIsEnabled()

        assertEquals(listOf("newConversation"), events)
    }

    // U25
    @Test
    fun userBubbleIsOnTheRightAndKaiQBubbleOnTheLeft() {
        setContent(AssistantPreviewStates.chatCpuFallback)
        val question = "¿Cuánta proteína debería tomar al día?"

        val user = composeRule.onNodeWithText(question).getBoundsInRoot()
        val assistant = composeRule.onNodeWithText("Repártelos", substring = true).getBoundsInRoot()

        assertTrue(user.right > assistant.right)
        assertTrue(user.left > assistant.left)
        assertEquals(16.dp, assistant.left)
        composeRule.onNodeWithContentDescription("Tú: $question").assertExists()
        composeRule.onNodeWithText(string(R.string.assistant_cpu_fallback)).assertIsDisplayed()
    }

    // U25
    @Test
    fun stoppedAndFailedRepliesAreMarked() {
        val messages = listOf(
            ChatMessage(0, ChatRole.USER, "hola", MessageStatus.DONE),
            ChatMessage(1, ChatRole.ASSISTANT, "Hola, ¿en qué", MessageStatus.STOPPED),
            ChatMessage(2, ChatRole.USER, "¿Me ayudas?", MessageStatus.DONE),
            ChatMessage(3, ChatRole.ASSISTANT, "Cla", MessageStatus.FAILED),
        )
        setContent(AssistantPreviewStates.chatEmpty.copy(chat = ChatState(messages = messages)))

        composeRule.onNodeWithText(string(R.string.assistant_message_stopped)).assertExists()
        composeRule.onNodeWithText(string(R.string.assistant_message_failed)).assertExists()
    }
}
