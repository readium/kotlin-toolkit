/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import java.nio.charset.Charset

/**
 * Bundle of media type and file extension hints for the MediaTypeSniffer.
 */
@Suppress("UNUSED_PARAMETER")
@Deprecated(
    "Use FormatHints instead.",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("FormatHints")
)
public data class MediaTypeHints(
    val mediaTypes: List<MediaType> = emptyList(),
    val fileExtensions: List<String> = emptyList()
) {
    public companion object {
        public operator fun invoke(mediaType: MediaType? = null, fileExtension: String? = null): Any {
            throw NotImplementedError()
        }

        public operator fun invoke(
            mediaTypes: List<String> = emptyList(),
            fileExtensions: List<String> = emptyList()
        ): Any {
            throw NotImplementedError()
        }
    }

    public operator fun plus(other: Any): Any {
        throw NotImplementedError()
    }

    /**
     * Returns a new [MediaTypeHints] after appending the given [fileExtension] hint.
     */
    public fun addFileExtension(fileExtension: String?): Any {
        throw NotImplementedError()
    }

    /** Finds the first [Charset] declared in the media types' `charset` parameter. */
    public val charset: Charset? get() =
        mediaTypes.firstNotNullOfOrNull { it.charset }

    /** Returns whether this context has any of the given file extensions, ignoring case. */
    public fun hasFileExtension(vararg fileExtensions: String): Boolean {
        val fileExtensionsHints = this.fileExtensions.map { it.lowercase() }
        for (fileExtension in fileExtensions.map { it.lowercase() }) {
            if (fileExtensionsHints.contains(fileExtension)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns whether this context has any of the given media type, ignoring case and extra
     * parameters.
     *
     * Implementation note: Use [MediaType] to handle the comparison to avoid edge cases.
     */
    public fun hasMediaType(vararg mediaTypes: String): Boolean {
        @Suppress("NAME_SHADOWING")
        val mediaTypes = mediaTypes.mapNotNull { MediaType(it) }
        for (mediaType in mediaTypes) {
            if (this.mediaTypes.any { mediaType.contains(it) }) {
                return true
            }
        }
        return false
    }
}
