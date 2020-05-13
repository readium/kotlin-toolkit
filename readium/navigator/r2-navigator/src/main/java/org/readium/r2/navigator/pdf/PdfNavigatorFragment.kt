/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.extensions.urlTo
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

class PdfNavigatorFragment(private val publication: Publication) : Fragment() {

    private lateinit var pdfView: PDFView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        pdfView = PDFView(context, null)

        lifecycleScope.launch {
            loadLink(publication.readingOrder.firstOrNull())
        }

        return pdfView
    }

    private suspend fun loadLink(link: Link?) {
        link ?: return
        val url = publication.urlTo(link) ?: return

        // Android forbids network requests on the main thread by default, so we
        // have to do that in the IO dispatcher.
        withContext(Dispatchers.IO) {
            try {
                pdfView.fromStream(url.openStream()).load()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

}
