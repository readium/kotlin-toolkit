package org.readium.r2.navigator.presentation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.getOrDefault

/**
 * Helper class which simplifies the modification of Presentation Settings and designing a user
 * settings interface.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalPresentation
class PresentationController(
    coroutineScope: CoroutineScope,
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
    suspend fun commit(changes: PresentationController.(Settings) -> Unit = {}) {
        changes(settings.value)
        navigator.applySettings(appSettings.merge(userSettings.value))
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

        set(setting, !setting.value)
    }

    data class Settings(
        val settings: Map<PresentationKey, Setting<*>?> = mutableMapOf()
    ) {
        val continuous: ToggleSetting? get() =
            settings[PresentationKey.CONTINUOUS] as? ToggleSetting

        val readingProgression: EnumSetting<ReadingProgression>? get() =
            (settings[PresentationKey.READING_PROGRESSION] as? StringSetting)
                ?.let { EnumSetting(ReadingProgression, it) }
    }

    /**
     * Holds the current value and the metadata of a Presentation Setting of type [T].
     *
     * @param key Presentation Key for this setting.
     * @param value Current value for the property.
     * @param isAvailable Indicates whether the Presentation Setting is available for the [navigator].
     * @param isActive Indicates whether the Presentation Setting is active for the current set of
     *        [userSettings].
     */
    sealed class Setting<T>(
        val key: PresentationKey,
        val value: T,
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
        value: Boolean,
        isAvailable: Boolean,
        isActive: Boolean,
    ) : Setting<Boolean>(key, value, isAvailable = isAvailable, isActive = isActive)

    class StringSetting(
        key: PresentationKey,
        value: String,
        val supportedValues: List<String>?,
        isAvailable: Boolean,
        isActive: Boolean,
        private val labelForValue: (Context, String) -> String
    ) : Setting<String>(key, value, isAvailable = isAvailable, isActive = isActive) {

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
        private val stringSetting: StringSetting,
    ) : Setting<T?>(stringSetting.key, mapper.get(stringSetting.value), isAvailable = stringSetting.isAvailable, isActive = stringSetting.isActive) {

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
