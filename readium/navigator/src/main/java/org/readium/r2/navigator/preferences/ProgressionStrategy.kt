/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.extensions.equalsDelta

/**
 * A strategy to increment or decrement a setting.
 */
interface ProgressionStrategy<V> {

    fun increment(value: V): V

    fun decrement(value: V): V
}

/**
 * Progression strategy based on a list of preferred values for the setting.
 *
 * Steps MUST be sorted in increasing order.
 *
 * @param equalsDelta Provide an equality algorithm to compare floating point numbers.
 */
class StepsProgression<T : Comparable<T>>(
    private val steps: List<T>,
    private val equalsDelta: (T, T) -> Boolean
) : ProgressionStrategy<T> {

    companion object {
        operator fun invoke(vararg steps: Int): StepsProgression<Int> =
            StepsProgression(steps = steps.toList(), equalsDelta = Int::equals)

        operator fun invoke(vararg steps: Float): StepsProgression<Float> =
            StepsProgression(steps = steps.toList(), equalsDelta = { a, b -> a.equalsDelta(b) })

        operator fun invoke(vararg steps: Double): StepsProgression<Double> =
            StepsProgression(steps = steps.toList(), equalsDelta = { a, b -> a.equalsDelta(b) })
    }

    override fun increment(value: T): T {
        val index = steps.indexOfLast { it < value || equalsDelta(it, value) }.takeIf { it != -1 } ?: return value
        return steps.getOrNull(index + 1) ?: value
    }

    override fun decrement(value: T): T {
        val index = steps.indexOfFirst { it > value || equalsDelta(it, value) }.takeIf { it != -1 } ?: return value
        return steps.getOrNull(index - 1) ?: value
    }
}

/**
 * Simple progression strategy which increments or decrements the setting
 * by a fixed number.
 */
class IntIncrement(private val increment: Int) : ProgressionStrategy<Int> {

    override fun increment(value: Int): Int = value + increment

    override fun decrement(value: Int): Int = value - increment
}

/**
 * Simple progression strategy which increments or decrements the setting
 * by a fixed number.
 */
class DoubleIncrement(private val increment: Double) : ProgressionStrategy<Double> {

    override fun increment(value: Double): Double = value + increment

    override fun decrement(value: Double): Double = value - increment
}
