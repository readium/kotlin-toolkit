/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.content.SharedPreferences
import android.view.View
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Publication

interface R2ActivityListener {

    val publication: Publication
    val preferences: SharedPreferences
    val publicationIdentifier: String
    val resourcePager: R2ViewPager?
        get() = null
    val allowToggleActionBar: Boolean
        get() = true

    fun toggleActionBar() {}
    fun toggleActionBar(v: View? = null) {}
    fun storeProgression(locations: Locations?) {}
    fun nextResource(smoothScroll: Boolean) {}
    fun previousResource(smoothScroll: Boolean) {}
    fun nextResource(v: View? = null) {}
    fun previousResource(v: View? = null) {}
    fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {}
    fun onPageEnded(end: Boolean) {}

}