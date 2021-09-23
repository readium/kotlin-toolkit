/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.extensions.optNullableString

data class Signature(val json: JSONObject) {
    val algorithm: String = json.optNullableString("algorithm") ?: throw LcpException.Parsing.Signature
    val certificate: String = json.optNullableString("certificate") ?: throw LcpException.Parsing.Signature
    val value: String = json.optNullableString("value") ?: throw LcpException.Parsing.Signature
}
