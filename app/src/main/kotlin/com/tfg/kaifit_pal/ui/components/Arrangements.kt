package com.tfg.kaifit_pal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Places the items of each line like [Arrangement.SpaceBetween], the first one at the start and
 * the last one at the end, and keeps at least [gap] between them. In a `FlowRow`, an item that
 * does not fit with that gap goes to the next line instead of squeezing the others.
 */
internal fun spaceBetweenWithGap(gap: Dp): Arrangement.Horizontal = object : Arrangement.Horizontal {
    override val spacing: Dp = gap

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        with(Arrangement.SpaceBetween) { arrange(totalSize, sizes, layoutDirection, outPositions) }
    }

    override fun toString(): String = "spaceBetweenWithGap($gap)"
}
