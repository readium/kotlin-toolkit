/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*

enum class NavigatorKind {
    EPUB_REFLOWABLE, EPUB_FIXEDLAYOUT, PDF, AUDIO, IMAGE
}

sealed class ReaderInitData {
    abstract val bookId: Long
    abstract val publication: Publication
    abstract val navigatorKind: NavigatorKind?
}

@OptIn(ExperimentalReadiumApi::class)
class VisualReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    override val navigatorKind: NavigatorKind?,
    val coroutineScope: CoroutineScope,
    val initialLocation: Locator?,
    val preferences: StateFlow<Configurable.Preferences>?,
) : ReaderInitData()

@ExperimentalMedia2
class MediaReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val mediaNavigator: MediaNavigator,
) : ReaderInitData() {
    override val navigatorKind: NavigatorKind = NavigatorKind.AUDIO
}

class DummyReaderInitData(
    override val bookId: Long,
) : ReaderInitData() {
    override val publication: Publication = Publication(Manifest(
        metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString(""))
    ))

    override val navigatorKind: NavigatorKind? = null
}
