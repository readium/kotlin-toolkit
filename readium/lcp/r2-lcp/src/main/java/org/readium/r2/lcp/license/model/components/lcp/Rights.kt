// TODO: extensions
/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lcp

import org.joda.time.DateTime
import org.json.JSONObject

data class Rights(val json: JSONObject) {
    val print: Int?
    val copy: Int?
    val start: DateTime?
    val end: DateTime?
    val extensions: JSONObject

    init {
        print = if (json.has("print")) json.getInt("print") else null
        copy = if (json.has("copy")) json.getInt("copy") else null
        start = if (json.has("start")) DateTime(json.getString("start")) else null
        end = if (json.has("end")) DateTime(json.getString("end")) else null

//        json.remove("print")
//        json.remove("copy")
//        json.remove("start")
//        json.remove("end")

        extensions = json
    }
}
