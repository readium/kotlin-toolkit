/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.*
import java.net.URL

sealed class ReaderInitData {
    abstract val bookId: Long
    abstract val publication: Publication
}

data class VisualReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val baseUrl: URL? = null,
    val initialLocation: Locator? = null
) : ReaderInitData()

@ExperimentalMedia2
data class MediaReaderInitData(
    override val bookId: Long,
    override val publication: Publication,
    val mediaNavigator: MediaNavigator,
) : ReaderInitData()

data class DummyReaderInitData(
    override val bookId: Long,
) : ReaderInitData() {
    override val publication: Publication = Publication(Manifest(
        metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString(""))
    ))
}
