package org.readium.r2.lcp

import org.readium.r2.lcp.DRMContext
import org.readium.r2.lcp.Lcp
import org.readium.r2.lcp.Model.Documents.LicenseDocument
import org.readium.r2.lcp.Model.Documents.StatusDocument
import org.readium.r2.shared.DrmLicense
import java.net.URL
import java.util.*
import java.util.zip.ZipFile

class LcpLicense(path: URL, inArchive: Boolean) : DrmLicense {

    var licensePath: URL
    var license: LicenseDocument
    var status: StatusDocument? = null
    var context: DRMContext? = null

    init {
        val data: ByteArray
        if (inArchive == false) {
            licensePath = path
            data = path.readBytes()
        } else {
            licensePath = URL("META-INF/license.lcpl")
            data = getData(licensePath, path)
        }
        license = LicenseDocument(data)
    }

    private fun getData(file: URL, url: URL) : ByteArray {
        val archive = try {
            ZipFile(url.toString())
        } catch (e: Exception){
            throw Exception(LcpError().errorDescription(LcpErrorCase.archive))
        }
        val entry = try {
            archive.getEntry(file.toString())
        } catch (e: Exception){
            throw Exception(LcpError().errorDescription(LcpErrorCase.fileNotInArchive))
        }
        return archive.getInputStream(entry).readBytes()
    }

    override fun decipher(data: ByteArray) : ByteArray? {
        if (context == null)
            throw Exception(LcpError().errorDescription(LcpErrorCase.invalidContext))
        return Lcp().decrypt(context!!, data)
    }

    override fun ret(completion: (Error) -> Void) {

    }

    override fun currentStatus(): String {
        return ""
    }

    override fun lastUpdate(): Date {
        return Date()
    }

    override fun issued(): Date {
        return Date()
    }

    override fun provider(): URL {
        return URL("")
    }

    override fun rightsEnd(): Date? {
        return Date()
    }

    override fun potentialRightsEnd(): Date? {
        return Date()

    }

    override fun rightsStart(): Date? {
        return Date()

    }

    override fun rightsPrints(): Int? {
        return 0
    }

    override fun rightsCopies(): Int? {
        return 0
    }

    override fun renew(endDate: Date?, completion: (Error) -> Void) {

    }

    override fun areRightsValid() {

    }

    override fun register() {

    }


}