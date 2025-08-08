/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.persistence

import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl

object LocatorRepository {

    private val savedLocators: MutableMap<AbsoluteUrl, Locator> =
        mutableMapOf()

    fun saveLocator(url: AbsoluteUrl, locator: Locator) {
        savedLocators[url] = locator
    }

    fun getLocator(url: AbsoluteUrl): Locator? {
        return savedLocators[url]
    }
}
