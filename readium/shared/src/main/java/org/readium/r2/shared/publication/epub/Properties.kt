/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.readium.r2.shared.publication.Properties

// EPUB extensions for link [Properties].
// https://readium.org/webpub-manifest/schema/extensions/epub/properties.schema.json

/**
 * Identifies content contained in the linked resource, that cannot be strictly identified using a
 * media type.
 */
val Properties.contains: Set<String>
    get() = (this["contains"] as? List<*>)
        ?.filterIsInstance(String::class.java)
        ?.toSet()
        ?: emptySet()

/**
 * Hints how the layout of the resource should be presented.
 */
val Properties.layout: EpubLayout?
    get() = EpubLayout(this["layout"] as? String)
