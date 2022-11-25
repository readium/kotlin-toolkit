/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lcp

import java.util.*
import org.json.JSONObject
import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.optNullableInt
import org.readium.r2.shared.extensions.optNullableString

data class Rights(val json: JSONObject) {
    val print: Int?
    val copy: Int?
    val start: Date?
    val end: Date?
    val extensions: JSONObject

    init {
        val clone = JSONObject(json.toString())

        print = clone.optNullableInt("print", remove = true)
        copy = clone.optNullableInt("copy", remove = true)
        start = clone.optNullableString("start", remove = true)?.iso8601ToDate()
        end = clone.optNullableString("end", remove = true)?.iso8601ToDate()

        extensions = clone
    }
}
