/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub.extensions

import org.readium.r2.shared.util.Url

/**
 * According to the EPUB specification, the HREFs in the EPUB package must be valid URLs (so
 * percent-encoded). Unfortunately, many EPUBs don't follow this rule, and use invalid HREFs such
 * as `my chapter.html` or `/dir/my chapter.html`.
 *
 * As a workaround, we assume the HREFs are valid percent-encoded URLs, and fallback to decoded paths
 * if we can't parse the URL.
 */
internal fun Url.Companion.fromEpubHref(href: String): Url? =
    Url(href) ?: fromDecodedPath(href)
