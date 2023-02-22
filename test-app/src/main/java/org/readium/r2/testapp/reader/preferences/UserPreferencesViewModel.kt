/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.preferences

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.adapters.pdfium.navigator.PdfiumPreferences
import org.readium.adapters.pdfium.navigator.PdfiumSettings
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.reader.EpubReaderInitData
import org.readium.r2.testapp.reader.PdfReaderInitData
import org.readium.r2.testapp.reader.ReaderInitData
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.extensions.mapStateIn

/**
 * Manages user settings.
 *
 * Note: This is not an Android ViewModel, but it is a component of [ReaderViewModel].
 *
 * @param bookId Database ID for the book.
 */
@OptIn(ExperimentalReadiumApi::class)
class UserPreferencesViewModel<S : Configurable.Settings, P : Configurable.Preferences<P>>(
    private val bookId: Long,
    private val viewModelScope: CoroutineScope,
    private val preferencesManager: PreferencesManager<P>,
    private val createPreferencesEditor: (P) -> PreferencesEditor<P>
) {
    val editor: StateFlow<PreferencesEditor<P>> = preferencesManager.preferences
        .mapStateIn(viewModelScope, createPreferencesEditor)

    fun bind(configurable: Configurable<S, P>, lifecycleOwner: LifecycleOwner) {
        with(lifecycleOwner) {
            preferencesManager.preferences
                .flowWithLifecycle(lifecycle)
                .onEach { configurable.submitPreferences(it) }
                .launchIn(lifecycleScope)
        }
    }

    fun commit() {
        viewModelScope.launch {
            preferencesManager.setPreferences(editor.value.preferences)
        }
    }

    companion object {

        operator fun invoke(
            viewModelScope: CoroutineScope,
            readerInitData: ReaderInitData
        ): UserPreferencesViewModel<*, *>? =
            when (readerInitData) {
                is EpubReaderInitData -> with(readerInitData) {
                    UserPreferencesViewModel<EpubSettings, EpubPreferences>(
                        bookId, viewModelScope, preferencesManager,
                        createPreferencesEditor = navigatorFactory::createPreferencesEditor
                    )
                }
                is PdfReaderInitData -> with(readerInitData) {
                    UserPreferencesViewModel<PdfiumSettings, PdfiumPreferences>(
                        bookId, viewModelScope, preferencesManager,
                        createPreferencesEditor = navigatorFactory::createPreferencesEditor
                    )
                }
                else -> null
            }
    }
}
