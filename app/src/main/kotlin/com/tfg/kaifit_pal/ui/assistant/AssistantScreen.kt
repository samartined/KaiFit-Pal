package com.tfg.kaifit_pal.ui.assistant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.llm.LlmConfig
import com.tfg.kaifit_pal.data.llm.ModelError
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.llm.UriImportSource
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.ui.components.MessageBubble
import com.tfg.kaifit_pal.ui.components.ModelPreviewStates
import com.tfg.kaifit_pal.ui.components.ModelStatusCard
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing

/**
 * Kai-Q tab. The engine is prepared each time the screen starts and the model file is picked
 * with the system file picker, which needs no storage permission.
 */
@Composable
fun AssistantScreen(
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenStarted()
        onStopOrDispose { viewModel.onScreenStopped() }
    }
    val contentResolver = LocalContext.current.contentResolver
    // ".litertlm" has no registered MIME type, so every file is offered; the header is checked.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onImportModel(UriImportSource(contentResolver, uri))
    }
    AssistantContent(
        state = uiState,
        onDownload = viewModel::onDownload,
        onImport = { importLauncher.launch(arrayOf("*/*")) },
        onCancelModelTransfer = viewModel::onCancelModelTransfer,
        onDismissModelError = viewModel::onDismissModelError,
        onRetry = viewModel::onRetry,
        onUseCpu = viewModel::onUseCpu,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onStop = viewModel::onStop,
        onSuggestionClick = viewModel::onSuggestionClick,
        onNewConversation = viewModel::onNewConversation,
        onNoticeShown = viewModel::onNoticeShown,
        modifier = modifier,
    )
}

/**
 * Stateless Kai-Q screen: the model card until the model is on the device, then the engine
 * loading or its error, and finally the chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantContent(
    state: AssistantUiState,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancelModelTransfer: () -> Unit,
    onDismissModelError: () -> Unit,
    onRetry: () -> Unit,
    onUseCpu: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onNoticeShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val releasedMessage = stringResource(R.string.assistant_notice_engine_released)
    LaunchedEffect(state.notice) {
        if (state.notice == AssistantNotice.ENGINE_RELEASED) {
            snackbarHostState.showSnackbar(releasedMessage)
            // Only once it has been shown: leaving the screen cancels this and keeps the notice.
            onNoticeShown()
        }
    }
    val chatReady = state.model is ModelState.Downloaded && state.engine is EngineState.Ready

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assistant_title), modifier = Modifier.semantics { heading() }) },
                actions = {
                    if (chatReady) {
                        IconButton(onClick = onNewConversation, enabled = state.chat.messages.isNotEmpty()) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_comment),
                                contentDescription = stringResource(R.string.assistant_new_conversation),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        val engine = state.engine
        when {
            state.model !is ModelState.Downloaded -> ScrollableColumn(contentModifier) {
                ModelStatusCard(
                    state = state.model,
                    device = state.device,
                    downloadOnWifiOnly = state.downloadOnWifiOnly,
                    onDownload = onDownload,
                    onImport = onImport,
                    onCancel = onCancelModelTransfer,
                    onDismissError = onDismissModelError,
                )
            }

            engine is EngineState.Ready -> Chat(
                chat = state.chat,
                fellBackToCpu = engine.fellBackToCpu,
                onInputChange = onInputChange,
                onSend = onSend,
                onStop = onStop,
                onSuggestionClick = onSuggestionClick,
                onNewConversation = onNewConversation,
                modifier = contentModifier,
            )

            engine is EngineState.Failed -> ScrollableColumn(contentModifier) {
                EngineError(engine, onRetry = onRetry, onUseCpu = onUseCpu)
            }

            // Loading, or about to load the model that has just been found.
            else -> ScrollableColumn(contentModifier) { Preparing() }
        }
    }
}

@Composable
private fun ScrollableColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
    ) {
        content()
    }
}

@Composable
private fun Preparing() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = stringResource(R.string.assistant_preparing),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.assistant_preparing_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EngineError(engine: EngineState.Failed, onRetry: () -> Unit, onUseCpu: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = stringResource(R.string.assistant_engine_error_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            if (engine.unsupportedDevice) {
                Text(text = stringResource(R.string.assistant_engine_unsupported), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            Text(text = stringResource(R.string.assistant_engine_error_message), style = MaterialTheme.typography.bodyLarge)
            if (engine.canUseCpu) {
                Text(
                    text = stringResource(R.string.assistant_engine_error_cpu_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.assistant_retry))
            }
            if (engine.canUseCpu) {
                OutlinedButton(
                    onClick = onUseCpu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.assistant_use_cpu))
                }
            }
        }
    }
}

@Composable
private fun Chat(
    chat: ChatState,
    fellBackToCpu: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.imePadding()) {
        if (fellBackToCpu) {
            Text(
                text = stringResource(R.string.assistant_cpu_fallback),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (chat.messages.isEmpty()) {
            EmptyChat(
                hasProfile = chat.hasProfile,
                onSuggestionClick = onSuggestionClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            Messages(
                chat = chat,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            ReplyFinishedAnnouncement(chat.messages)
        }
        if (chat.contextFull) ContextFullBanner(onNewConversation)
        Text(
            text = stringResource(R.string.assistant_disclaimer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        InputRow(chat = chat, onInputChange = onInputChange, onSend = onSend, onStop = onStop)
    }
}

@Composable
private fun Messages(chat: ChatState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    // Newest first in a reversed list: the latest message sits at the bottom and grows upwards
    // while it streams, and scrolling back to item 0 brings a new message into view.
    val newestFirst = chat.messages.asReversed()
    LaunchedEffect(chat.messages.size) {
        listState.animateScrollToItem(0)
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.Top),
    ) {
        items(newestFirst, key = { it.id }) { message ->
            MessageBubble(message)
        }
    }
}

/**
 * Tells TalkBack once that Kai-Q has finished its reply, instead of reading every piece of it as
 * it streams: a polite live region that only exists once the last reply goes from streaming to
 * done while the chat is shown. It is 1 dp, because accessibility services skip nodes without size.
 */
