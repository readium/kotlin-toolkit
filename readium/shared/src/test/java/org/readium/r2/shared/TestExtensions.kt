/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.UrlHref

fun urlHref(url: String): Href =
    UrlHref(Url(url)!!)
