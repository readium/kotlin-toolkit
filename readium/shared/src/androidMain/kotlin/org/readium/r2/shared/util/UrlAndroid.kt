/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import android.net.Uri
import com.eygraber.uri.Uri as KmpUri
import java.io.File
import java.net.URI
import java.net.URL
import org.readium.r2.shared.InternalReadiumApi

/**
 * Converts the URL to a [File], if it's a file URL.
 */
public fun AbsoluteUrl.toFile(): File? =
    if (isFile) File(path!!) else null

/**
 * Creates a URL pointing to this [File] which must denote an absolute path.
 *
 * @param isDirectory If the URL must end with a trailing slash because it points to a directory.
 */
public fun File.toUrl(isDirectory: Boolean): AbsoluteUrl {
    require(isAbsolute)

    val uri = KmpUri.Builder().also {
        it.scheme("file")
        it.authority("")
        it.path(path)
        if (isDirectory) it.appendPath("")
    }.build()

    return checkNotNull(AbsoluteUrl(uri))
}

public fun Uri.toUrl(): Url? =
    Url(toKmpUri())

public fun Uri.toAbsoluteUrl(): AbsoluteUrl? =
    AbsoluteUrl(toKmpUri())

public fun Uri.toRelativeUrl(): RelativeUrl? =
    RelativeUrl(toKmpUri())

private fun Uri.toKmpUri(): KmpUri =
    KmpUri.parse(toString())

public fun Url.toUri(): Uri =
    Uri.parse(toString())

@InternalReadiumApi
public fun Url.toURI(): URI =
    URI(toString())

public fun URL.toUrl(): Url? =
    Url(toKmpUri())

public fun URL.toAbsoluteUrl(): AbsoluteUrl? =
    AbsoluteUrl(toKmpUri())

public fun URL.toRelativeUrl(): RelativeUrl? =
    RelativeUrl(toKmpUri())

private fun URL.toKmpUri(): KmpUri =
    KmpUri.parse(toString()).addFileAuthority()

public fun URI.toUrl(): Url? =
    Url(KmpUri.parse(toString()).addFileAuthority())

/**
 * [URL] and [URI] can return a file URL without the empty authority, which is invalid.
 *
 * This method adds the empty authority if needed, for example:
 * `file:/path/to/file` becomes `file:///path/to/file`
 */
private fun KmpUri.addFileAuthority(): KmpUri =
    if (scheme?.lowercase() != "file" || authority != null) {
        this
    } else {
        buildUpon().authority("").build()
    }
