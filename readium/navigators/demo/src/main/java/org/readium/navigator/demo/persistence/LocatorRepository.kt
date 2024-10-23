package org.readium.navigator.demo.persistence

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
