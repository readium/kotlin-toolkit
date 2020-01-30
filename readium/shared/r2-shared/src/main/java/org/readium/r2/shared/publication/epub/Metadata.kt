/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.readium.r2.shared.publication.Metadata

// EPUB extensions for [Metadata].
// https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json

/**
 * Hints how the layout of the resource should be presented.
 */
val Metadata.layout: EpubLayout?
    get() = EpubLayout.from(this["layout"] as? String)