@Composable
private fun ReplyFinishedAnnouncement(messages: List<ChatMessage>) {
    val last = messages.lastOrNull()
    // The last reply this composition saw streaming. A plain remember, not a saveable one: a reply
    // that was already done when the chat was composed (back to the tab, a rotation, a recreated
    // activity) is not announced again.
    val streamingReplyId = remember { mutableStateOf<Long?>(null) }
    SideEffect {
        if (last?.role == ChatRole.ASSISTANT && last.status == MessageStatus.STREAMING) {
            streamingReplyId.value = last.id
        }
    }
    if (last?.role != ChatRole.ASSISTANT || last.status != MessageStatus.DONE) return
    if (last.id != streamingReplyId.value) return
    val announcement = stringResource(R.string.assistant_reply_ready)
    Box(
        modifier = Modifier
            .size(1.dp)
            .semantics {
                contentDescription = announcement
                liveRegion = LiveRegionMode.Polite
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChat(hasProfile: Boolean, onSuggestionClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val suggestions = SuggestionRes.map { stringResource(it) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(
            text = stringResource(R.string.assistant_empty_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = stringResource(R.string.assistant_empty_message), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(if (hasProfile) R.string.assistant_uses_profile else R.string.assistant_no_profile),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            suggestions.forEach { suggestion ->
                // The chip keeps its Material height; its touch target already reaches 48 dp.
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion, modifier = Modifier.padding(vertical = Spacing.s)) },
                )
            }
        }
    }
}

@Composable
private fun ContextFullBanner(onNewConversation: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(text = stringResource(R.string.assistant_context_full), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onNewConversation,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(stringResource(R.string.assistant_new_conversation))
            }
        }
    }
}

@Composable
private fun InputRow(chat: ChatState, onInputChange: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.screenHorizontal, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        OutlinedTextField(
            value = chat.input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            enabled = !chat.contextFull,
            // The label stays while typing, so TalkBack always says which field this is.
            label = { Text(stringResource(R.string.assistant_input_label)) },
            placeholder = { Text(stringResource(R.string.assistant_input_hint)) },
            // Near the limit, a counter warns that longer text, such as a long paste, is cut.
            supportingText = if (chat.input.length >= INPUT_COUNTER_FROM_CHARS) {
                {
                    Text(
                        text = stringResource(R.string.assistant_input_counter, chat.input.length, LlmConfig.MAX_INPUT_CHARS),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
            } else {
                null
            },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (chat.canSend) onSend() }),
        )
        if (chat.isGenerating) {
            ChatIconButton(R.drawable.ic_stop, R.string.assistant_stop, enabled = true, onClick = onStop)
        } else {
            ChatIconButton(R.drawable.ic_send, R.string.assistant_send, enabled = chat.canSend, onClick = onSend)
        }
    }
}

@Composable
private fun ChatIconButton(@DrawableRes iconRes: Int, @StringRes descriptionRes: Int, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(painter = painterResource(iconRes), contentDescription = stringResource(descriptionRes))
    }
}

/** Length of the message from which the chat field counts its characters against the limit. */
private const val INPUT_COUNTER_FROM_CHARS = 900

