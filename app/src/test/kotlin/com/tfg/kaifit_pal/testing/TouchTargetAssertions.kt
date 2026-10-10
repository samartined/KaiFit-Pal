package com.tfg.kaifit_pal.testing

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

/** The minimum touch target of Material and of the Android accessibility guidelines. */
val MinTouchTarget: Dp = 48.dp

/** Rounding allowance, as in the `assertTouch…IsEqualTo` assertions of Compose. */
private val TOLERANCE = 0.5.dp

/**
 * Size of the touch target of this node: the smaller of the bounds that accessibility services
 * get and the space that the element takes in the layout.
 *
 * Both count. Compose reports clickable nodes to accessibility services at 48 dp at least, even
 * when they take less room, so only the layout shows a 32 dp clickable box; and the reverse
 * happens with a slider, which takes 48 dp but whose node may report only its 44 dp track.
 */
fun SemanticsNode.touchTargetSize(): DpSize = with(layoutInfo.density) {
    DpSize(
        width = min(touchBoundsInRoot.width.toDp(), layoutInfo.width.toDp()),
        height = min(touchBoundsInRoot.height.toDp(), layoutInfo.height.toDp()),
    )
}

/** Whether both sides of [touchTargetSize] reach [MinTouchTarget]. */
fun SemanticsNode.hasMinimumTouchTarget(): Boolean {
    val size = touchTargetSize()
    return size.width >= MinTouchTarget - TOLERANCE && size.height >= MinTouchTarget - TOLERANCE
}

/** The size of its touch target and what the node says, for failure messages. */
fun SemanticsNode.describeTouchTarget(): String {
    val size = touchTargetSize()
    val text = config.getOrElse(SemanticsProperties.Text) { emptyList() }.joinToString()
    val description = config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.joinToString()
    return "${size.width} × ${size.height}: text=\"$text\" description=\"$description\""
}

/** Asserts that the touch target of the node, see [touchTargetSize], is at least [expected] tall. */
fun SemanticsNodeInteraction.assertTouchHeightIsAtLeast(expected: Dp): SemanticsNodeInteraction = apply {
    val node = fetchSemanticsNode()
    if (node.touchTargetSize().height < expected - TOLERANCE) {
        throw AssertionError("Touch target below $expected tall: ${node.describeTouchTarget()}")
    }
}

/** Asserts that the touch target of the node, see [touchTargetSize], is at least [expected] wide. */
fun SemanticsNodeInteraction.assertTouchWidthIsAtLeast(expected: Dp): SemanticsNodeInteraction = apply {
    val node = fetchSemanticsNode()
    if (node.touchTargetSize().width < expected - TOLERANCE) {
        throw AssertionError("Touch target below $expected wide: ${node.describeTouchTarget()}")
    }
}
