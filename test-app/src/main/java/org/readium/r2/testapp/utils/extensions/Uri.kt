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
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.content.ContentResolverError
import org.readium.r2.testapp.utils.toFile
import org.readium.r2.testapp.utils.tryOrNull

suspend fun Uri.copyToTempFile(context: Context, dir: File): Try<File, ContentResolverError> {
    val filename = UUID.randomUUID().toString()
    val file = File(dir, "$filename.${extension(context)}")

    val inputStream = try {
        context.contentResolver.openInputStream(this)
    } catch (e: FileNotFoundException) {
        return Try.failure(ContentResolverError.FileNotFound(e))
    } ?: return Try.failure(ContentResolverError.NotAvailable())

    try {
        inputStream.toFile(file)
    } catch (e: IOException) {
        return Try.failure(ContentResolverError.IO(e))
    }

    return Try.success(file)
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
