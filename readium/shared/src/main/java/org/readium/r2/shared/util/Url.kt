/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import java.io.File
import java.net.URL
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.extensions.tryOrNull

/**
 * A Uniform Resource Locator.
 */
@JvmInline
value class Url internal constructor(internal val url: URL) {

    val scheme: String
        get() = url.protocol

    val authority: String
        get() = url.authority

    val path: String
        get() = url.path

    val filename: String
        get() = File(path).name

    val extension: String?
        get() = url.extension

    override fun toString(): String =
        url.toString()

    companion object {

        operator fun invoke(url: String): Url? =
            tryOrNull { Url(URL(url)) }
    }
}

fun Url.toURL(): URL =
    url

fun Url.isFile(): Boolean =
    scheme == "file"

fun Url.readBytes() = url.openStream().use { it.readBytes() }

fun File.toUrl(): Url =
    Url(toURI().toURL()!!)

fun Uri.toUrl(): Url? =
    Url(toString())
