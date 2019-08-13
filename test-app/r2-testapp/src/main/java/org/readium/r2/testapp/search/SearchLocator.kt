package org.readium.r2.testapp.search

import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.LocatorText


/**
 * This class describes a search result
 *
 */

//DO WE STILL NEED TO STORE MARK OBJ AND RANGEINFO?
class SearchLocator(href: String,
                    type: String,
                    title: String? = null,
                    locations: Locations? = null,
                    text: LocatorText?, var mark: String? = null, var rangeInfo: String? = null) :  Locator(href, type, title ,locations, text) {





}