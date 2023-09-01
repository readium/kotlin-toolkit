/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.UrlHref
import org.readium.r2.shared.util.Url

fun urlHref(url: String): Href =
    UrlHref(Url(url)!!)
