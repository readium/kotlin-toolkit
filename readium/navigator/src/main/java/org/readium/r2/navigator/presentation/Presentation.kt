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
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.ValueCoder
import timber.log.Timber
import java.text.NumberFormat

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
interface Presentation2 {
    val values: PresentationValues

    fun <V> validateValue(key: PresentationKey<V, *>, value: V): Boolean = true
    fun isActiveForValues(key: PresentationKey<*, *>, values: PresentationValues): Boolean = true
    fun activateInValues(key: PresentationKey<*, *>, values: PresentationValues): Try<PresentationValues, UserException> = Try.success(values)
    fun stepCountForKey(key: PresentationKey<*, *>): Int? = null
    fun <E: Enum<E>>supportedValuesForKey(key: PresentationKey<E, *>): List<E>? = null
}

@ExperimentalPresentation
data class PresentationKey<V, R>(
    val key: String,
    private val coder: ValueCoder<V?, R?>
) : ValueCoder<V?, R?> by coder {

    override fun toString(): String = key

    companion object {
        val CONTINUOUS = PresentationKey("continuous", PresentationToggle)
        val FIT = PresentationKey("fit", Fit)
        val ORIENTATION = PresentationKey("orientation", Orientation)
        val OVERFLOW = PresentationKey("overflow", Overflow)
        val PAGE_SPACING = PresentationKey("pageSpacing", PresentationRange)
        val READING_PROGRESSION = PresentationKey("readingProgression", ReadingProgression)
    }
}

@ExperimentalPresentation
@JvmInline
value class PresentationToggle(val bool: Boolean) {

    companion object : ValueCoder<PresentationToggle?, Boolean?> {
        override fun encode(value: PresentationToggle?): Boolean? = value?.bool
        override fun decode(rawValue: Boolean?): PresentationToggle? = rawValue?.let { PresentationToggle(it) }
    }

    fun toggle(): PresentationToggle =
        PresentationToggle(!bool)
}

@ExperimentalPresentation
@JvmInline
value class PresentationRange private constructor(val double: Double) {
    companion object : ValueCoder<PresentationRange?, Double?> {
        operator fun invoke(double: Double): PresentationRange =
            PresentationRange(double.coerceIn(0.0, 1.0))

        override fun encode(value: PresentationRange?): Double? = value?.double
        override fun decode(rawValue: Double?): PresentationRange? = rawValue?.let { invoke(it) }
    }

    /**
     * Formats the range into a localized percentage string, e.g. "42%".
     */
    val percentageString: String get() =
        NumberFormat.getPercentInstance().run {
            maximumFractionDigits = 0
            format(double)
        }
}

/**
 * Holds a list of key-value pairs provided by the app to influence a Navigator's Presentation
 * Properties. The keys must be valid Presentation Property Keys.
 */
@ExperimentalPresentation
@Parcelize
data class PresentationValues(val values: @WriteWith<PresentationValuesParceler> Map<String, Any?> = emptyMap()) : Parcelable, JSONable {

    constructor(vararg values: Pair<PresentationKey<*, *>, Any?>) : this(mapOf(*values).mapKeys { it.key.key })

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

    inline operator fun <reified V, reified R> get(key: PresentationKey<V, R>): V? =
        key.decode(values[key.key] as? R)

    val continuous: PresentationToggle?
        get() = get(PresentationKey.CONTINUOUS)

    val fit: Fit?
        get() = get(PresentationKey.FIT)

    val orientation: Orientation?
        get() = get(PresentationKey.ORIENTATION)

    val overflow: Overflow?
        get() = get(PresentationKey.OVERFLOW)

    val pageSpacing: PresentationRange?
        get() = get(PresentationKey.PAGE_SPACING)

    val readingProgression: ReadingProgression?
        get() = get(PresentationKey.READING_PROGRESSION)

    /**
     * Returns a copy of this object after modifying the settings in the given closure.
     */
    fun copy(transform: MutableMap<String, Any?>.() -> Unit): PresentationValues =
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
        JSONObject(values.filterValues { it != null })

    companion object {

        fun fromJSON(json: JSONObject?): PresentationValues =
            PresentationValues(json?.toMap() ?: emptyMap())
    }
}

/**
 * Implementation of a [Parceler] to be used with [@Parcelize] to serialize [PresentationValues].
 */
@ExperimentalPresentation
object PresentationValuesParceler : Parceler<Map<String, Any?>> {

    override fun create(parcel: Parcel): Map<String, Any?> =
        try {
            parcel.readString()?.let {
                JSONObject(it).toMap()
            } ?: emptyMap()
        } catch (e: Exception) {
            Timber.e(e, "Failed to read a PresentationValues from a Parcel")
            emptyMap()
        }

    override fun Map<String, Any?>.write(parcel: Parcel, flags: Int) {
        try {
            parcel.writeString(JSONObject(this).toString())
        } catch (e: Exception) {
            Timber.e(e, "Failed to write a PresentationValues into a Parcel")
        }
    }
}
