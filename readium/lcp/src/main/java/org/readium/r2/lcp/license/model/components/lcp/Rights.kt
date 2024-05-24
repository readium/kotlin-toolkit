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

import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.optNullableInt
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.Instant

public data class Rights(val json: JSONObject) {
    val print: Int?
    val copy: Int?
    val start: Instant?
    val end: Instant?
    val extensions: JSONObject

    init {
        val clone = JSONObject(json.toString())

        print = clone.optNullableInt("print", remove = true)
        copy = clone.optNullableInt("copy", remove = true)
        start = clone.optNullableString("start", remove = true)?.let { Instant.parse(it) }
        end = clone.optNullableString("end", remove = true)?.let { Instant.parse(it) }

        extensions = clone
    }
}
