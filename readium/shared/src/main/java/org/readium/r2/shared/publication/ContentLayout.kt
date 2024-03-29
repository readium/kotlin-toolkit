/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("DEPRECATION")

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The [ContentLayout] defines how a [Publication] should be laid out, based on the declared
 * [ReadingProgression] and its language.
 *
 * Since [ReadingProgression] might be declared as [AUTO] or undefined in the Readium Web
 * Publication Manifest, [ContentLayout] is useful to determine the actual layout.
 *
 * @param cssId Identifier for this layout style, shared with ReadiumCSS.
 */
@Parcelize
@Deprecated("Use `Metadata.effectiveReadingProgression` instead", level = DeprecationLevel.ERROR)
public enum class ContentLayout(private val cssId: String) : Parcelable {
    // Right to left
    RTL("rtl"),

    // Left to right
    LTR("ltr"),

    // Asian language, vertically laid out
    CJK_VERTICAL("cjk-vertical"),

    // Asian language, horizontally laid out
    CJK_HORIZONTAL("cjk-horizontal");
}
