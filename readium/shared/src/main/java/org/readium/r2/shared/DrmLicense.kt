package org.readium.r2.shared

import java.net.URL
import java.util.*

interface DrmLicense {
    fun decipher(data: ByteArray) : ByteArray?
    fun areRightsValid()
    fun register()
    fun renew(endDate: Date?, completion: (Error) -> Void)
    fun ret(completion: (Error) -> Void)
    fun currentStatus() : String
    fun lastUpdate() : Date
    fun issued() : Date
    fun provider() : URL
    fun rightsEnd() : Date?
    fun potentialRightsEnd() : Date?
    fun rightsStart() : Date?
    fun rightsPrints() : Int?
    fun rightsCopies() : Int?
}