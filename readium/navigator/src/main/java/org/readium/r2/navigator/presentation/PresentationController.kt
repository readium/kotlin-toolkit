package org.readium.r2.navigator.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.MapWithDefaultCompanion
import org.readium.r2.shared.util.getOrDefault

/**
 * Helper class which simplifies the modification of Presentation Settings and designing a user
 * settings interface.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresentationController(
    val navigator: Navigator,
    val appSettings: PresentationSettings,
    userSettings: PresentationSettings,
) {
    private val _userSettings = MutableStateFlow(userSettings)
    val userSettings: StateFlow<PresentationSettings>
        get() = _userSettings.asStateFlow()

    suspend fun settings(scope: CoroutineScope): StateFlow<Settings> =
        userSettings.combine(navigator.presentation) { userSettings, presentation ->
            combineSettings(userSettings, presentation)
        }
        .stateIn(scope)

    private fun combineSettings(userSettings: PresentationSettings, presentation: Presentation): Settings {
        val settings = mutableMapOf<PresentationKey, Setting<*>>()

        for (key in userSettings.settings.keys + presentation.properties.keys) {
            val userValue = userSettings.settings[key]
            val navigatorProperty = presentation.properties[key]

            val setting: Setting<*>? = when {
                (navigatorProperty is Presentation.ToggleProperty || userValue is Boolean) -> {
                    val property = navigatorProperty as? Presentation.ToggleProperty
                    val value = userValue as? Boolean
                    ToggleSetting(
                        key = key,
                        value = value ?: property?.value ?: false,
                        isAvailable = property != null,
                        isActive = property?.isActiveForSettings(userSettings) ?: false,
                    )
                }

                (navigatorProperty is Presentation.StringProperty || userValue is String) -> {
                    val property = navigatorProperty as? Presentation.StringProperty
                    val value = userValue as? String
                    StringSetting(
                        key = key,
                        value = value ?: property?.value ?: "",
                        supportedValues = property?.supportedValues,
                        isAvailable = property != null,
                        isActive = property?.isActiveForSettings(userSettings) ?: false,
                        labelForValue = { property?.labelForValue(it) ?: it }
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
     * Applies the current set of [userSettings] to the Navigator.
     */
    fun apply() {
        navigator.apply(appSettings.merge(userSettings.value))
    }

    /**
     * Clears all user settings to revert to the Navigator default values.
     */
    fun reset() {
        _userSettings.value = PresentationSettings()
    }

    /**
     * Changes the value of the given setting.
     * The new value will be set in the user settings.
     */
    fun <T> set(setting: Setting<T>, value: T?) {
        var settings = userSettings.value.copy {
            if (value == null) {
                remove(setting.key)
            } else {
                set(setting.key, value)
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
    fun toggle(property: ToggleSetting) {
        set(property, !property.value)
    }

    data class Settings(
        val properties: Map<PresentationKey, Setting<*>?> = mutableMapOf()
    ) {
        val continuous: ToggleSetting? get() =
            properties[PresentationKey.CONTINUOUS] as? ToggleSetting

        val readingProgression: EnumSetting<ReadingProgression>? get() =
            (properties[PresentationKey.READING_PROGRESSION] as? StringSetting)
                ?.let { EnumSetting(it, ReadingProgression) }
    }

    /**
     * Holds the current value and the metadata of a Presentation Setting of type [T].
     */
    interface Setting<T> {
        /**
         * Presentation Key for this setting.
         */
        val key: PresentationKey

        /**
         * Current value for the property.
         */
        val value: T

        /**
         * Indicates whether the Presentation Setting is available for the [navigator].
         */
        val isAvailable: Boolean

        /**
         * Indicates whether the Presentation Setting is active for the current set of [userSettings].
         */
        val isActive: Boolean
    }

    class ToggleSetting(
        key: PresentationKey,
        value: Boolean,
        isAvailable: Boolean,
        isActive: Boolean,
    ) : BaseSetting<Boolean>(key, value, isAvailable = isAvailable, isActive = isActive)

    class StringSetting(
        key: PresentationKey,
        value: String,
        val supportedValues: List<String>?,
        isAvailable: Boolean,
        isActive: Boolean,
        private val labelForValue: (String) -> String
    ) : BaseSetting<String>(key, value, isAvailable = isAvailable, isActive = isActive) {

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

    class EnumSetting<T : Enum<T>>(
        private val stringSetting: StringSetting,
        private val mapper: MapCompanion<String, T>,
    ) : BaseSetting<T?>(stringSetting.key, mapper.get(stringSetting.value), isAvailable = stringSetting.isAvailable, isActive = stringSetting.isActive) {

        val supportedValues: List<T>? = stringSetting.supportedValues
            ?.mapNotNull { mapper.get(it) }


        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "reading progression" property, the value ltr has for label "Left to
         * right" in English.
         */
        fun labelForValue(value: T): String =
            stringSetting.labelForValue(mapper.getKey(value) ?: value.name)
    }

    abstract class BaseSetting<T>(
        override val key: PresentationKey,
        override val value: T,
        override val isAvailable: Boolean,
        override val isActive: Boolean
    ) : Setting<T>
}
