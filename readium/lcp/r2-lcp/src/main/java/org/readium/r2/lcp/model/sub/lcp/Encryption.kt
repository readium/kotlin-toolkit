/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.sub.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class Encryption(json: JSONObject) {
    var profile: URL
    private var contentKey: ContentKey
    var userKey: UserKey

    init {
        try {
            profile = URL(json.getString("profile"))
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.encryption))
        }
        contentKey = ContentKey(json.getJSONObject("content_key"))
        userKey = UserKey(json.getJSONObject("user_key"))
    }
}