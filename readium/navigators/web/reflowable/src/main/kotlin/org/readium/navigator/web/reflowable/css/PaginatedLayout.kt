/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable.css

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import kotlin.math.floor
import kotlin.math.roundToInt
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.r2.shared.ExperimentalReadiumApi

internal data class PaginatedLayout(
    val colCount: Int,
    val lineLength: Dp?,
    val pageGutter: Dp,
)

internal class PaginatedLayoutResolver(
    private val baseMinMargins: Dp,
    private val baseOptimalLineLength: Dp,
    private val baseMinLineLength: Dp,
    private val baseMaxLineLength: Dp,
) {

    fun layout(
        settings: ReflowableWebSettings,
        systemFontScale: Float,
        viewportWidth: Dp,
    ): PaginatedLayout {
        val fontScale = systemFontScale * settings.fontSize.toFloat()
        val minPageGutter =
            baseMinMargins * settings.minMargins.toFloat()
        val optimalLineLength =
            baseOptimalLineLength * settings.optimalLineLength.toFloat() * fontScale
        val minLineLength =
            settings.minimalLineLength?.let { baseMinLineLength * it.toFloat() * fontScale }
        val maxLineLength =
            settings.maximalLineLength?.let { baseMaxLineLength * it.toFloat() * fontScale }

        return when (val colCount = settings.columnCount) {
            null ->
                layoutAuto(
                    minimalPageGutter = minPageGutter,
                    viewportWidth = viewportWidth,
                    optimalLineLength = optimalLineLength,
                    maximalLineLength = maxLineLength,

                )

            else ->
                layoutNColumns(
                    colCount = colCount,
                    minimalPageGutter = minPageGutter,
                    viewportWidth = viewportWidth,
                    maximalLineLength = maxLineLength,
                    minimalLineLength = minLineLength,
                )
        }
    }

    private fun layoutAuto(
        minimalPageGutter: Dp,
        optimalLineLength: Dp,
        viewportWidth: Dp,
        maximalLineLength: Dp?,
    ): PaginatedLayout {
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

        val finalMarginWidth = (viewportWidth - lineLength * colCount)

        val pageGutter = finalMarginWidth / colCount / 2

        return PaginatedLayout(colCount, lineLength, pageGutter)
    }

    private fun layoutNColumns(
        colCount: Int,
        minimalPageGutter: Dp,
        viewportWidth: Dp,
        minimalLineLength: Dp?,
        maximalLineLength: Dp?,
    ): PaginatedLayout {
        val minPageGutter = minimalPageGutter.coerceAtMost(viewportWidth)

        val actualAvailableWidth = viewportWidth - minPageGutter * 2 * colCount

        val minimalLineLength = minimalLineLength?.coerceAtMost(viewportWidth - minPageGutter * 2)

        val maximalLineLength = maximalLineLength?.coerceAtLeast(minPageGutter * 2)

        val lineLength = actualAvailableWidth / colCount

        return when {
            minimalLineLength != null && lineLength < minimalLineLength ->
                layoutNColumns(
                    colCount = colCount - 1,
                    minimalPageGutter = minPageGutter,
                    viewportWidth = viewportWidth,
                    minimalLineLength = minimalLineLength,
                    maximalLineLength = maximalLineLength
                )
            maximalLineLength != null && lineLength > maximalLineLength ->
                layoutNColumns(
                    colCount = colCount + 1,
                    minimalPageGutter = minPageGutter,
                    viewportWidth = viewportWidth,
                    minimalLineLength = minimalLineLength,
                    maximalLineLength = maximalLineLength,
                )
            else ->
                PaginatedLayout(
                    colCount = colCount,
                    lineLength = lineLength,
                    pageGutter = minPageGutter,
                )
        }
    }
}
