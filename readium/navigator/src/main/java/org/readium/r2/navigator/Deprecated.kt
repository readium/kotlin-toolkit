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
import org.readium.r2.shared.publication.Publication

@Deprecated("Use navigator fragments.", level = DeprecationLevel.ERROR)
interface IR2Activity {

    val publication: Publication
    val preferences: SharedPreferences
    val publicationIdentifier: String
    val publicationFileName: String
    val publicationPath: String
    val bookId: Long
    val resourcePager: R2ViewPager?
        get() = null
    val allowToggleActionBar: Boolean
        get() = true

    fun toggleActionBar() {}
    fun toggleActionBar(v: View? = null) {}
    fun nextResource(v: View? = null) {}
    fun previousResource(v: View? = null) {}
    fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {}
    fun onPageEnded(end: Boolean) {}
    fun onPageLoaded() {}
    fun highlightActivated(id: String) {}
    fun highlightAnnotationMarkActivated(id: String) {}
}

@Deprecated("Use TtsNavigator.", level = DeprecationLevel.ERROR)
interface IR2TTS {
    fun playTextChanged(text: String) {}
    fun playStateChanged(playing: Boolean) {}
    fun dismissScreenReader() {}
}
