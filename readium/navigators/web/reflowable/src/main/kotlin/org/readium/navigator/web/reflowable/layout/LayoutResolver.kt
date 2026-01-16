/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.max
import kotlin.math.floor
import kotlin.math.roundToInt
import org.readium.navigator.web.internals.util.AbsolutePaddingValues
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.r2.shared.ExperimentalReadiumApi

internal data class Layout(
    val colCount: Int,
    val lineLength: Dp,
)

internal class LayoutResolver(
    private val baseMinMargins: Dp,
    private val baseOptimalLineLength: Dp,
    private val baseMinLineLength: Dp,
    private val baseMaxLineLength: Dp,
) {
    fun layout(
        settings: ReflowableWebSettings,
        systemFontScale: Float,
        viewportSize: DpSize,
        safeDrawing: AbsolutePaddingValues,
    ): Layout {
        val optimalLineLength =
            baseOptimalLineLength * settings.optimalLineLength.toFloat() * systemFontScale
        val minLineLength =
            settings.minimalLineLength?.let { baseMinLineLength * it.toFloat() * systemFontScale }
        val maxLineLength =
            settings.maximalLineLength?.let { baseMaxLineLength * it.toFloat() * systemFontScale }
        val minMargins =
            baseMinMargins * settings.minMargins.toFloat() * systemFontScale

        val minMarginsWithInsets = when (settings.verticalText) {
            true -> minMargins.coerceAtLeast(max(safeDrawing.top, safeDrawing.bottom))
            false -> minMargins.coerceAtLeast(max(safeDrawing.left, safeDrawing.right))
        }

        return when (settings.scroll) {
            true ->
                scrolledLayout(
                    viewportSize = if (settings.verticalText) viewportSize.height else viewportSize.width,
                    minimalMargins = minMarginsWithInsets,
                    maximalLineLength = maxLineLength
                )
            false ->
                paginatedLayout(
                    viewportWidth = viewportSize.width,
                    requestedColCount = settings.columnCount,
                    minimalMargins = minMarginsWithInsets,
                    optimalLineLength = optimalLineLength,
                    maximalLineLength = maxLineLength,
                    minimalLineLength = minLineLength
                )
        }
    }

    private fun scrolledLayout(
        viewportSize: Dp,
        minimalMargins: Dp,
        maximalLineLength: Dp?,
    ): Layout {
        val minMargins = minimalMargins.coerceAtMost(viewportSize)

        val availableSize = viewportSize - minMargins * 2

        val maximalLineLength = maximalLineLength?.coerceAtLeast(minMargins * 2)

        val lineLength = availableSize.coerceAtMost(maximalLineLength ?: availableSize)

        return Layout(
            colCount = 1,
            lineLength = lineLength
        )
    }

    private fun paginatedLayout(
        viewportWidth: Dp,
        requestedColCount: Int?,
        minimalMargins: Dp,
        optimalLineLength: Dp,
        maximalLineLength: Dp?,
        minimalLineLength: Dp?,
    ): Layout {
        return when (requestedColCount) {
            null ->
                paginatedLayoutAuto(
                    minimalPageGutter = minimalMargins,
                    viewportWidth = viewportWidth,
                    optimalLineLength = optimalLineLength,
                    maximalLineLength = maximalLineLength,

                )
            else ->
                paginatedLayoutNColumns(
                    colCount = requestedColCount,
                    minimalMargins = minimalMargins,
                    viewportWidth = viewportWidth,
                    maximalLineLength = maximalLineLength,
                    minimalLineLength = minimalLineLength,
                )
        }
    }

    private fun paginatedLayoutAuto(
        minimalPageGutter: Dp,
        optimalLineLength: Dp,
        viewportWidth: Dp,
        maximalLineLength: Dp?,
    ): Layout {
        /*
         * marginWidth = 2 * pageGutter * colCount
         * colCount = (viewportWidth - marginWidth) / optimalLineLength
         *
         * resolves to marginWidth = (2 * pageGutter * viewportWidth / optimalLineLength) / (2 * pageGutter / optimalLineLength + 1)
         */

        val optimalLineLength =
            optimalLineLength.coerceAtMost(viewportWidth)

        val minMarginWidthWithFloatingColCount =
            (minimalPageGutter * 2 * (viewportWidth / optimalLineLength)) / (1 + minimalPageGutter * 2 / optimalLineLength)

        val colCount =
            floor((viewportWidth - minMarginWidthWithFloatingColCount) / optimalLineLength)
                .roundToInt()
                .coerceAtLeast(1)

        val minMarginWidth = minimalPageGutter * 2 * colCount

        val lineLength = ((viewportWidth - minMarginWidth) / colCount)
            .let { it.coerceAtMost(maximalLineLength ?: it) }
            .coerceAtMost((viewportWidth - minMarginWidth) / colCount)

        return Layout(colCount, lineLength)
    }

    private fun paginatedLayoutNColumns(
        colCount: Int,
        minimalMargins: Dp,
        viewportWidth: Dp,
        minimalLineLength: Dp?,
        maximalLineLength: Dp?,
    ): Layout {
        val minPageGutter = minimalMargins.coerceAtMost(viewportWidth)

        val actualAvailableWidth = viewportWidth - minPageGutter * 2 * colCount

        val minimalLineLength = minimalLineLength?.coerceAtMost(actualAvailableWidth)

        val maximalLineLength = maximalLineLength?.coerceAtLeast(minPageGutter * 2)

        val lineLength = actualAvailableWidth / colCount

        return when {
            minimalLineLength != null && lineLength < minimalLineLength ->
                paginatedLayoutNColumns(
                    colCount = colCount - 1,
                    minimalMargins = minPageGutter,
                    viewportWidth = viewportWidth,
                    minimalLineLength = minimalLineLength,
                    maximalLineLength = maximalLineLength
                )
            maximalLineLength != null && lineLength > maximalLineLength ->
                Layout(
                    colCount = colCount,
                    lineLength = maximalLineLength,
                )
            else ->
                Layout(
                    colCount = colCount,
                    lineLength = lineLength,
                )
        }
    }
}
