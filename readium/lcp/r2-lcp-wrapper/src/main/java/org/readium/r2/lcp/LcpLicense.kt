package org.readium.r2.lcp

import org.readium.r2.lcp.Model.Documents.LicenseDocument
import org.readium.r2.lcp.Model.Documents.StatusDocument
import org.readium.r2.shared.DrmLicense
import org.readium.r2.shared.removeLastComponent
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class LcpLicense(path: URL, inArchive: Boolean) : DrmLicense {

    var licensePath: URL
    var license: LicenseDocument
    var status: StatusDocument? = null
    var context: DRMContext? = null

    init {
        val data: ByteArray
        if (!inArchive) {
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

    suspend fun fetchStatusDocument() {
        val statusLink = license.link("status") ?: return
        //TODO : finish this method
    }

    // If start is null or before now, or if end is null or before now, throw invalidRights Exception
    override fun areRightsValid() {
        val now = Date()
        if (license.rights.start?.before(now)  ?: license.rights.end?.after(now) ?: true)
            throw Exception(LcpError().errorDescription(LcpErrorCase.invalidRights))
    }

    fun checkStatus() {
        val status = if (status?.status != null) status?.status else throw Exception(LcpError().errorDescription(LcpErrorCase.missingLicenseStatus))
        when (status){
            "returned" -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusReturned))
            "expired" -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusExpired))
            "revoked" -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusRevoked))
            "cancelled" -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusCancelled))
            else -> return
        }
    }

    override fun register() {
        val database = LCPDatabase()
        //val existingLicense = database.licenses.existingLicense(license.id) ?: return
        if (status == null)
            return
        val deviceId = android.os.Build.ID
        val deviceName = android.os.Build.MODEL
        val url = status?.link("register")?.href ?: return
        val registerUrl = URL(url.toString().replace("%7B?id,name%7D", ""))
        //TODO : Http request
    }

    //The return function in swift
    override fun ret(completion: (String) -> Void) {
        if (status == null) {
            completion(LcpError().errorDescription(LcpErrorCase.noStatusDocument))
            return
        }
        val deviceId = android.os.Build.ID
        val deviceName = android.os.Build.MODEL
        val url = status!!.link("return")?.href
        if (url == null){
            completion(LcpError().errorDescription(LcpErrorCase.noStatusDocument))
            return
        }
        val returnUrl = URL(url.toString().replace("%7B?id,name%7D", "") + "?id=$deviceId&name=$deviceName")
        //TODO : Http request
    }

    override fun renew (endDate: Date?, completion: (String) -> Void) {
        if (status == null) {
            completion(LcpError().errorDescription(LcpErrorCase.noStatusDocument))
            return
        }
        val deviceId = android.os.Build.ID
        val deviceName = android.os.Build.MODEL
        val url = status!!.link("return")?.href
        if (url == null){
            completion(LcpError().errorDescription(LcpErrorCase.noStatusDocument))
            return
        }
        val returnUrl = URL(url.toString().replace("%7B?id,name%7D", "") + "?id=$deviceId&name=$deviceName")
        //TODO : Http request
    }

    suspend fun updateStatusDocument() {
        if (status == null)
            return
        val licenseLink = status!!.link("license") ?: return
        val latestUpdate = license.dateOfLastUpdate()
        if (latestUpdate == LCPDatabase().shared.licenses.dateOfLastUpdate(license.id))
            return
        //TODO : Http request
    }

    fun moveLicense(licenseURL: URL, publicationURL: URL) {
        var urlMetaInf = publicationURL.removeLastComponent().toString()
        urlMetaInf = "${urlMetaInf}META-INF"

        //Writes into the zipfile
        val fi = FileInputStream(urlMetaInf)
        val data = ByteArray(2048)
        val origin = BufferedInputStream(fi, 2048)
        val entry = ZipEntry("$publicationURL/META-INF/license.lcpl")
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(publicationURL.toString())))
        out.putNextEntry(entry)
        var count = origin.read(data, 0, 2048)
        while (count != -1){
            out.write(data, 0, count)
            count = origin.read(data, 0, 2048)
        }
        //Closes the zipfile
        origin.close()
    }

    override fun currentStatus(): String {
        return status?.status ?: ""
    }

    override fun lastUpdate(): Date {
        return license.dateOfLastUpdate()
    }

    override fun issued(): Date {
        return license.issued
    }

    override fun provider(): URL {
        return license.provider
    }

    override fun rightsEnd(): Date? {
        return license.rights.end
    }

    override fun potentialRightsEnd(): Date? {
        return license.rights.potentialEnd
    }

    override fun rightsStart(): Date? {
        return license.rights.start
    }

    override fun rightsPrints(): Int? {
        return license.rights.print
    }

    override fun rightsCopies(): Int? {
        return license.rights.copy
    }

}