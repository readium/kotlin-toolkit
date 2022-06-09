/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import android.graphics.PointF
import androidx.fragment.app.Fragment
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

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

    data class Settings(
        val fit: Presentation.Fit? = null,
        val overflow: Presentation.Overflow = Presentation.Overflow.DEFAULT,
        val readingProgression: ReadingProgression = ReadingProgression.AUTO
    )

    interface Listener {
        fun onPageChanged(pageIndex: Int)
        fun onTap(point: PointF): Boolean
    }

    abstract val pageIndex: Int
    abstract fun goToPageIndex(index: Int, animated: Boolean): Boolean
}
