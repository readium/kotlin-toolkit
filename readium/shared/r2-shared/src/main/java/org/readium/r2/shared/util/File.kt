/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents a path on the file system.
 *
 * Used to cache the Format to avoid computing it at different locations.
 */
open class File private constructor(val file: java.io.File) {

    private var knownMediaType: MediaType? = null
    private var mediaTypeHint: String? = null

    private constructor(file: java.io.File, mediaType: MediaType? = null, mediaTypeHint: String? = null) : this(file) {
        this.knownMediaType = mediaType
        this.mediaTypeHint = mediaTypeHint
    }

    /**
     * Creates a File from a path and a media type hint..
     *
     * @param path Absolute path to the file or directory.
     * @param mediaType If the file's media type is already known, providing it will improve performances.
     */
    constructor(path: String, mediaType: String? = null) :
            this(java.io.File(path), mediaTypeHint = mediaType)

    /**
     *  Creates a File from a path and an already resolved media type.
     */
    constructor(path: String, mediaType: MediaType?) :
            this(java.io.File(path), mediaType = mediaType)

    /**
     * Absolute path on the file system.
     */
    val path: String = file.absolutePath

    /**
     * Last path component, or filename.
     */
    val name: String = file.name

    /**
     * Whether the path points to a directory.
     *
     * This can be used to open exploded publication archives.
     */
    val isDirectory: Boolean get() = file.isDirectory

    private lateinit var _mediaType: Try<MediaType, Exception>

    /**
     * Media type, if the path points to a file.
     */
    suspend fun mediaType(): MediaType {
        if (!::_mediaType.isInitialized) {
            val mediaType = knownMediaType
                ?: MediaType.ofFile(file, mediaTypeHint)
            _mediaType = mediaType
                ?.let { Try.success(it) }
                ?: Try.failure(Exception("Unable to detect media type."))
        }

        return _mediaType.getOrDefault(MediaType.BINARY)
    }

    @Deprecated("Renamed mediaType()", replaceWith = ReplaceWith("mediaType()"), level = DeprecationLevel.ERROR)
    suspend fun format(): MediaType? = mediaType()

}
