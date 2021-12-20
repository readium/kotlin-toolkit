/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.presentation

import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.extensions.merge
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.ValueCoder
import org.readium.r2.shared.util.getOrElse
import timber.log.Timber

typealias PresentationToggle = Boolean
typealias PresentationRange = Double

@ExperimentalPresentation
@Parcelize
@Suppress("unused") // V and R are not unused, PresentationKey is a phantom type.
data class PresentationKey<V, R>(val key: String) : Parcelable {
    companion object {
        val CONTINUOUS = PresentationToggleKey("continuous")
        val FIT = PresentationEnumKey<Fit>("fit")
        val ORIENTATION = PresentationEnumKey<Orientation>("orientation")
        val OVERFLOW = PresentationEnumKey<Overflow>("overflow")
        val PAGE_SPACING = PresentationRangeKey("pageSpacing")
        val READING_PROGRESSION = PresentationEnumKey<ReadingProgression>("readingProgression")
    }

    override fun toString(): String = key
}

@ExperimentalPresentation
typealias PresentationToggleKey = PresentationKey<PresentationToggle, Boolean>

@ExperimentalPresentation
typealias PresentationRangeKey = PresentationKey<PresentationRange, Double>

@ExperimentalPresentation
typealias PresentationEnumKey<V> = PresentationKey<V, String>

@ExperimentalPresentation
typealias AnyPresentationKey = PresentationKey<*, *>

/**
 * Holds a list of key-value pairs provided by the app to influence a Navigator's Presentation
 * Properties. The keys must be valid Presentation Property Keys.
 */
@ExperimentalPresentation
@Parcelize
data class PresentationValues(val values: @WriteWith<PresentationValuesParceler> Map<AnyPresentationKey, Any?> = emptyMap()) : Parcelable, JSONable {

    constructor(vararg values: Pair<AnyPresentationKey, Any?>) : this(mapOf(*values))

    constructor(
        continuous: Boolean? = null,
        fit: Fit? = null,
        orientation: Orientation? = null,
        overflow: Overflow? = null,
        pageSpacing: Double? = null,
        readingProgression: ReadingProgression? = null
    ) : this(
        PresentationKey.CONTINUOUS to continuous,
        PresentationKey.FIT to fit?.value,
        PresentationKey.ORIENTATION to orientation?.value,
        PresentationKey.OVERFLOW to overflow?.value,
        PresentationKey.PAGE_SPACING to pageSpacing,
        PresentationKey.READING_PROGRESSION to readingProgression?.value,
    )

    inline operator fun <reified R> get(key: PresentationKey<*, R>): R? =
        values[key] as? R

    inline operator fun <reified V, reified R> get(key: PresentationKey<V, R>, coder: ValueCoder<V?, R?>): V? =
        coder.decode(get(key))

    val continuous: Boolean?
        get() = get(PresentationKey.CONTINUOUS)

    val fit: Fit?
        get() = get(PresentationKey.FIT, Fit)

    val orientation: Orientation?
        get() = get(PresentationKey.ORIENTATION, Orientation)

    val overflow: Overflow?
        get() = get(PresentationKey.OVERFLOW, Overflow)

    val pageSpacing: Double?
        get() = get(PresentationKey.PAGE_SPACING)

    val readingProgression: ReadingProgression?
        get() = get(PresentationKey.READING_PROGRESSION, ReadingProgression)

    /**
     * Returns a copy of this object after modifying the settings in the given closure.
     */
    fun copy(transform: MutableMap<AnyPresentationKey, Any?>.() -> Unit): PresentationValues =
        PresentationValues(values.toMutableMap().apply(transform).toMap())

    /**
     * Returns a copy of this object after overwriting any setting with the values from [other].
     */
    fun merge(other: PresentationValues): PresentationValues =
        PresentationValues(
            (other.values.entries + values.entries)
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, value) -> value.firstOrNull { it != null } }
        )

    override fun toJSON(): JSONObject =
        JSONObject(values.filterValues { it != null }.mapKeys { it.key.key })

    companion object {

        fun fromJSON(json: JSONObject?): PresentationValues {
            val values: Map<AnyPresentationKey, Any>? =
                json?.toMap()?.mapKeys { AnyPresentationKey(it.key) }
            return PresentationValues(values ?: emptyMap())
        }
    }
}

/**
 * Implementation of a [Parceler] to be used with [@Parcelize] to serialize [PresentationValues].
 */
@ExperimentalPresentation
object PresentationValuesParceler : Parceler<Map<AnyPresentationKey, Any?>> {

    override fun create(parcel: Parcel): Map<AnyPresentationKey, Any?> =
        try {
            parcel.readString()?.let {
                JSONObject(it).toMap()
                    .mapKeys { pair -> AnyPresentationKey(pair.key) }
            } ?: emptyMap()
        } catch (e: Exception) {
            Timber.e(e, "Failed to read a PresentationValues from a Parcel")
            emptyMap()
        }

    override fun Map<AnyPresentationKey, Any?>.write(parcel: Parcel, flags: Int) {
        try {
            parcel.writeString(JSONObject(mapKeys { it.key.key }).toString())
        } catch (e: Exception) {
            Timber.e(e, "Failed to write a PresentationValues into a Parcel")
        }
    }
}

interface PresentableNavigator : Navigator {

    @ExperimentalPresentation
    val presentation: StateFlow<Presentation>

    /**
     * Submits a new set of Presentation Settings used by the Navigator to recompute its
     * Presentation Properties.
     *
     * Note that the Navigator might not update its presentation right away, or might even ignore
     * some of the provided settings. They are only used as guidelines to compute the Presentation
     * Properties.
     */
    @ExperimentalPresentation
    suspend fun applyPresentationSettings(settings: PresentationValues) {}
}

@ExperimentalPresentation
interface Presentation {
    val values: PresentationValues

    fun <V> constraintsForKey(key: PresentationKey<V, *>): PresentationValueConstraints<V>?
}

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

    override fun validate(value: Double): Boolean =
        (0.0..1.0).contains(value)
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
