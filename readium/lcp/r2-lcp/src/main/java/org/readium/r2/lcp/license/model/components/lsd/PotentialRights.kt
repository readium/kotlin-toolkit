/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lsd

import org.joda.time.DateTime
import org.json.JSONObject

data class PotentialRights(val json: JSONObject) {
    val end: DateTime?

    init {
        end = if (json.has("end")) DateTime(json.getString("end")) else null
    }
}
