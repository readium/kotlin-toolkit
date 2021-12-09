/*
 * Module: r2-navigator-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.extensions

import android.net.Uri
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

internal fun Link.withBaseUrl(baseUrl: String): Link {
    // Already an absolute URL?
    if (Uri.parse(href).scheme != null) {
        return this
    }

    check(!baseUrl.endsWith("/"))
    check(href.startsWith("/"))
    return copy(href = baseUrl + href)
}
