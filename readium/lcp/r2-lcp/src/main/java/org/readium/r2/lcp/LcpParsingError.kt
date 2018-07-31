/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

enum class LcpParsingErrors {
    json,
    date,
    link,
    updated,
    updatedDate,
    encryption,
    signature
}

class LcpParsingError {

    fun errorDescription(error: LcpParsingErrors) = when(error){
        LcpParsingErrors.json -> "The JSON is no representing a valid Status Document."
        LcpParsingErrors.date -> "Invalid ISO8601 dates found."
        LcpParsingErrors.link -> "Invalid Link found in the JSON."
        LcpParsingErrors.encryption -> "Invalid Encryption object."
        LcpParsingErrors.signature -> "Invalid License Document Signature."
        LcpParsingErrors.updated -> "Invalid Updated object."
        LcpParsingErrors.updatedDate -> "Invalid Updated object date."
    }
}