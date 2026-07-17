/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components.lcp

import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.util.json.optNullableInt
import org.readium.r2.shared.util.json.optNullableString

public data class Rights(val json: JsonObject) {
    val print: Int?
    val copy: Int?
    val start: Instant?
    val end: Instant?
    val extensions: JsonObject

    init {
        val clone = json.toMutableMap()

        print = clone.optNullableInt("print", remove = true)
        copy = clone.optNullableInt("copy", remove = true)
        start = clone.optNullableString("start", remove = true)?.toInstant()
        end = clone.optNullableString("end", remove = true)?.toInstant()

        extensions = JsonObject(clone)
    }
}
