/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOrNull
import java.io.File
import java.io.InputStream
import java.util.*


suspend fun InputStream.toFile(path: String) {
    withContext(Dispatchers.IO) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
        }
    }
}

suspend fun InputStream.copyToTempFile(dir: String): File? = tryOrNull {
    val filename = UUID.randomUUID().toString()
    File(dir + filename)
        .also { toFile(it.path) }
}
