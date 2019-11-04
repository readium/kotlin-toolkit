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
import org.readium.r2.shared.Link
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication
import java.net.URL

interface R2ActivityListener {

    val publication: Publication
    val preferences: SharedPreferences
    val publicationIdentifier: String
    val publicationFileName: String
    val publicationPath: String
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


public interface Navigator {
    val currentLocation: Locator?
    fun go(locator: Locator, animated: Boolean, completion: () -> Unit) : Boolean
    fun go(link: Link, animated: Boolean, completion: () -> Unit) : Boolean
    fun goForward(animated: Boolean, completion: () -> Unit) : Boolean
    fun goBackward(animated: Boolean, completion: () -> Unit) : Boolean

}

public fun Navigator.go(locator: Locator, animated: Boolean = false, completion: () -> Unit = {}) : Boolean =
        go(locator = locator, animated = animated, completion = completion)

public fun Navigator.go(link: Link, animated: Boolean = false, completion: () -> Unit = {}) : Boolean =
        go(link = link, animated = animated, completion = completion)

public fun Navigator.goForward(animated: Boolean = false, completion: () -> Unit = {}) : Boolean =
        goForward(animated = animated, completion = completion)

public fun Navigator.goBackward(animated: Boolean = false, completion: () -> Unit = {}) : Boolean =
        goBackward(animated = animated, completion = completion)



public interface NavigatorDelegate {
    fun navigator(navigator: Navigator, locator: Locator)
    fun navigator(navigator: Navigator, error: NavigatorError)
    fun navigator(navigator: Navigator, url: URL)
}


//public fun NavigatorDelegate.navigator(navigator: Navigator, url: URL) {
//    if (UIApplication.shared.canOpenURL(url)) {
//        UIApplication.shared.openURL(url)
//    }
//}



sealed class NavigatorError : Exception() {
    object copyForbidden : NavigatorError()

    val errorDescription: String?
        get() {
            return when (this) {
                is copyForbidden -> "NavigatorError.copyForbidden"
            }
        }
}



public enum class ReadingProgression (val rawValue: String) {
    rtl("rtl"), ltr("ltr"), auto("auto");

    companion object {
        operator fun invoke(rawValue: String) = ReadingProgression.values().firstOrNull { it.rawValue == rawValue }
    }
}


public interface VisualNavigator: Navigator {
//    val view: UIView
    val readingProgression: ReadingProgression
    fun goLeft(animated: Boolean, completion: () -> Unit) : Boolean
    fun goRight(animated: Boolean, completion: () -> Unit) : Boolean
}


public fun VisualNavigator.goLeft(animated: Boolean = false, completion: () -> Unit = {}) : Boolean {
    return when (readingProgression) {
        ReadingProgression.ltr -> goBackward(animated = animated, completion = completion)
        ReadingProgression.auto -> goBackward(animated = animated, completion = completion)
        ReadingProgression.rtl -> goForward(animated = animated, completion = completion)
    }
}

public fun VisualNavigator.goRight(animated: Boolean = false, completion: () -> Unit = {}) : Boolean {
    return when (readingProgression) {
        ReadingProgression.ltr -> goForward(animated = animated, completion = completion)
        ReadingProgression.auto -> goForward(animated = animated, completion = completion)
        ReadingProgression.rtl -> goBackward(animated = animated, completion = completion)
    }
}


//public interface VisualNavigatorDelegate: NavigatorDelegate {
//    fun navigator(navigator: VisualNavigator, point: CGPoint)
//}

//public fun VisualNavigatorDelegate.navigator(navigator: VisualNavigator, point: CGPoint) {}



