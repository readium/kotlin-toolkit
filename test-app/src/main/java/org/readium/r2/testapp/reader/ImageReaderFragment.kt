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
import androidx.fragment.app.commitNow
import org.readium.navigator.image.ImageNavigatorFragment
import org.readium.navigator.image.preferences.ImagePreferences
import org.readium.navigator.image.preferences.ImagePreferencesEditor
import org.readium.navigator.image.preferences.ImageSettings
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel

@OptIn(ExperimentalReadiumApi::class)
class ImageReaderFragment : VisualReaderFragment(), ImageNavigatorFragment.Listener {

    override lateinit var navigator: ImageNavigatorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val readerData = model.readerInitData as ImageReaderInitData

        val navigatorState = readerData.navigatorFactory.createNavigatorState(readerData.initialLocation)

        childFragmentManager.fragmentFactory =
            ImageNavigatorFragment.createFactory(navigatorState, this)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view =  super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.fragment_reader_container, ImageNavigatorFragment::class.java, Bundle(), NAVIGATOR_FRAGMENT_TAG)
            }
        }
        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG)!! as ImageNavigatorFragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("Unchecked_cast")
        (model.settings as UserPreferencesViewModel<ImageSettings, ImagePreferences, ImagePreferencesEditor>)
            .bind(navigator, viewLifecycleOwner)
    }

    companion object {

        const val NAVIGATOR_FRAGMENT_TAG = "navigator"
    }
}