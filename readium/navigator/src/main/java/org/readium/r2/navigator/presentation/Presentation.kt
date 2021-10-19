package org.readium.r2.navigator.presentation

import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Try

data class PresentationKey(val key: String) {
    companion object {
        val CONTINUOUS = PresentationKey("continuous")
        val READING_PROGRESSION = PresentationKey("readingProgression")
    }

    override fun toString(): String = key
}

/**
 * Holds the current values for the Presentation Properties determining how a publication is
 * rendered by a Navigator. For example, "font size" or "playback rate".
 */
data class Presentation(
    val properties: Map<PresentationKey, Property<*>?> = emptyMap()
) {
    val continuous: ToggleProperty? get() =
        properties[PresentationKey.CONTINUOUS] as? ToggleProperty

    val readingProgression: EnumProperty<ReadingProgression>? get() =
        properties[PresentationKey.READING_PROGRESSION] as? EnumProperty<ReadingProgression>

    /**
     * Holds the current value and the metadata of a Presentation Property of type [T].
     */
    interface Property<T> {
        /**
         * Current value for the property.
         */
        val value: T

        /**
         * Determines whether the property will be active when the given settings are applied to the
         * Navigator.
         *
         * For example, with an EPUB Navigator using Readium CSS, the property "letter spacing" requires
         * to switch off the "publisher defaults" setting to be active.
         *
         * This is useful to determine whether to grey out a view in the user settings interface.
         */
        fun isActiveForSettings(settings: PresentationSettings): Boolean

        /**
         * Modifies the given settings to make sure the property will be activated when applying them to
         * the Navigator.
         *
         * For example, with an EPUB Navigator using Readium CSS, activating the "letter spacing"
         * property means ensuring the "publisher defaults" setting is disabled.
         *
         * If the property cannot be activated, returns a user-facing localized error.
         */
        fun activateInSettings(settings: PresentationSettings): Try<PresentationSettings, Exception>
    }

    /**
     * Property representable as a toggle switch in the user interface. For example,
     * "publisher defaults" or "continuous".
     */
    class ToggleProperty(
        value: Boolean,
        isActiveForSettings: (PresentationSettings) -> Boolean = { true },
        activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception> = { Try.Success(it) }
    ) : BaseProperty<Boolean>(value, isActiveForSettings, activateInSettings)

    /**
     * Property representable as a dropdown menu or radio buttons group in the user interface. For
     * example, "reading progression" or "font family".
     *
     * @param supportedValues List of values supported by this navigator, in logical order. Null if
     *        any value is supported.
     */
    class StringProperty(
        value: String,
        val supportedValues: List<String>?,
        isActiveForSettings: (PresentationSettings) -> Boolean = { true },
        activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception> = { Try.success(it) },
        private val labelForValue: (String) -> String = { it },
    ) : BaseProperty<String>(value, isActiveForSettings, activateInSettings) {

        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "reading progression" property, the value ltr has for label "Left to
         * right" in English.
         */
        fun labelForValue(value: String): String =
            labelForValue.invoke(value)
    }

    /**
     * Property representable as a dropdown menu or radio buttons group in the user interface. For
     * example, "reading progression" or "font family".
     */
    class EnumProperty<T : Enum<T>>(
        value: T,
        isActiveForSettings: (PresentationSettings) -> Boolean = { true },
        activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception> = { Try.success(it) },
        private val labelForValue: (T) -> String = { it.name },
    ) : BaseProperty<T>(
        value,
        isActiveForSettings, activateInSettings
    ) {
        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "reading progression" property, the value ltr has for label "Left to
         * right" in English.
         */
        fun labelForValue(value: T): String =
            labelForValue.invoke(value)
    }

    abstract class BaseProperty<T>(
        override val value: T,
        private val isActiveForSettings: (PresentationSettings) -> Boolean,
        private val activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception>,
    ) : Property<T> {

        override fun isActiveForSettings(settings: PresentationSettings): Boolean =
            isActiveForSettings.invoke(settings)

        override fun activateInSettings(settings: PresentationSettings): Try<PresentationSettings, Exception> =
            activateInSettings.invoke(settings)
    }
}

/**
 * Holds a list of key-value pairs provided by the app to influence a Navigator's Presentation
 * Properties. The keys must be valid Presentation Property Keys.
 */
data class PresentationSettings(val settings: Map<PresentationKey, Any?> = emptyMap()) {
    val continuous: Boolean?
        get() = settings[PresentationKey.CONTINUOUS] as? Boolean

    val readingProgression: ReadingProgression?
        get() = settings[PresentationKey.READING_PROGRESSION] as? ReadingProgression

    /**
     * Returns a copy of this object after modifying the settings in the given closure.
     */
    fun copy(transform: MutableMap<PresentationKey, Any?>.() -> Unit): PresentationSettings =
        PresentationSettings(settings.toMutableMap().apply(transform).toMap())

    /**
     * Returns a copy of this object after overwriting any setting with the values from [other].
     */
    fun merge(other: PresentationSettings): PresentationSettings =
        PresentationSettings(
            (other.settings.entries + settings.entries)
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.firstOrNull() }
        )
}
