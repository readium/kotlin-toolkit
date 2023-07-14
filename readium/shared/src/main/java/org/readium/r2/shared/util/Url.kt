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
value class Url private constructor(internal val uri: Uri) {

    val scheme: String
        get() = uri.scheme!!

    val authority: String
        get() = uri.authority!!

    val path: String
        get() = uri.path!!

    val filename: String
        get() = File(path).name

    val extension: String?
        get() = File(path).extension
            .takeIf { it.isNotEmpty() }

    override fun toString(): String =
        uri.toString()

    companion object {

        operator fun invoke(url: String): Url? =
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

fun Url.isFile(): Boolean =
    scheme == "file"

fun Url.isHttp(): Boolean =
    scheme == "http" || scheme == "https"

fun File.toUrl(): Url =
    Url(Uri.fromFile(this))!!

fun Uri.toUrl(): Url? =
    Url.invoke(this)

fun Url.toUri() =
    uri
