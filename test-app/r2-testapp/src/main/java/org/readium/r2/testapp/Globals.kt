/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import java.io.File
import java.io.FileFilter
import java.io.InputStream

/**
 * Created by aferditamuriqi on 10/3/17.
 */

/**
 * Global Parameters
 */

lateinit var books: MutableList<Book>
val supportedProfiles = arrayListOf("http://readium.org/lcp/basic-profile", "http://readium.org/lcp/profile-1.0")


/**
 * Extensions
 */

@ColorInt
fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

/**
 * As there are cases where [File.listFiles] returns null even though it is a directory, we return
 * an empty list instead.
 */
fun File.listFilesSafely(filter: FileFilter? = null): List<File> {
    val array: Array<File>? = if (filter == null) listFiles() else listFiles(filter)
    return array?.toList() ?: emptyList()
}

fun InputStream.toFile(path: String) {
    use { input ->
        File(path).outputStream().use { input.copyTo(it) }
    }
}
