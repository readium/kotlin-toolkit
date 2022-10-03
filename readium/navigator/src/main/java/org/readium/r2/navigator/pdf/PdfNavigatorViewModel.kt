/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions

@OptIn(ExperimentalReadiumApi::class)
internal class PdfNavigatorViewModel(
    application: Application,
    private val publication: Publication,
    initialLocator: Locator,
    preferences: Preferences,
    private val settingsFactory: (Metadata, Preferences) -> PdfSettings
) : AndroidViewModel(application) {

    data class State(
        val locator: Locator
    )

    private val _state = MutableStateFlow(
        State(
            locator = initialLocator,
        )
    )

    val state: StateFlow<State> = _state.asStateFlow()

    private val _settings: MutableStateFlow<PdfSettings> = MutableStateFlow(
        settingsFactory(publication.metadata, preferences)
    )

    val settings: StateFlow<Configurable.Settings> = _settings.asStateFlow()

    fun submitPreferences(preferences: Preferences) = viewModelScope.launch {
        _settings.value = settingsFactory(publication.metadata, preferences)
    }

    fun onPageChanged(pageIndex: Int) = viewModelScope.launch {
        publication.positions().getOrNull(pageIndex)?.let { locator ->
            _state.update {
                it.copy(locator = locator)
            }
        }
    }

    companion object {
        fun createFactory(
            application: Application,
            publication: Publication,
            initialLocator: Locator?,
            preferences: Preferences,
            settingsFactory: (Metadata, Preferences) -> PdfSettings
        ) = createViewModelFactory {
            PdfNavigatorViewModel(
                application = application,
                publication = publication,
                initialLocator = initialLocator
                    ?: requireNotNull(publication.locatorFromLink(publication.readingOrder.first())),
                preferences = preferences,
                settingsFactory = settingsFactory
            )
        }
    }
}
