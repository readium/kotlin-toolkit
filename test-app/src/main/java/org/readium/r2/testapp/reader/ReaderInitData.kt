/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader

import org.readium.adapters.pdfium.navigator.PdfiumPreferences
import org.readium.adapters.pdfium.navigator.PdfiumPreferencesEditor
import org.readium.adapters.pdfium.navigator.PdfiumSettings
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.media3.tts.AndroidTtsNavigatorFactory
import org.readium.r2.navigator.media3.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*
import org.readium.r2.testapp.reader.preferences.PreferencesManager
import org.readium.r2.testapp.reader.tts.TtsServiceFacade

sealed class ReaderInitData {
    abstract val bookId: Long
    abstract val publication: Publication
}

sealed class VisualReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    var initialLocation: Locator?,
    val ttsInitData: TtsInitData?,
) : ReaderInitData()

class ImageReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    ttsInitData: TtsInitData?,
) : VisualReaderInitData(bookId, publication, initialLocation, ttsInitData)

class EpubReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    val preferencesManager: PreferencesManager<EpubPreferences>,
    val navigatorFactory: EpubNavigatorFactory,
    ttsInitData: TtsInitData?,
) : VisualReaderInitData(bookId, publication, initialLocation, ttsInitData)

class PdfReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    val preferencesManager: PreferencesManager<PdfiumPreferences>,
    val navigatorFactory: PdfNavigatorFactory<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor>,
    ttsInitData: TtsInitData?,
) : VisualReaderInitData(bookId, publication, initialLocation, ttsInitData)

@OptIn(ExperimentalMedia2::class)
class MediaReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val mediaNavigator: MediaNavigator,
    val sessionBinder: MediaService.Binder
    // val preferencesManager: PreferencesManager<ExoPlayerPreferences>,
    // val navigatorFactory: PlayerNavigatorFactory<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerPreferencesEditor>
) : ReaderInitData()

class DummyReaderInitData(
    override val bookId: Long,
) : ReaderInitData() {
    override val publication: Publication = Publication(
        Manifest(
            metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString(""))
        )
    )
}

class TtsInitData(
    val ttsServiceFacade: TtsServiceFacade,
    val ttsNavigatorFactory: AndroidTtsNavigatorFactory,
    val preferencesManager: PreferencesManager<AndroidTtsPreferences>,
)
