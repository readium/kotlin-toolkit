/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)
@file:Suppress("DEPRECATION")

package org.readium.r2.shared.publication.epub

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.MapCompanion

/**
 * Hints how the layout of the resource should be presented.
 * https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json
 */
@Deprecated("This was removed from RWPM. You can still use the EPUB extensibility to access the original values.", replaceWith = ReplaceWith("Layout"))
@Parcelize
public enum class EpubLayout(public val value: String) : Parcelable {
    FIXED("fixed"),
    REFLOWABLE("reflowable"),
    ;

    public companion object : MapCompanion<String, EpubLayout>(
        entries.toTypedArray(),
        EpubLayout::value
    )
}
