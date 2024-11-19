/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader

import org.readium.adapter.exoplayer.audio.ExoPlayerNavigator
import org.readium.adapter.exoplayer.audio.ExoPlayerNavigatorFactory
import org.readium.adapter.exoplayer.audio.ExoPlayerPreferences
import org.readium.adapter.pdfium.navigator.PdfiumNavigatorFactory
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*
import org.readium.r2.testapp.reader.preferences.PreferencesManager

sealed class ReaderInitData {
    abstract val bookId: Long
    abstract val publication: Publication
}

sealed class VisualReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val initialLocation: Locator?,
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
    val navigatorFactory: PdfiumNavigatorFactory,
    ttsInitData: TtsInitData?,
) : VisualReaderInitData(bookId, publication, initialLocation, ttsInitData)

class TtsInitData(
    val mediaServiceFacade: MediaServiceFacade,
    val navigatorFactory: AndroidTtsNavigatorFactory,
    val preferencesManager: PreferencesManager<AndroidTtsPreferences>,
)

class MediaReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val mediaNavigator: ExoPlayerNavigator,
    val preferencesManager: PreferencesManager<ExoPlayerPreferences>,
    val navigatorFactory: ExoPlayerNavigatorFactory,
) : ReaderInitData()

class DummyReaderInitData(
    override val bookId: Long,
) : ReaderInitData() {
    override val publication: Publication = Publication(
        Manifest(
            metadata = Metadata(identifier = "dummy")
        )
    )
}
