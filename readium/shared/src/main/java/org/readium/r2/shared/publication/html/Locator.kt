/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.html

import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

// HTML extensions for [Locator.Locations].
// https://github.com/readium/architecture/blob/master/models/locators/extensions/html.md

/**
 * A CSS Selector.
 */
val Locator.Locations.cssSelector: String?
    get() = this["cssSelector"] as? String

/**
 * [partialCfi] is an expression conforming to the "right-hand" side of the EPUB CFI syntax, that is
 * to say: without the EPUB-specific OPF spine item reference that precedes the first ! exclamation
 * mark (which denotes the "step indirection" into a publication document). Note that the wrapping
 * epubcfi(***) syntax is not used for the [partialCfi] string, i.e. the "fragment" part of the CFI
 * grammar is ignored.
 */
val Locator.Locations.partialCfi: String?
    get() = this["partialCfi"] as? String

/**
 * An HTML DOM range.
 */
val Locator.Locations.domRange: DomRange?
    get() = (this["domRange"] as? Map<*, *>)
        ?.let { DomRange.fromJSON(JSONObject(it)) }
