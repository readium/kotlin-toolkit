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

import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.json.optJsonArray
import org.readium.r2.shared.util.json.optString
import org.readium.r2.shared.util.json.stringOrNull

@OptIn(InternalReadiumApi::class)
public data class User(val json: JsonObject) {
    val id: String?
    val email: String?
    val name: String?
    var extensions: JsonObject
    var encrypted: MutableList<String> = mutableListOf<String>()

    init {
        id = if ("id" in json) json.optString("id") else null
        email = if ("email" in json) json.optString("email") else null
        name = if ("name" in json) json.optString("name") else null

        json.optJsonArray("encrypted")?.let { encryptedArray ->
            for (element in encryptedArray) {
                element.stringOrNull?.let { encrypted.add(it) }
            }
        }

        extensions = json
    }
}
