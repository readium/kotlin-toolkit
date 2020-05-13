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
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.pdf.PdfNavigatorViewModel.GoToLocationEvent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import timber.log.Timber

class PdfNavigatorFragment(viewModelFactory: ViewModelProvider.Factory) : Fragment(), Navigator {

    private lateinit var pdfView: PDFView

    private val viewModel: PdfNavigatorViewModel by viewModels { viewModelFactory }

    private var currentHref: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        pdfView = PDFView(context, null)

        viewModel.goToLocation.observe(viewLifecycleOwner, Observer { event ->
            event ?: return@Observer

            lifecycleScope.launch {
                goTo(event)
                viewModel.goToLocation.value = null
            }
        })

        return pdfView
    }

    private suspend fun goTo(event: GoToLocationEvent) {
        if (currentHref == event.href) {
            pdfView.jumpTo(event.page, event.animated)

        } else {
            // Android forbids network requests on the main thread by default, so we have to do that
            // in the IO dispatcher.
            withContext(Dispatchers.IO) {
                try {
                    pdfView
                        .fromStream(event.url.openStream())
                        .defaultPage(event.page)
                        .onPageChange { page, pageCount ->
                            if (isAdded) {
                                viewModel.onPageChanged(event.href, page = page, pageCount = pageCount)
                            }
                        }
                        .load()

                    currentHref = event.href
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    override val currentLocator: LiveData<Locator?> get() =
        viewModel.currentLocator

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean =
        viewModel.goTo(locator, animated, completion)

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        viewModel.goTo(link, animated, completion)

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean =
        viewModel.goForward(animated, completion)

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean =
        viewModel.goBackward(animated, completion)

}
