package com.tfg.kaifit_pal.testing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Asserts that the touch target of the node, which may be larger than what it draws, is at least
 * [expected] tall. Like the `assertTouch…IsEqualTo` assertions of Compose, it allows half a dp
 * for rounding.
 */
fun SemanticsNodeInteraction.assertTouchHeightIsAtLeast(expected: Dp): SemanticsNodeInteraction = apply {
    val node = fetchSemanticsNode()
    val height = node.touchSize { height }
    if (height < expected - TOLERANCE) {
        throw AssertionError("Touch height $height of ${node.describe()} is below $expected")
    }
}

/** Asserts that the touch target of the node is at least [expected] wide; see [assertTouchHeightIsAtLeast]. */
fun SemanticsNodeInteraction.assertTouchWidthIsAtLeast(expected: Dp): SemanticsNodeInteraction = apply {
    val node = fetchSemanticsNode()
    val width = node.touchSize { width }
    if (width < expected - TOLERANCE) {
        throw AssertionError("Touch width $width of ${node.describe()} is below $expected")
    }
}

private val TOLERANCE = 0.5.dp

private fun SemanticsNode.touchSize(side: Rect.() -> Float): Dp =
    with(layoutInfo.density) { touchBoundsInRoot.side().toDp() }

private fun SemanticsNode.describe(): String = "node #$id (${config.joinToString { "${it.key.name}=${it.value}" }})"
