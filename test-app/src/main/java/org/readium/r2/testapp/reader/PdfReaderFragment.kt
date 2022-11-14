/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.commitNow
import org.readium.adapters.pdfium.navigator.PdfiumPreferences
import org.readium.adapters.pdfium.navigator.PdfiumPreferencesEditor
import org.readium.adapters.pdfium.navigator.PdfiumSettings
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel

@OptIn(ExperimentalReadiumApi::class)
class PdfReaderFragment : VisualReaderFragment(), PdfNavigatorFragment.Listener {

    override lateinit var navigator: PdfNavigatorFragment<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor>

    override fun onCreate(savedInstanceState: Bundle?) {
        val readerData = model.readerInitData as PdfReaderInitData

        childFragmentManager.fragmentFactory =
            readerData.navigatorFactory.createFragmentFactory(
                initialLocator = readerData.initialLocation,
                initialPreferences = readerData.preferencesManager.preferences.value,
                listener = this,
            )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                replace(R.id.fragment_reader_container, PdfNavigatorFragment::class.java, Bundle(), NAVIGATOR_FRAGMENT_TAG)
            }
        }
        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG)!!
                as PdfNavigatorFragment<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor>
        @Suppress("Unchecked_cast")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("Unchecked_cast")
        (model.settings as UserPreferencesViewModel<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor>)
            .bind(navigator, viewLifecycleOwner)
    }

    override fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
        val message = when (error) {
            is Resource.Exception.OutOfMemory -> "The PDF is too large to be rendered on this device"
            else -> "Failed to render this PDF"
        }
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()

        // There's nothing we can do to recover, so we quit the Activity.
        requireActivity().finish()
    }

    companion object {

        const val NAVIGATOR_FRAGMENT_TAG = "navigator"
    }
}