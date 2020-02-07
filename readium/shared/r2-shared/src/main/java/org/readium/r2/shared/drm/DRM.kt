/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.drm

import org.readium.r2.shared.util.KeyMapper
import java.io.Serializable

data class DRM(val brand: Brand): Serializable  {
    val scheme: Scheme
    var license: DRMLicense? = null

    enum class Brand(val rawValue: String): Serializable  {
        lcp("lcp");

        companion object : KeyMapper<String, Brand>(values(), Brand::rawValue)

    }

    enum class Scheme(val rawValue: String): Serializable  {
        lcp("http://readium.org/2014/01/lcp");

        companion object : KeyMapper<String, Scheme>(values(), Scheme::rawValue)

    }

    init {
        when (brand) {
            Brand.lcp -> scheme = Scheme.lcp
        }
    }
}

interface DRMLicense: Serializable {
    val encryptionProfile: String?
    fun decipher(data: ByteArray): ByteArray?
    val canCopy: Boolean
    fun copy(text: String): String?
}

val DRMLicense.encryptionProfile: String?
    get() = null

val DRMLicense.canCopy: Boolean
    get() = true

fun DRMLicense.copy(text: String): String? =
        if (canCopy) text else null
