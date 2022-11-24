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
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
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
) : ReaderInitData()

class ImageReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?
) : VisualReaderInitData(bookId, publication, initialLocation)

class EpubReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    val preferencesManager: PreferencesManager<EpubPreferences>,
    val navigatorFactory: EpubNavigatorFactory
) : VisualReaderInitData(bookId, publication, initialLocation)

class PdfReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    val preferencesManager: PreferencesManager<PdfiumPreferences>,
    val navigatorFactory: PdfNavigatorFactory<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor>
) : VisualReaderInitData(bookId, publication, initialLocation)

@ExperimentalMedia2
class MediaReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val mediaNavigator: MediaNavigator,
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
