// TODO templated
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
import org.json.JSONObject
import org.readium.r2.lcp.public.ParsingError
import java.net.URL


data class Link(val json: JSONObject) {
    val href: String
    var rel = mutableListOf<String>()
    val title: String?
    val type: String?
    val templated: Boolean
    val profile: String?
    val length: Int?
    val hash: String?

    init {

        href = if (json.has("href")) json.getString("href") else throw ParsingError.link

        if (json.has("rel")) {
            val rel = json["rel"]
            if (rel is String) {
                this.rel.add(rel)
            } else if (rel is JSONArray) {
                for (i in 0 until rel.length()) {
                    this.rel.add(rel[i].toString())
                }
            }
        }

        if (rel.isEmpty()) {
            throw ParsingError.link
        }

        title = if (json.has("title")) json.getString("title") else null
        type = if (json.has("type")) json.getString("type") else null
        templated = if (json.has("templated")) json.getBoolean("templated") else false
        profile = if (json.has("profile")) json.getString("profile") else null
        length = if (json.has("length")) json.getInt("length") else null
        hash = if (json.has("hash")) json.getString("hash") else null

    }


//TODO: needs some more work here
    fun url(parameters:  List<Pair<String, Any?>>) : URL? {
        if (!templated) {
            return URL(href)
        }
//        val registerUrl = URL(url.toString().replace("{?id,name}", ""))
//        val renewUrl = URL(url.toString().replace("{?end,id,name}", ""))

        var sanitized =  URL(href.replace("{?id,name}", ""))
        sanitized =  URL(sanitized.toString().replace("{?end,id,name}", ""))
        return sanitized


//    val urlString = href.replacingOccurrences(of = "\\{\\?.+?\\}", with = "", options = listOf<.regularExpression>)
//        var urlBuilder = URLComponents(string = urlString) ?: return null
//        urlBuilder.queryItems = parameters.map { param  ->
//            URLQueryItem(name = param.key, value = param.value.description)
//        }
//        return urlBuilder.url
    }

    val url: URL?
        get() = url(parameters = listOf())

}