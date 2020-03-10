/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components

import org.json.JSONArray

data class Links(val json: JSONArray) {

    var links:MutableList<Link> = mutableListOf()

    init {
        for (i in 0 until json.length()) {
            links.add(Link(json.getJSONObject(i)))
        }
    }

    operator fun get(rel: String): List<Link> {
        return links.filter { it.rel.contains(rel) }
    }

}
