/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.graphics.PointF
import androidx.fragment.app.Fragment
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import java.lang.Exception

@PdfSupport
typealias PdfDocumentFragmentFactory = suspend (
    publication: Publication,
    link: Link,
    initialPageIndex: Int,
    settings: PdfDocumentFragment.Settings,
    listener: PdfDocumentFragment.Listener?
) -> PdfDocumentFragment

@PdfSupport
abstract class PdfDocumentFragment : Fragment() {

    interface Listener {
        fun onPageChanged(pageIndex: Int)
        fun onTap(point: PointF): Boolean
        fun onResourceLoadFailed(link: Link, error: Resource.Exception)
    }

    abstract val pageIndex: Int
    abstract fun goToPageIndex(index: Int, animated: Boolean): Boolean

    data class Settings(
        val fit: Presentation.Fit? = null,
        val overflow: Presentation.Overflow = Presentation.Overflow.DEFAULT,
        val readingProgression: ReadingProgression = ReadingProgression.AUTO
    )

    abstract var settings: Settings
}
