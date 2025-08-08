/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import org.readium.r2.shared.util.Language

public val Language.isRtl: Boolean get() {
    val c = code.lowercase()
    return c == "ar" ||
        c == "fa" ||
        c == "he" ||
        c == "zh-hant" ||
        c == "zh-tw"
}

public val Language.isCjk: Boolean get() {
    val c = code.lowercase()
    return c == "ja" ||
        c == "ko" ||
        removeRegion().code.lowercase() == "zh"
}
