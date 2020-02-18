/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.readium.r2.shared.util.MapCompanion

/**
 * Hints how the layout of the resource should be presented.
 * https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json
 */
@Parcelize
enum class EpubLayout(val value: String) : Parcelable {
    FIXED("fixed"),
    REFLOWABLE("reflowable");

    companion object : MapCompanion<String, EpubLayout>(values(), EpubLayout::value) {

        @Deprecated("Renamed to [FIXED]", ReplaceWith("EpubLayout.FIXED"))
        val Fixed: EpubLayout get() = FIXED
        @Deprecated("Renamed to [REFLOWABLE]", ReplaceWith("EpubLayout.REFLOWABLE"))
        val Reflowable: EpubLayout get() = REFLOWABLE

    }

}
