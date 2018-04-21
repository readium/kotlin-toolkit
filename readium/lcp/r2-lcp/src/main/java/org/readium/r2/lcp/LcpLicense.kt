package org.readium.r2.lcp

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.joda.time.DateTime
import org.readium.lcp.sdk.DRMContext
import org.readium.lcp.sdk.Lcp
import org.readium.r2.lcp.Model.Documents.LicenseDocument
import org.readium.r2.lcp.Model.Documents.StatusDocument
import org.readium.r2.shared.DrmLicense
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.net.URL
import java.util.*
import java.util.zip.ZipFile

const val lcplFilePath = "META-INF/license.lcpl"

class LcpLicense : DrmLicense {

    private val TAG = this::class.java.simpleName

    var archivePath: URL
    var license: LicenseDocument
    var status: StatusDocument? = null
    var context: DRMContext? = null
    var androidContext:Context
    val lcpHttpService: LcpHttpService = LcpHttpService()
    val database:LcpDatabase

    constructor(path: URL, inArchive: Boolean, context: Context) {

        androidContext = context
        archivePath = path

        database = LcpDatabase(androidContext)

        val data: ByteArray
        if (!inArchive) {
            data = path.openStream().readBytes()
            license = LicenseDocument(data)
        } else {
            val url  = lcplFilePath
            data = getData(url, path)
            license = LicenseDocument(data)
        }

    }

    override fun decipher(data: ByteArray) : ByteArray? {
        if (context == null)
            throw Exception(LcpError().errorDescription(LcpErrorCase.invalidContext))
        return Lcp().decrypt(context!!, data)
    }

    fun fetchStatusDocument() : Promise<Unit?, Exception> {
        return task{
            Log.i(TAG,"LCP fetchStatusDocument")
            val statusLink = license.link("status")
            statusLink?.let {
                val document = lcpHttpService.statusDocument(it.href.toString()).get()
                status = document
            }
        }
    }

    // If start is null or before now, or if END is null or before now, throw invalidRights Exception
    override fun areRightsValid() {
        Log.i(TAG,"LCP areRightsValid")
        val now = Date()
        license.rights.start.let {
            if (it != null && DateTime(it).toDate().before(now)) {
                throw Exception(LcpError().errorDescription(LcpErrorCase.invalidRights))
            }
        }
        license.rights.end.let {
            if (it != null && DateTime(it).toDate().after(now)) {
                throw Exception(LcpError().errorDescription(LcpErrorCase.invalidRights))
            }
        }
    }

