/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.DRM
import java.io.InputStream

/**
 * Container of a publication
 *
 * @var rootFile : a RootFile class containing the path the publication, the version
 *                 and the mime type of it
 *
 * @var drm : contain the brand, scheme, profile and license of DRM if it exist
 *
 * @func data : return the ByteArray content of a file from the publication
 *
 * @func dataLength : return the length of content
 *
 * @func dataInputStream : return the InputStream of content
 */
interface Container {
    var rootFile: RootFile
    var drm: DRM?
    fun contains(relativePath: String): Boolean
    fun data(relativePath: String): ByteArray
    fun dataLength(relativePath: String): Long
    fun dataInputStream(relativePath: String): InputStream
}

sealed class ContainerError : Exception() {
    object streamInitFailed : ContainerError()
    object fileNotFound : ContainerError()
    object fileError : ContainerError()
    data class missingFile(val path: String) : ContainerError()
    data class xmlParse(val underlyingError: Error) : ContainerError()
    data class missingLink(val title: String?) : ContainerError()
}

