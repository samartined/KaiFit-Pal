package com.tfg.kaifit_pal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.theme.Spacing

/**
 * A question that shows its answer when tapped, as in a list of frequently asked questions.
 *
 * The whole header, at least 48 dp tall, is one button that tells accessibility services whether
 * the answer is shown. The chevron turns over and the answer slides in and out.
 */
@Composable
fun ExpandableCard(
    question: String,
    answer: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")
    val state = stringResource(if (expanded) R.string.expandable_expanded else R.string.expandable_collapsed)
    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                    .semantics { stateDescription = state }
                    .padding(horizontal = Spacing.l, vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = question,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.s),
                    style = MaterialTheme.typography.titleSmall,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    // The state description already says whether the answer is shown.
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = answer,
                    modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.l),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
