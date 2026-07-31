/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import timber.log.Timber

public fun script(src: Url): String =
    """<script type="text/javascript" src="$src"></script>"""

public fun String.inject(
    sourceUrl: AbsoluteUrl?,
    injectables: List<String>,
): String {
    val headEndIndex = this.indexOf("</head>", 0, true)
    return if (headEndIndex == -1) {
        Timber.e("</head> closing tag not found in resource with href: $sourceUrl")
        this
    } else {
        StringBuilder(this)
            .insert(headEndIndex, "\n" + injectables.joinToString("\n") + "\n")
            .toString()
    }
}
