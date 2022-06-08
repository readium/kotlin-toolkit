/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.PdfDocumentFactory

@PdfSupport
internal class PdfNavigatorViewModel(
    application: Application,
    private val publication: Publication,
    initialLocator: Locator,
) : AndroidViewModel(application) {

    private val _currentDocument = MutableStateFlow(publication.readingOrder.first())
    val currentDocument: StateFlow<Link> = _currentDocument.asStateFlow()

    private val _currentLocator = MutableStateFlow(initialLocator)
    val currentLocator: StateFlow<Locator> = _currentLocator.asStateFlow()

    fun onPageChanged(pageIndex: Int) = viewModelScope.launch {
        publication.positions().getOrNull(pageIndex)
            ?.let { _currentLocator.value = it }
    }

    companion object {
        @OptIn(InternalReadiumApi::class)
        fun createFactory(
            application: Application,
            publication: Publication,
            initialLocator: Locator?,
        ) = createViewModelFactory {
            val locator = initialLocator
                ?: publication.readingOrder.firstOrNull()?.let { publication.locatorFromLink(it) }
                ?: Locator(href = "#", type = MediaType.PDF.toString())

            PdfNavigatorViewModel(application, publication, locator)
        }
    }
}