    fun checkStatus() {
        Log.i(TAG,"LCP checkStatus")
        val status = if (status?.status != null) status?.status else throw Exception(LcpError().errorDescription(LcpErrorCase.missingLicenseStatus))
        when (status){
            StatusDocument.Status.returned -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusReturned))
            StatusDocument.Status.expired -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusExpired))
            StatusDocument.Status.revoked -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusRevoked))
            StatusDocument.Status.cancelled -> throw Exception(LcpError().errorDescription(LcpErrorCase.licenseStatusCancelled))
            else -> return
        }
    }

    override fun register() {
        Log.i(TAG,"LCP register")

        val date = database.licenses.dateOfLastUpdate(license.id)
        Log.i(TAG, "LCP dateOfLastUpdate ${date}")

        if (database.licenses.existingLicense(license.id)) return
        if (status == null) return

        val url = status?.link("register")?.href ?: return
        val registerUrl = URL(url.toString().replace("{?id,name}", ""))

        val params = listOf(
                "id" to getDeviceId(),
                "name" to getDeviceName())
        try {
            lcpHttpService.register(registerUrl.toString(), params).get()?.let {
                database.licenses.insert(license, it)
            }
        }catch (e:Exception) {
            Log.e(TAG, "LCP register ${e.message}")
        }

    }

    // TODO : incomplete
    override fun ret(completion: (String) -> Void) {
        Log.i(TAG,"LCP return")

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
        lcpHttpService.returnLicense(returnUrl.toString()).get()?.let {
            //TODO
        }
    }

    // TODO : incomplete
    override fun renew (endDate: Date?, completion: (String) -> Void) {
        Log.i(TAG,"LCP renew")
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

        lcpHttpService.renewLicense(returnUrl.toString()).get()?.let {
            //TODO
        }
    }

    fun getDeviceId() : String {
        Log.i(TAG,"LCP getDeviceId")
        var deviceId = UUID.randomUUID().toString()
        val prefs = androidContext.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        deviceId = prefs.getString("lcp_device_id", deviceId)
        prefs.edit().putString("lcp_device_id", deviceId).apply()
        return deviceId
    }

    fun getDeviceName() : String {
        Log.i(TAG,"LCP getDeviceName")
        val deviceName = BluetoothAdapter.getDefaultAdapter()
        return deviceName.name
    }

    fun getStatus() : StatusDocument.Status? {
        Log.i(TAG,"LCP getStatus")
        return status?.status
    }

    fun fetchPublication(): String? {
        Log.i(TAG,"LCP fetchPublication")
        val publicationLink = license.link("publication")
        publicationLink?.let {
            return lcpHttpService.publicationUrl(publicationLink.href.toString()).get()
        }
        return null
    }


    // TODO : double check his.
    fun updateLicenseDocument() : Promise<Unit, Exception> {
        return task {
            Log.i(TAG,"LCP updateLicenseDocument")
            if (status != null) {
                val licenseLink = status!!.link("license")
                val latestUpdate = license.dateOfLastUpdate()

                val lastUpdate = database.licenses.dateOfLastUpdate(license.id)
                lastUpdate?.let {
                    if (lastUpdate > latestUpdate)
                    {
                        return@let
                    }
                }

                license = lcpHttpService.fetchUpdatedLicense(licenseLink!!.href.toString()).get()
                Log.i(TAG, "LCP  ${license.json}")

                moveLicense(archivePath, licenseLink.href.toString())

                database.licenses.insert(license, status!!.status.toString())
            }
        }
    }

    private fun getData(file: String, url: URL) : ByteArray {
        Log.i(TAG,"LCP getData")
        val archive = try {
            ZipFile(url.path)
        } catch (e: Exception){
            throw Exception(LcpError().errorDescription(LcpErrorCase.archive))
        }
        val entry = try {
            archive.getEntry(file)
        } catch (e: Exception){
            throw Exception(LcpError().errorDescription(LcpErrorCase.fileNotInArchive))
        }
        return archive.getInputStream(entry).readBytes()
    }

    fun moveLicense(licenseURL: URL, publicationURL: String) {
        Log.i(TAG,"LCP moveLicense")
        task {
            val source = File(publicationURL)
            val tmpZip = File(publicationURL+".tmp")
            tmpZip.delete()
            source.copyTo(tmpZip)
            source.delete()
            ZipUtil.addEntry(tmpZip, lcplFilePath,  licenseURL.openStream().readBytes(),  source);

            tmpZip
        } then {
            it.delete()
        }
    }

    override fun currentStatus(): String {
        Log.i(TAG,"LCP currentStatus")
        return status?.status.toString()
    }

    override fun lastUpdate(): Date {
        Log.i(TAG,"LCP lastUpdate")
        return DateTime(license.dateOfLastUpdate()).toDate()
    }

    override fun issued(): Date {
        Log.i(TAG,"LCP issued")
        return DateTime(license.issued).toDate()
    }

    override fun provider(): URL {
        Log.i(TAG,"LCP provider")
        return license.provider
    }

    override fun rightsEnd(): Date? {
        Log.i(TAG,"LCP rightsEnd")
        return DateTime(license.rights.end).toDate()
    }

    override fun potentialRightsEnd(): Date? {
        Log.i(TAG,"LCP potentialRightsEnd")
        return DateTime(license.rights.potentialEnd).toDate()
    }

    override fun rightsStart(): Date? {
        Log.i(TAG,"LCP rightsStart")
        return DateTime(license.rights.start).toDate()
    }

    override fun rightsPrints(): Int? {
        Log.i(TAG,"LCP rightsPrints")
        return license.rights.print
    }

    override fun rightsCopies(): Int? {
        Log.i(TAG,"LCP rightsCopies")
        return license.rights.copy
    }

}
