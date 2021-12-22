/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.presentation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import java.lang.ref.WeakReference
import kotlin.math.round

/**
 * Helper class which simplifies the modification of Presentation Settings and designing a user
 * settings interface.
 *
 * @param autoActivateSettings Requests the navigator to activate a non-active setting when its
 *        value is changed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalPresentation
class PresentationController(
    private val scope: CoroutineScope,
    settings: PresentationValues = PresentationValues(),
    private var autoCommit: Boolean = true,
    private val autoActivateSettings: Boolean = true,
    private val onAdjust: (PresentationValues) -> PresentationValues = { it },
    private val onCommit: (PresentationValues) -> Unit = {},
) {

    private val _settings = MutableStateFlow(Settings(
        values = settings,
        adjustedValues = onAdjust(settings),
        properties = null
    ))
    val settings: StateFlow<Settings>
        get() = _settings.asStateFlow()

    private val committedValues = MutableSharedFlow<PresentationValues>()

    init {
        committedValues
            .onEach { onCommit(it) }
            .launchIn(scope)

        this.settings
            .distinctUntilChangedBy(Settings::adjustedValues)
            .onEach {
                if (autoCommit) {
                    commit(it)
                }
            }
            .launchIn(scope)
    }

    fun bind(navigator: PresentableNavigator) {
        navigator.presentationProperties
            .onEach {
                _settings.value = _settings.value.copy(properties = it)
            }
            .launchIn(scope)

        committedValues
            .onEach { navigator.applyPresentationSettings(it) }
            .launchIn(scope)
    }

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
        committedValues.emit(settings.adjustedValues)
    }

    /**
     * Clears all user settings to revert to the Navigator default values.
     */
    fun reset() {
        set(PresentationValues())
    }

    /**
     * Clears the given user setting to revert to the Navigator default value.
     */
    fun <T> reset(setting: Setting<T>?) {
        set(setting, null)
    }

    fun set(settings: PresentationValues) {
        _settings.value = _settings.value.copy(
            values = settings,
            adjustedValues = onAdjust(settings)
        )
    }

    /**
     * Changes the value of the given setting.
     * The new value will be set in the user settings.
     */
    fun <T> set(setting: Setting<T>?, value: T?) {
        setting ?: return

        val settings = _settings.value

        var values = settings.values.copy {
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
            values = values,
            adjustedValues = onAdjust(values)
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
        val values: PresentationValues,
        val adjustedValues: PresentationValues,
        val properties: PresentationProperties?,
    ) : JSONable by values {

        inline operator fun <reified V, reified R> get(key: PresentationKey<V, R>): Setting<V>? {
            val property = properties?.get(key)
            val effectiveValue = property?.value
            val userValue = values[key]
            val constraints = property?.constraints
            val isActive = constraints?.isActiveForValues(adjustedValues) ?: false

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
            get() = get(PresentationKey.CONTINUOUS)

        val fit: Setting<Fit>?
            get() = get(PresentationKey.FIT)

        val orientation: Setting<Orientation>?
            get() = get(PresentationKey.ORIENTATION)

        val overflow: Setting<Overflow>?
            get() = get(PresentationKey.OVERFLOW)

        val pageSpacing: Setting<PresentationRange>?
            get() = get(PresentationKey.PAGE_SPACING)

        val readingProgression: Setting<ReadingProgression>?
            get() = get(PresentationKey.READING_PROGRESSION)
    }

    data class Setting<V>(
        val key: PresentationKey<V, *>,
        val value: V?,
        val effectiveValue: V?,
        val isActive: Boolean,
        val constraints: PresentationConstraints<V>?
    )
}

@ExperimentalPresentation
val PresentationController.Setting<PresentationRange>.step: Double get() =
    constraints?.step ?: 0.1

@ExperimentalPresentation
val <E : Enum<E>> PresentationController.Setting<E>.supportedValues: List<E>? get() =
    constraints?.supportedValues
