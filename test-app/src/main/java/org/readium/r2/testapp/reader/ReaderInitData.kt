/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferences
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferencesFilter
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.adapters.pspdfkit.navigator.PsPdfKitNavigatorFactory
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferencesSerializer
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesFilter
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*

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

@OptIn(ExperimentalReadiumApi::class)
class EpubReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    val coroutineScope: CoroutineScope,
    val preferencesFlow: StateFlow<EpubPreferences>,
    val preferencesFilter: EpubPreferencesFilter,
    val preferencesSerializer: EpubPreferencesSerializer,
    val navigatorFactory: EpubNavigatorFactory
) : VisualReaderInitData(bookId, publication, initialLocation)

@OptIn(ExperimentalReadiumApi::class)
class PdfReaderInitData(
    bookId: Long,
    publication: Publication,
    initialLocation: Locator?,
    val coroutineScope: CoroutineScope,
    val preferencesFlow: StateFlow<PsPdfKitPreferences>,
    val preferencesFilter: PsPdfKitPreferencesFilter,
    val preferencesSerializer: PsPdfKitPreferencesSerializer,
    val navigatorFactory: PsPdfKitNavigatorFactory
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
