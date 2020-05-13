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
import androidx.lifecycle.LiveData
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression

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
    fun progressionDidChange(progression: Double) {}
}

interface IR2TTS {
    fun playTextChanged(text: String) {}
    fun playStateChanged(playing: Boolean) {}
    fun dismissScreenReader() {}
}


interface Navigator {

    val currentLocator: LiveData<Locator?>

    fun go(locator: Locator, animated: Boolean = false, completion: () -> Unit = {}): Boolean
    fun go(link: Link, animated: Boolean = false, completion: () -> Unit = {}): Boolean
    fun goForward(animated: Boolean = false, completion: () -> Unit = {}): Boolean
    fun goBackward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

    @Deprecated("Use [currentLocator] instead", ReplaceWith("currentLocator.value"))
    val currentLocation: Locator? get() = currentLocator.value
}

interface NavigatorDelegate {
    @Deprecated("Observe [currentLocator] instead")
    fun locationDidChange(navigator: Navigator? = null, locator: Locator)
}


interface VisualNavigator : Navigator {
    val readingProgression: ReadingProgression

    fun goLeft(animated: Boolean, completion: () -> Unit): Boolean
    fun goRight(animated: Boolean, completion: () -> Unit): Boolean
}


fun VisualNavigator.goLeft(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (readingProgression) {
        ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
            goBackward(animated = animated, completion = completion)

        ReadingProgression.RTL, ReadingProgression.BTT ->
            goForward(animated = animated, completion = completion)
    }
}

fun VisualNavigator.goRight(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (readingProgression) {
        ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
            goForward(animated = animated, completion = completion)

        ReadingProgression.RTL, ReadingProgression.BTT ->
            goBackward(animated = animated, completion = completion)
    }
}
