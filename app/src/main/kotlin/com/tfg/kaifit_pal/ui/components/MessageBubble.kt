package com.tfg.kaifit_pal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.assistant.ChatMessage
import com.tfg.kaifit_pal.ui.assistant.ChatRole
import com.tfg.kaifit_pal.ui.assistant.MessageStatus
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing

/**
 * One chat message: the user's on the right on `primaryContainer`, Kai-Q's on the left on
 * `surfaceContainerHigh`, at most 85 % of the width. The text can be selected and copied, and a
 * screen reader announces who wrote it. A stopped or failed reply gets a note below.
 */
@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val fromUser = message.role == ChatRole.USER
    val alignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val spokenText = stringResource(
        if (fromUser) R.string.assistant_message_from_user else R.string.assistant_message_from_assistant,
        message.text,
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.fillMaxWidth(BUBBLE_MAX_WIDTH_FRACTION),
            horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Surface(
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spokenText },
                shape = MaterialTheme.shapes.medium,
                color = if (fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (message.text.isEmpty() && message.status == MessageStatus.STREAMING) {
                    val typing = stringResource(R.string.assistant_typing)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(Spacing.m)
                            .size(20.dp)
                            .semantics { contentDescription = typing },
                        strokeWidth = 2.dp,
                    )
                } else {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            val note = when (message.status) {
                MessageStatus.STOPPED -> R.string.assistant_message_stopped
                MessageStatus.FAILED -> R.string.assistant_message_failed
                MessageStatus.DONE, MessageStatus.STREAMING -> null
            }
            if (note != null) {
                Text(
                    text = stringResource(note),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.status == MessageStatus.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

private const val BUBBLE_MAX_WIDTH_FRACTION = 0.85f

@PreviewLightDark
@Composable
private fun MessageBubblePreview() {
    KaiFitTheme {
        Surface {
            Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                MessageBubble(ChatMessage(1, ChatRole.USER, "¿Cuánta proteína debería tomar al día?", MessageStatus.DONE))
                MessageBubble(
                    ChatMessage(
                        2,
                        ChatRole.ASSISTANT,
                        "Con tu peso, entre 1,6 y 2 g por kilo al día: unos 130 a 160 g.",
                        MessageStatus.STOPPED,
                    ),
                )
            }
        }
    }
}
