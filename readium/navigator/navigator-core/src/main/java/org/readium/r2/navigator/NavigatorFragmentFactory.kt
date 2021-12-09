/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import androidx.fragment.app.FragmentFactory
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Factory for the Navigator Fragments.
 *
 * @param publication Publication to render in the navigator.
 * @param baseUrl A base URL where this publication is served from, when relevant. This is required
 *        only for an EPUB publication.
 * @param initialLocator The first location which should be visible when rendering the publication.
 *        Can be used to restore the last reading location.
 * @param listener Optional listener to implement to observe events, such as user taps.
 */
@Deprecated("Each [Fragment] has now its own factory, such as `EpubNavigatorFragment.createFactory()`. To use a single [Activity] with several navigator fragments, you can compose the factories with [CompositeFragmentFactory].", level = DeprecationLevel.ERROR)
class NavigatorFragmentFactory(
    private val publication: Publication,
    private val baseUrl: String? = null,
    private val initialLocator: Locator? = null,
    private val listener: Navigator.Listener? = null
) : FragmentFactory()
