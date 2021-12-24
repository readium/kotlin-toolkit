/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.presentation

import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap

@ExperimentalPresentation
interface PresentationConstraints<V> {
    fun validate(value: V): Try<Unit, UserException> = Try.success(Unit)
    fun isActiveForValues(values: PresentationValues): Boolean = true
    fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> = Try.success(values)

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value
     * and each constraints from outside in.
     */
    fun <R> fold(initial: R, operation: (R, PresentationConstraints<V>) -> R): R = operation(initial, this)

    companion object {
        operator fun <V> invoke(): PresentationConstraints<V> = object : PresentationConstraints<V> {}
    }
}

@ExperimentalPresentation
class PresentationRangeConstraints(
    val stepCount: Int? = null,
) : PresentationConstraints<PresentationRange>

@ExperimentalPresentation
val PresentationConstraints<PresentationRange>.stepCount: Int? get() =
    fold<Int?>(null) { acc, constraints ->
        (constraints as? PresentationRangeConstraints)?.stepCount ?: acc
    }

@ExperimentalPresentation
val PresentationConstraints<PresentationRange>.step: Double?
    get() = stepCount?.takeIf { it > 0 }?.let { 1.0 / it }

@ExperimentalPresentation
class PresentationEnumConstraints<E : Enum<E>>(
    val supportedValues: List<E>? = null
) : PresentationConstraints<E> {

    override fun validate(value: E): Try<Unit, UserException> {
        if (supportedValues?.contains(value) == false) {
            return Try.failure(UserException(R.string.readium_presentation_valueNotSupportedError))
        }
        return super.validate(value)
    }
}

@ExperimentalPresentation
val <E: Enum<E>> PresentationConstraints<E>.supportedValues: List<E>? get() =
    fold<List<E>?>(null) { acc, constraints ->
        (constraints as? PresentationEnumConstraints)?.supportedValues ?: acc
    }

@ExperimentalPresentation
class PresentationCombinedConstraints<V>(
    private val outer: PresentationConstraints<V>,
    private val inner: PresentationConstraints<V>,
) : PresentationConstraints<V> {

    override fun validate(value: V): Try<Unit, UserException> =
        inner.validate(value).flatMap { outer.validate(value) }

    override fun isActiveForValues(values: PresentationValues): Boolean =
        inner.isActiveForValues(values) && outer.isActiveForValues(values)

    override fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> =
        inner.activateInValues(values)
            .flatMap { outer.activateInValues(it) }

    override fun <R> fold(initial: R, operation: (R, PresentationConstraints<V>) -> R): R =
        inner.fold(outer.fold(initial, operation), operation)
}

@ExperimentalPresentation
operator fun <V> PresentationConstraints<V>.plus(other: PresentationConstraints<V>): PresentationConstraints<V> =
    PresentationCombinedConstraints(this, other)

@ExperimentalPresentation
class PresentationDependencyConstraints<V>(
    private val requiredValues: PresentationValues,
) : PresentationConstraints<V> {

    override fun isActiveForValues(values: PresentationValues): Boolean {
        val requiredVals = requiredValues.values.filterValues { it != null }
        for ((key, value) in requiredVals) {
            if (value != values.values[key]) {
                return false
            }
        }
        return true
    }

    override fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> =
        Try.success(values.merge(requiredValues))
}

@ExperimentalPresentation
fun <V> PresentationConstraints<V>.require(values: PresentationValues): PresentationConstraints<V> =
    this + PresentationDependencyConstraints(values)
