/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

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
@Deprecated(
    "Use [publication.get()] to access publication content.",
    level = DeprecationLevel.ERROR
)
public interface Container {
    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public fun data(relativePath: String): ByteArray

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public fun dataLength(relativePath: String): Long

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public fun dataInputStream(relativePath: String): InputStream
}

public sealed class ContainerError : Exception() {
    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public object streamInitFailed : ContainerError()

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public object fileNotFound : ContainerError()

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public object fileError : ContainerError()

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public data class missingFile(public val path: String) : ContainerError()

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public data class xmlParse(public val underlyingError: Error) : ContainerError()

    @Deprecated(
        "Use [publication.get()] to access publication content.",
        level = DeprecationLevel.ERROR
    )
    public data class missingLink(public val title: String?) : ContainerError()
}
