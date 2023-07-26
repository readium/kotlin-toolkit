/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import java.io.File
import org.readium.r2.shared.extensions.tryOrNull

/**
 * A Uniform Resource Locator.
 */
@JvmInline
public value class Url private constructor(internal val uri: Uri) {

    public val scheme: String
        get() = uri.scheme!!

    public val authority: String
        get() = uri.authority!!

    public val path: String
        get() = uri.path!!

    public val filename: String
        get() = File(path).name

    public val extension: String?
        get() = File(path).extension
            .takeIf { it.isNotEmpty() }

    override fun toString(): String =
        uri.toString()

    public companion object {

        public operator fun invoke(url: String): Url? =
            invoke(Uri.parse(url))

        internal operator fun invoke(uri: Uri): Url? =
            tryOrNull {
                requireNotNull(uri.scheme)
                requireNotNull(uri.authority)
                requireNotNull(uri.path)
                Url(uri)
            }
    }
}

public fun Url.isFile(): Boolean =
    scheme == "file"

public fun Url.isHttp(): Boolean =
    scheme == "http" || scheme == "https"

public fun File.toUrl(): Url =
    Url(Uri.fromFile(this))!!

public fun Uri.toUrl(): Url? =
    Url.invoke(this)

public fun Url.toUri(): Uri =
    uri
