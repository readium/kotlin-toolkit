/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.extensions

import org.readium.r2.shared.util.Language

internal val Language.isRtl: Boolean get() {
    val c = code.lowercase()
    return c == "ar" ||
        c == "fa" ||
        c == "he" ||
        c == "zh-hant" ||
        c == "zh-tw"
}

internal val Language.isCjk: Boolean get() {
    val c = code.lowercase()
    return c == "ja" ||
        c == "ko" ||
        removeRegion().code.lowercase() == "zh"
}
