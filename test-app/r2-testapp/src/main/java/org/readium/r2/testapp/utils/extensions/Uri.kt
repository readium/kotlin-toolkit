/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import android.content.Context
import android.net.Uri
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.utils.ContentResolverUtil
import java.io.File
import java.util.*

suspend fun Uri.copyToTempFile(context: Context, dir: String): File? = tryOrNull {
    val filename = UUID.randomUUID().toString()
    val mediaType = MediaType.ofUri(this, context.contentResolver)
    val path = "$dir$filename.${mediaType?.fileExtension ?: "tmp"}"
    ContentResolverUtil.getContentInputStream(context, this, path)
    return File(path)
}