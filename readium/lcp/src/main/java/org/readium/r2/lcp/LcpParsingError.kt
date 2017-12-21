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