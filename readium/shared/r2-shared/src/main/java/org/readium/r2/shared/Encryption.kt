package org.readium.r2.shared

import java.io.Serializable

//  Contains metadata parsed from Encryption.xml
class Encryption : Serializable{
    //  Identifies the algorithm used to encrypt the resource
    var algorithm: String? = null
    //  Compression method used on the resource
    var compression: String? = null
    //  Original length of the resource in bytes before compression / encryption
    var originalLength: Int? = null
    //  Identifies the encryption profile used to encrypt the resource
    var profile: String? = null
    //  Identifies the encrytpion scheme used to encrypt the resource
    var scheme: String? = null
}
