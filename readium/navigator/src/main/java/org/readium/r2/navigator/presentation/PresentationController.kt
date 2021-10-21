package org.readium.r2.navigator.presentation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.extensions.toStringPercentage
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.Fit
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.getOrDefault
import kotlin.math.round

/**
 * Helper class which simplifies the modification of Presentation Settings and designing a user
 * settings interface.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalPresentation
class PresentationController(
    private val coroutineScope: CoroutineScope,
    val navigator: Navigator,
    val appSettings: PresentationSettings = PresentationSettings(),
    userSettings: PresentationSettings = PresentationSettings(),
) {

    private val _settings = MutableStateFlow(combineSettings(userSettings, navigator.presentation.value))
    val settings: StateFlow<Settings>
        get() = _settings.asStateFlow()

    private val _userSettings = MutableStateFlow(userSettings)
    val userSettings: StateFlow<PresentationSettings>
        get() = _userSettings.asStateFlow()

    init {
        coroutineScope.launch {
            _userSettings.combine(navigator.presentation) { userSettings, presentation ->
                combineSettings(userSettings, presentation)
            }.collect { _settings.value = it }
        }
    }

    private fun combineSettings(userSettings: PresentationSettings, presentation: Presentation): Settings {
        val settings = mutableMapOf<PresentationKey, Setting<*>>()

        for (key in userSettings.settings.keys + presentation.properties.keys) {
            val userValue = userSettings.settings[key]
            val navigatorProperty = presentation.properties[key]

            val setting: Setting<*>? = when {
                (navigatorProperty is Presentation.ToggleProperty || userValue is Boolean) -> {
                    val property = navigatorProperty as? Presentation.ToggleProperty
                    ToggleSetting(
                        key = key,
                        userValue = userValue as? Boolean,
                        effectiveValue = property?.value,
                        isAvailable = property != null,
                        isActive = property?.isActiveForSettings(userSettings) ?: false,
                    )
                }

                (navigatorProperty is Presentation.RangeProperty || userValue is Double) -> {
                    val property = navigatorProperty as? Presentation.RangeProperty
                    RangeSetting(
                        key = key,
                        userValue = userValue as? Double,
                        effectiveValue = property?.value,
                        stepCount = property?.stepCount,
                        isAvailable = property != null,
                        isActive = property?.isActiveForSettings(userSettings) ?: false,
                        labelForValue = { c, v -> property?.labelForValue(c, v) ?: v.toStringPercentage() }
                    )
                }

                (navigatorProperty is Presentation.StringProperty || userValue is String) -> {
                    val property = navigatorProperty as? Presentation.StringProperty
                    StringSetting(
                        key = key,
                        userValue = userValue as? String,
                        effectiveValue = property?.value,
                        supportedValues = property?.supportedValues,
                        isAvailable = property != null,
                        isActive = property?.isActiveForSettings(userSettings) ?: false,
                        labelForValue = { c, v -> property?.labelForValue(c, v) ?: v }
                    )
                }

                else -> null
            }

            if (setting != null) {
                settings[key] = setting
            }
        }

        return Settings(settings)
    }

    /**
     * Applies the current set of settings to the Navigator.
     */
    fun commit(changes: PresentationController.(Settings) -> Unit = {}) {
        changes(settings.value)
        coroutineScope.launch {
            navigator.applySettings(appSettings.merge(userSettings.value))
        }
    }

    /**
     * Clears all user settings to revert to the Navigator default values.
     */
    fun reset() {
        _userSettings.value = PresentationSettings()
    }

    /**
     * Clears the given user setting to revert to the Navigator default value.
     */
    fun <T> reset(setting: Setting<T>?) {
        set(setting, null)
    }

    /**
     * Changes the value of the given setting.
     * The new value will be set in the user settings.
     */
    fun <T> set(setting: Setting<T>?, value: T?) {
        setting ?: return

        var settings = userSettings.value.copy {
            if (value == null) {
                remove(setting.key)
            } else {
                set(setting.key, setting.toJson(value))
            }
        }

        val navigatorProperty = navigator.presentation.value.properties[setting.key]
        if (navigatorProperty != null) {
            settings = navigatorProperty.activateInSettings(settings)
                .getOrDefault(settings)
        }

        _userSettings.value = settings
    }

    /**
     * Inverts the value of the given toggle setting.
     */
    fun toggle(setting: ToggleSetting?) {
        setting ?: return

        set(setting, !(setting.userValue ?: setting.effectiveValue ?: false))
    }

    /**
     * Inverts the value of the given setting. If the setting is already set to the given value, it
     * is nulled out.
     */
    fun <T> toggle(setting: Setting<T>?, value: T) {
        setting ?: return

        if (setting.userValue == value) {
            reset(setting)
        } else {
            set(setting, value)
        }
    }

    /**
     * Increments the value of the given range setting to the next effective step.
     */
    fun increment(setting: RangeSetting?) {
        setting ?: return

        val step = setting.step
        val value = setting.userValue ?: setting.effectiveValue ?: 0.5

        set(setting, (value + step).roundToStep(setting.step).coerceAtMost(1.0))
    }

    /**
     * Decrements the value of the given range setting to the previous effective step.
     */
    fun decrement(setting: RangeSetting?) {
        setting ?: return

        val step = setting.step
        val value = setting.userValue ?: setting.effectiveValue ?: 0.5

        set(setting, (value - step).roundToStep(setting.step).coerceAtLeast(0.0))
    }

    private fun Double.roundToStep(step: Double): Double =
        round(this / step) * step

    data class Settings(
        val settings: Map<PresentationKey, Setting<*>?> = mutableMapOf()
    ) {
        constructor(vararg settings: Pair<PresentationKey, Setting<*>?>) : this(mapOf(*settings))

        val continuous: ToggleSetting? get() =
            settings[PresentationKey.CONTINUOUS] as? ToggleSetting

        val fit: EnumSetting<Fit>? get() =
            (settings[PresentationKey.FIT] as? StringSetting)
                ?.let { EnumSetting(Fit, it) }

        val overflow: EnumSetting<Overflow>? get() =
            (settings[PresentationKey.OVERFLOW] as? StringSetting)
                ?.let { EnumSetting(Overflow, it) }

        val pageSpacing: RangeSetting? get() =
            settings[PresentationKey.PAGE_SPACING] as? RangeSetting

        val readingProgression: EnumSetting<ReadingProgression>? get() =
            (settings[PresentationKey.READING_PROGRESSION] as? StringSetting)
                ?.let { EnumSetting(ReadingProgression, it) }
    }

    /**
     * Holds the current value and the metadata of a Presentation Setting of type [T].
     *
     * @param key Presentation Key for this setting.
     * @param userValue Value taken from the current user settings.
     * @param effectiveValue Actual value in effect for the navigator.
     * @param isAvailable Indicates whether the Presentation Setting is available for the [navigator].
     * @param isActive Indicates whether the Presentation Setting is active for the current set of
     *        [userSettings].
     */
    sealed class Setting<T>(
        val key: PresentationKey,
        val userValue: T?,
        val effectiveValue: T?,
        val isAvailable: Boolean,
        val isActive: Boolean,
    ) {
        /**
         * Serializes the given value to its JSON type.
         */
        open fun toJson(value: T): Any = value as Any
    }

    class ToggleSetting(
        key: PresentationKey,
        userValue: Boolean?,
        effectiveValue: Boolean?,
        isAvailable: Boolean,
        isActive: Boolean,
    ) : Setting<Boolean>(key, userValue, effectiveValue, isAvailable = isAvailable, isActive = isActive)

    class RangeSetting(
        key: PresentationKey,
        userValue: Double?,
        effectiveValue: Double?,
        val stepCount: Int?,
        isAvailable: Boolean,
        isActive: Boolean,
        private val labelForValue: (Context, Double) -> String,
    ) : Setting<Double>(key, userValue, effectiveValue, isAvailable = isAvailable, isActive = isActive) {

        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "font size" property, the value 0.4 might have for label "12 pt",
         * depending on the Navigator.
         */
        fun labelForValue(context: Context, value: Double): String =
            labelForValue.invoke(context, value)

        internal val step: Double get() =
            if (stepCount == null || stepCount == 0) 0.1
            else 1.0 / stepCount

    }

    class StringSetting(
        key: PresentationKey,
        userValue: String?,
        effectiveValue: String?,
        val supportedValues: List<String>?,
        isAvailable: Boolean,
        isActive: Boolean,
        private val labelForValue: (Context, String) -> String,
    ) : Setting<String>(key, userValue, effectiveValue, isAvailable = isAvailable, isActive = isActive) {

        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "reading progression" property, the value ltr has for label "Left to
         * right" in English.
         */
        fun labelForValue(context: Context, value: String): String =
            labelForValue.invoke(context, value)
    }

    class EnumSetting<T : Enum<T>>(
        private val mapper: MapCompanion<String, T>,
        val stringSetting: StringSetting,
    ) : Setting<T?>(
        stringSetting.key,
        mapper.get(stringSetting.userValue),
        mapper.get(stringSetting.effectiveValue),
        isAvailable = stringSetting.isAvailable,
        isActive = stringSetting.isActive
    ) {

        constructor(
            mapper: MapCompanion<String, T>,
            key: PresentationKey,
            userValue: T?,
            effectiveValue: T?,
            supportedValues: List<T>?,
            isAvailable: Boolean,
            isActive: Boolean,
            labelForValue: (Context, T) -> String
        ) : this(mapper, StringSetting(
            key,
            userValue?.let { mapper.getKey(it) },
            effectiveValue?.let { mapper.getKey(it) },
            supportedValues?.map { mapper.getKey(it) },
            isAvailable = isAvailable,
            isActive = isActive,
            labelForValue = { c, v -> mapper.get(v)?.let { labelForValue(c, it) } ?: v }
        ))

        override fun toJson(value: T?): Any =
            value?.let { stringSetting.toJson(mapper.getKey(it)) } as Any

        val supportedValues: List<T>? = stringSetting.supportedValues
            ?.mapNotNull { mapper.get(it) }


        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "reading progression" property, the value ltr has for label "Left to
         * right" in English.
         */
        fun labelForValue(context: Context, value: T): String =
            stringSetting.labelForValue(context, mapper.getKey(value))
    }
}
