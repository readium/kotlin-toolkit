/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

public data class MediaTypeHints(
    val mediaTypes: List<MediaType> = emptyList(),
    val fileExtensions: List<String> = emptyList()
) {
    public companion object {
        public operator fun invoke(mediaType: MediaType? = null, fileExtension: String? = null): MediaTypeHints =
            MediaTypeHints(
                mediaTypes = listOfNotNull(mediaType),
                fileExtensions = listOfNotNull(fileExtension)
            )

        public operator fun invoke(
            mediaTypes: List<String> = emptyList(),
            fileExtensions: List<String> = emptyList()
        ): MediaTypeHints =
            MediaTypeHints(mediaTypes.mapNotNull { MediaType(it) }, fileExtensions = fileExtensions)
    }

    public operator fun plus(other: MediaTypeHints): MediaTypeHints =
        MediaTypeHints(
            mediaTypes = mediaTypes + other.mediaTypes,
            fileExtensions = fileExtensions + other.fileExtensions
        )
}