/** The four prompts suggested in an empty chat. */
private val SuggestionRes = listOf(
    R.string.assistant_suggestion_protein,
    R.string.assistant_suggestion_breakfast,
    R.string.assistant_suggestion_meals,
    R.string.assistant_suggestion_training,
)

/** Fixed states for previews and screenshots, with realistic messages in Spanish. */
internal object AssistantPreviewStates {

    private val ready = EngineState.Ready(InferenceBackend.GPU, fellBackToCpu = false)

    val notDownloaded = AssistantUiState(model = ModelState.NotDownloaded, device = ModelPreviewStates.roomyDevice)

    val notDownloadedWithWarnings = AssistantUiState(
        model = ModelState.NotDownloaded,
        device = ModelPreviewStates.crampedDevice,
        downloadOnWifiOnly = false,
    )

    val unsupported = AssistantUiState(model = ModelState.NotDownloaded, device = ModelPreviewStates.unsupportedDevice)

    val downloading = AssistantUiState(model = ModelPreviewStates.downloading, device = ModelPreviewStates.roomyDevice)

    val waitingForWifi = AssistantUiState(model = ModelPreviewStates.waitingForWifi, device = ModelPreviewStates.roomyDevice)

    val importing = AssistantUiState(model = ModelPreviewStates.importing, device = ModelPreviewStates.roomyDevice)

    val downloadError = AssistantUiState(
        model = ModelState.Failed(ModelError.VERIFICATION_FAILED),
        device = ModelPreviewStates.roomyDevice,
    )

    val loading = AssistantUiState(model = ModelPreviewStates.downloaded, engine = EngineState.Loading)

    val engineError = AssistantUiState(
        model = ModelPreviewStates.downloaded,
        engine = EngineState.Failed(unsupportedDevice = false, canUseCpu = true),
    )

    val chatEmpty = AssistantUiState(model = ModelPreviewStates.downloaded, engine = ready, chat = ChatState(hasProfile = true))

    private val twoTurns = listOf(
        ChatMessage(0, ChatRole.USER, "¿Cuánta proteína debería tomar al día?", MessageStatus.DONE),
        ChatMessage(
            1,
            ChatRole.ASSISTANT,
            "Con tu objetivo de déficit, tu plan guardado marca 210 g de proteína al día, unos 2,6 g por kilo. " +
                "Repártelos en 3 o 4 comidas de unos 50 g para que te resulte más fácil llegar.",
            MessageStatus.DONE,
        ),
        ChatMessage(2, ChatRole.USER, "¿Qué puedo desayunar para llegar a esa cantidad?", MessageStatus.DONE),
        ChatMessage(
            3,
            ChatRole.ASSISTANT,
            "Por ejemplo: 200 g de yogur griego natural con avena y fruta, o una tortilla de 2 huevos y 150 g " +
                "de claras con pan integral. Cualquiera de los dos aporta unos 35 a 40 g de proteína.",
            MessageStatus.DONE,
        ),
    )

    val chatStreaming = chatEmpty.copy(
        chat = ChatState(
            messages = twoTurns + listOf(
                ChatMessage(4, ChatRole.USER, "¿Y antes de entrenar por la tarde?", MessageStatus.DONE),
                ChatMessage(
                    5,
                    ChatRole.ASSISTANT,
                    "Una o dos horas antes, toma algo con hidratos y algo de proteína, como un plátano con",
                    MessageStatus.STREAMING,
                ),
            ),
            isGenerating = true,
            hasProfile = true,
        ),
    )

    val chatContextFull = chatEmpty.copy(
        chat = ChatState(messages = twoTurns, contextFull = true, hasProfile = true),
    )

    val chatCpuFallback = chatEmpty.copy(
        engine = EngineState.Ready(InferenceBackend.CPU, fellBackToCpu = true),
        chat = ChatState(messages = twoTurns.take(2), input = "¿Y los días de descanso?", hasProfile = true),
    )
}

@PreviewLightDark
@Composable
private fun AssistantContentNotDownloadedPreview() {
    KaiFitTheme {
        AssistantPreview(AssistantPreviewStates.notDownloaded)
    }
}

@PreviewLightDark
@Composable
private fun AssistantContentChatPreview() {
    KaiFitTheme {
        AssistantPreview(AssistantPreviewStates.chatStreaming)
    }
}

/** [AssistantContent] with no-op callbacks, for previews and screenshots. */
@Composable
internal fun AssistantPreview(state: AssistantUiState) {
    AssistantContent(
        state = state,
        onDownload = {},
        onImport = {},
        onCancelModelTransfer = {},
        onDismissModelError = {},
        onRetry = {},
        onUseCpu = {},
        onInputChange = {},
        onSend = {},
        onStop = {},
        onSuggestionClick = {},
        onNewConversation = {},
        onNoticeShown = {},
    )
}
