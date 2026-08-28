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
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.optNullableString

public data class Signature(val json: JSONObject) {
    val algorithm: String = json.optNullableString("algorithm") ?: throw LcpException(
        LcpError.Parsing.Signature
    )
    val certificate: String = json.optNullableString("certificate") ?: throw LcpException(
        LcpError.Parsing.Signature
    )
    val value: String = json.optNullableString("value") ?: throw LcpException(
        LcpError.Parsing.Signature
    )
}
