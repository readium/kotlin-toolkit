/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

/**
 * Hints how the layout of the resource should be presented.
 * https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json
 */
enum class EpubLayout(val value: String) {
    FIXED("fixed"),
    REFLOWABLE("reflowable");

    companion object {

        fun from(value: String?) = EpubLayout.values().firstOrNull { it.value == value }

        /**
         * Resolves from an EPUB rendition:layout property.
         */
        fun fromEpub(value: String, fallback: EpubLayout = REFLOWABLE): EpubLayout {
            return when(value) {
                "reflowable" -> REFLOWABLE
                "pre-paginated" -> FIXED
                else -> fallback
            }
        }

        @Deprecated("Renamed to [FIXED]", ReplaceWith("FIXED"))
        val Fixed: EpubLayout get() = FIXED
        @Deprecated("Renamed to [REFLOWABLE]", ReplaceWith("REFLOWABLE"))
        val Reflowable: EpubLayout get() = REFLOWABLE

    }

}
