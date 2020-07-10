/* Module: r2-testapp-kotlin
* Developers: Quentin Gliosca
*
* Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
* Licensed to the Readium Foundation under one or more contributor license agreements.
* Use of this source code is governed by a BSD-style license which is detailed in the
* LICENSE file present in the project repository where this source code is maintained.
*/

package org.readium.r2.testapp.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.util.File
import java.io.FileOutputStream
import java.net.URL

suspend fun URL.download(path: String): Boolean = tryOr(false) {
    withContext(Dispatchers.IO) {
        openStream().use { input ->
            FileOutputStream(File(path).file).use { output ->
                input.copyTo(output)
            }
        }
    }
    true
}