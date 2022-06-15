/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.positions

@OptIn(ExperimentalReadiumApi::class)
@PdfSupport
internal class PdfNavigatorViewModel(
    application: Application,
    private val publication: Publication,
    initialLocator: Locator,
    settings: PdfDocumentFragment.Settings,
    private val defaultSettings: PdfDocumentFragment.Settings
) : AndroidViewModel(application) {

    data class State(
        val locator: Locator,
        val userSettings: PdfDocumentFragment.Settings,
        val appliedSettings: PdfDocumentFragment.Settings
    )

    private val _state = MutableStateFlow(
        State(
            locator = initialLocator,
            userSettings = settings,
            appliedSettings = combineSettings(settings)
        )
    )

    val state: StateFlow<State> = _state.asStateFlow()

    fun setUserSettings(settings: PdfDocumentFragment.Settings) {
        _state.update {
            it.copy(
                userSettings = settings,
                appliedSettings = combineSettings(settings)
            )
        }
    }

    private fun combineSettings(settings: PdfDocumentFragment.Settings) =
        PdfDocumentFragment.Settings(
            fit = settings.fit
                ?: publication.metadata.presentation.fit
                ?: defaultSettings.fit,
            overflow = settings.overflow.takeUnless { it == Presentation.Overflow.AUTO }
                ?: publication.metadata.presentation.overflow.takeUnless { it == Presentation.Overflow.AUTO }
                ?: defaultSettings.overflow,
            readingProgression = settings.readingProgression.takeUnless { it == ReadingProgression.AUTO }
                ?: publication.metadata.readingProgression.takeUnless { it == ReadingProgression.AUTO }
                ?: defaultSettings.readingProgression
        )

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
            settings: PdfDocumentFragment.Settings,
            defaultSettings: PdfDocumentFragment.Settings
        ) = createViewModelFactory {
            PdfNavigatorViewModel(
                application = application,
                publication = publication,
                initialLocator = initialLocator
                    ?: requireNotNull(publication.locatorFromLink(publication.readingOrder.first())),
                settings = settings,
                defaultSettings = defaultSettings)
        }
    }
}
