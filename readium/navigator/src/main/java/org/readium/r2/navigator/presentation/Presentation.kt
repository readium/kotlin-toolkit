/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.presentation

import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.util.ValueCoder
import java.text.NumberFormat

@ExperimentalPresentation
interface PresentableNavigator : Navigator {

    val presentationProperties: StateFlow<PresentationProperties>

    /**
     * Submits a new set of Presentation Settings used by the Navigator to recompute its
     * Presentation Properties.
     *
     * Note that the Navigator might not update its presentation right away, or might even ignore
     * some of the provided settings. They are only used as guidelines to compute the Presentation
     * Properties.
     */
    suspend fun applyPresentationSettings(settings: PresentationValues)
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

@ExperimentalPresentation
data class PresentationProperties(
    val properties: Set<PresentationProperty<*, *>>
) {
    constructor(vararg properties: PresentationProperty<*, *>) : this(setOf(*properties))

    val keys: Set<PresentationKey<*, *>> get() = properties.map { it.key }.toSet()

    val values: PresentationValues get() = PresentationValues(
        properties.associate { prop ->
            prop.key.key to prop.encodedValue
        }
    )

    operator fun <T, R> get(key: PresentationKey<T, R>): PresentationProperty<T, R>? =
        properties.firstOrNull { it.key == key } as PresentationProperty<T, R>?
}

@ExperimentalPresentation
data class PresentationProperty<T, R>(
    val key: PresentationKey<T, R>,
    val value: T,
    val constraints: PresentationConstraints<T>? = null
) {
    val encodedValue: R? get() = key.encode(value)
}

/**
 * Holds a list of key-value pairs provided by the app to influence a Navigator's Presentation
 * Properties. The keys must be valid Presentation Property Keys.
 */
@ExperimentalPresentation
data class PresentationValues(val values: Map<String, Any?> = emptyMap()) : JSONable {

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
