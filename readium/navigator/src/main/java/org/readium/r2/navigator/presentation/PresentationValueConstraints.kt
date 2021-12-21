/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.presentation

import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.extensions.merge
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

@ExperimentalPresentation
interface PresentationValueConstraints<V> {
    val extras: Map<String, Any?>
    fun validate(value: V): Boolean = true
    fun isActiveForValues(values: PresentationValues): Boolean = true
    fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> = Try.success(values)
}

@ExperimentalPresentation
class PresentationToggleConstraints(
    override val extras: Map<String, Any?> = emptyMap()
) : PresentationValueConstraints<PresentationToggle>

@ExperimentalPresentation
class PresentationRangeConstraints(
    stepCount: Int? = null,
    extras: Map<String, Any?> = emptyMap()
) : PresentationValueConstraints<PresentationRange> {

    override val extras: Map<String, Any?> =
        extras.merge("stepCount" to stepCount)

    override fun validate(value: PresentationRange): Boolean =
        (0.0..1.0).contains(value.double)
}

@ExperimentalPresentation
val PresentationValueConstraints<PresentationRange>.stepCount: Int?
    get() = extras["stepCount"] as? Int

@ExperimentalPresentation
val PresentationValueConstraints<PresentationRange>.step: Double?
    get() = stepCount?.takeIf { it > 0 }?.let { 1.0 / it }

@ExperimentalPresentation
class PresentationEnumConstraints<E : Enum<E>>(
    supportedValues: List<E>? = null,
    extras: Map<String, Any?> = emptyMap()
) : PresentationValueConstraints<E> {

    override val extras: Map<String, Any?> =
        extras.merge("supportedValues" to supportedValues)

    override fun validate(value: E): Boolean =
        supportedValues?.contains(value) ?: true
}

@ExperimentalPresentation
val <E: Enum<E>> PresentationValueConstraints<E>.supportedValues: List<E>?
    get() = extras["supportedValues"] as? List<E>

@ExperimentalPresentation
class PresentationValueCompositeConstraints<V>(
    private vararg val constraints: PresentationValueConstraints<V>
) : PresentationValueConstraints<V> {

    override val extras: Map<String, Any?> get() =
        constraints.fold(emptyMap()) { extras, c ->
            extras.merge(c.extras)
        }

    override fun validate(value: V): Boolean =
        constraints.all { it.validate(value) }

    override fun isActiveForValues(values: PresentationValues): Boolean =
        constraints.all { it.isActiveForValues(values) }

    override fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> {
        var res = values
        for (c in constraints) {
            res = c.activateInValues(res)
                .getOrElse { return Try.failure(it) }
        }
        return Try.success(res)
    }
}

@ExperimentalPresentation
operator fun <V> PresentationValueConstraints<V>.plus(other: PresentationValueConstraints<V>): PresentationValueConstraints<V> =
    PresentationValueCompositeConstraints(this, other)

@ExperimentalPresentation
class PresentationValueDependencyConstraints<V>(
    private val requiredValues: PresentationValues,
    override val extras: Map<String, Any?> = emptyMap()
) : PresentationValueConstraints<V> {

    override fun isActiveForValues(values: PresentationValues): Boolean {
        val requiredVals = requiredValues.values.filterValues { it != null }
        for ((key, value) in requiredVals) {
            if (value != values.values[key]) {
                return false
            }
        }
        return true
    }

    override fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> {
        return Try.success(values.merge(requiredValues))
    }
}

@ExperimentalPresentation
fun <V> PresentationValueConstraints<V>.require(values: PresentationValues): PresentationValueConstraints<V> =
    this + PresentationValueDependencyConstraints(values)
