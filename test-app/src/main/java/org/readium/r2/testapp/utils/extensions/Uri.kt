/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.util.*
import org.readium.r2.shared.util.Try
import org.readium.r2.testapp.utils.ContentResolverUtil
import org.readium.r2.testapp.utils.tryOrNull

suspend fun Uri.copyToTempFile(context: Context, dir: File): Try<File, Exception> =
    try {
        val filename = UUID.randomUUID().toString()
        val file = File(dir, "$filename.${extension(context)}")
        ContentResolverUtil.getContentInputStream(context, this, file)
        Try.success(file)
    } catch (e: Exception) {
        Try.failure(e)
    }

private fun Uri.extension(context: Context): String? {
    if (scheme == ContentResolver.SCHEME_CONTENT) {
        tryOrNull {
            context.contentResolver.queryProjection(this, MediaStore.MediaColumns.DISPLAY_NAME)
                ?.let { filename ->
                    File(filename).extension
                        .takeUnless { it.isBlank() }
                }
        }?.let { return it }
    }

    return path?.let { File(it).extension }
}

private fun ContentResolver.queryProjection(uri: Uri, projection: String): String? =
    tryOrNull<String?> {
        query(uri, arrayOf(projection), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
            return null
        }
    }
