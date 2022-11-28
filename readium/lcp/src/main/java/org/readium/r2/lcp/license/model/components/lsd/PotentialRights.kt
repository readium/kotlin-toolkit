/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lsd

import java.util.*
import org.json.JSONObject
import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.optNullableString

data class PotentialRights(val json: JSONObject) {
    val end: Date? = json.optNullableString("end")?.iso8601ToDate()
}
