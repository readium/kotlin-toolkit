/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.utils.ContentResolverUtil

suspend fun Uri.copyToTempFile(context: Context, dir: File): Try<File, Exception> =
    try {
        val filename = UUID.randomUUID().toString()
        val mediaType = MediaType.ofUri(this, context.contentResolver)
        val file = File(dir, "$filename.${mediaType?.fileExtension ?: "tmp"}")
        ContentResolverUtil.getContentInputStream(context, this, file)
        Try.success(file)
    } catch (e: Exception) {
        Try.failure(e)
    }
