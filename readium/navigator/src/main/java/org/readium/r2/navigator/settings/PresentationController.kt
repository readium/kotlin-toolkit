/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.util.Closeable
import kotlin.math.round

/**
 * Helper class which simplifies the modification of Presentation Settings and designing a user
 * settings interface.
 *
 * @param autoActivateSettings Requests the navigator to activate a non-active setting when its
 *        value is changed.
 */
@ExperimentalReadiumApi
class PresentationController(
    preferences: Preferences = Preferences(),
    private val autoActivateSettings: Boolean = true,
    private val onAdjust: (Preferences) -> Preferences = { it },
    private val onCommit: (Preferences) -> Unit = {},
    private val scope: CoroutineScope,
) : Closeable {

    private val _settings = MutableStateFlow(Settings(
        settings = null,
        preferences = preferences,
        adjustedPreferences = onAdjust(preferences),
    ))
    val settings: StateFlow<Settings>
        get() = _settings.asStateFlow()

    private val committedPreferences = MutableSharedFlow<Preferences>()

    init {
        committedPreferences
            .onEach { onCommit(it) }
            .launchIn(scope)

        this.settings
            .distinctUntilChangedBy(Settings::adjustedPreferences)
            .onEach {
                if (autoCommit) {
                    commit(it)
                }
            }
            .launchIn(scope)
    }

    override fun close() {
        navigatorJob?.cancel()
        navigatorJob = null
    }

    /** Job for the currently bound navigator. */
    private var navigatorJob: Job? = null

    fun bind(navigator: PresentableNavigator) {
        navigatorJob?.cancel()
        navigatorJob = scope.launch {
            navigator.presentationSettings
                .onEach { settings ->
                    _settings.update { it.copy(settings = settings) }
                }
                .launchIn(this)

            committedPreferences
                .onEach { navigator.applyPresentationPreferences(it) }
                .launchIn(this)

            commit(settings.value)
        }
    }

    private var autoCommit: Boolean = true

    /**
     * Commits the current set of settings after applying the given changes.
     */
    fun commit(changes: PresentationController.(Settings) -> Unit) {
        val oldAutoCommit = autoCommit
        autoCommit = false
        changes(settings.value)
        autoCommit = oldAutoCommit

        commit()
    }

    /**
     * Commits the current set of settings.
     */
    fun commit() {
        scope.launch {
            commit(settings.value)
        }
    }

    private suspend fun commit(settings: Settings) {
        committedPreferences.emit(settings.adjustedPreferences)
    }

    /**
     * Clears all user settings to revert to the Navigator default values.
     */
    fun reset() {
        set(Preferences())
    }

    /**
     * Clears the given user setting to revert to the Navigator default value.
     */
    fun <T> reset(setting: Setting<T>?) {
        set(setting, null)
    }

    fun set(settings: Preferences) {
        _settings.value = _settings.value.copy(
            preferences = settings,
            adjustedPreferences = onAdjust(settings)
        )
    }

    /**
     * Changes the value of the given setting.
     * The new value will be set in the user settings.
     */
    fun <T> set(setting: Setting<T>?, value: T?) {
        setting ?: return

        val settings = _settings.value

        var values = settings.preferences.copy {
            if (value == null) {
                remove(setting.key.key)
            } else {
                set(setting.key.key, setting.key.encode(value))
            }
        }

        if (autoActivateSettings) {
            values = setting.constraints?.activateInValues(values)?.getOrNull() ?: values
        }

        _settings.value = settings.copy(
            preferences = values,
            adjustedPreferences = onAdjust(values)
        )
    }

    /**
     * Inverts the value of the given toggle setting.
     */
    fun toggle(setting: Setting<PresentationToggle>?) {
        setting ?: return

        val value = setting.value ?: setting.effectiveValue ?: PresentationToggle(false)
        set(setting, value.toggle())
    }

    /**
     * Inverts the value of the given setting. If the setting is already set to the given value, it
     * is nulled out.
     */
    fun <T> toggle(setting: Setting<T>?, value: T) {
        setting ?: return

        if (setting.value == value) {
            reset(setting)
        } else {
            set(setting, value)
        }
    }

    /**
     * Increments the value of the given range setting to the next effective step.
     */
    fun increment(setting: Setting<PresentationRange>?) {
        setting ?: return

        val step = setting.step
        val value = setting.value?.double ?: setting.effectiveValue?.double ?: 0.5

        set(setting, PresentationRange((value + step).roundToStep(setting.step).coerceAtMost(1.0)))
    }

    /**
     * Decrements the value of the given range setting to the previous effective step.
     */
    fun decrement(setting: Setting<PresentationRange>?) {
        setting ?: return

        val step = setting.step
        val value = setting.value?.double ?: setting.effectiveValue?.double ?: 0.5

        set(setting, PresentationRange((value - step).roundToStep(setting.step).coerceAtLeast(0.0)))
    }

    private fun Double.roundToStep(step: Double): Double =
        round(this / step) * step

    data class Settings(
        val settings: PresentationSettings?,
        val preferences: Preferences,
        val adjustedPreferences: Preferences,
    ) : JSONable by preferences {

        inline operator fun <reified V, reified R> get(key: SettingKey<V, R>): Setting<V>? {
            val property = settings?.get(key)
            val effectiveValue = property?.value
            val userValue = preferences[key]
            val constraints = property?.constraints
            val isActive = constraints?.isActiveForValues(adjustedPreferences) ?: false

            if (userValue == null && effectiveValue == null) {
                return null
            }

            return Setting(
                key = key,
                value = userValue,
                effectiveValue = effectiveValue,
                isActive = isActive,
                constraints = constraints,
            )
        }

        val continuous: Setting<PresentationToggle>?
            get() = get(SettingKey.CONTINUOUS)

        val fit: Setting<Fit>?
            get() = get(SettingKey.FIT)

        val orientation: Setting<Orientation>?
            get() = get(SettingKey.ORIENTATION)

        val overflow: Setting<Overflow>?
            get() = get(SettingKey.OVERFLOW)

        val pageSpacing: Setting<PresentationRange>?
            get() = get(SettingKey.PAGE_SPACING)

        val readingProgression: Setting<ReadingProgression>?
            get() = get(SettingKey.READING_PROGRESSION)
    }

    data class Setting<V>(
        val key: SettingKey<V, *>,
        val value: V?,
        val effectiveValue: V?,
        val isActive: Boolean,
        val constraints: PresentationConstraints<V>?
    )
}

@ExperimentalReadiumApi
val PresentationController.Setting<PresentationRange>.step: Double get() =
    constraints?.step ?: 0.1

@ExperimentalReadiumApi
val <E : Enum<E>> PresentationController.Setting<E>.supportedValues: List<E>? get() =
    constraints?.supportedValues
