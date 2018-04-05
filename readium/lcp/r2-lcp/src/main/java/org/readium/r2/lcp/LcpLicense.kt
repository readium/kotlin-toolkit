package org.readium.r2.lcp

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.lcp.Model.Documents.LicenseDocument
import org.readium.r2.lcp.Model.Documents.StatusDocument
import org.readium.r2.shared.DrmLicense
import org.readium.r2.shared.removeLastComponent
import java.io.*
import java.net.URL
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class LcpLicense : DrmLicense {


    var archivePath: URL
    var license: LicenseDocument
    var status: StatusDocument? = null
    var context: DRMContext? = null
    var androidContext:Context

//    constructor(path: URL, context: Context) {
//        androidContext = context
//        archivePath = path
//        val data = path.openStream().readBytes()
//        license = LicenseDocument(data)
//    }

    constructor(path: URL, inArchive: Boolean, context: Context) {

        androidContext = context
        archivePath = path

        val data: ByteArray
        if (!inArchive) {
            data = path.openStream().readBytes()
            license = LicenseDocument(data)
        } else {
            val url  = URL("META-INF/license.lcpl")
            data = getData(url, path)
            license = LicenseDocument(data)
        }

    }

//    init {
//        val data: ByteArray
//        if (!inArchive) {
//            archivePath = path
//
//                val input = path.openStream()
//
//                data = input.readBytes()
//                license = LicenseDocument(data)
//
//
//        } else {
//            archivePath = URL("META-INF/license.lcpl")
//            data = getData(archivePath, path)
//            license = LicenseDocument(data)
//        }
//    }

    // TODO : incomplete
    override fun decipher(data: ByteArray) : ByteArray? {
        if (context == null)
            throw Exception(LcpError().errorDescription(LcpErrorCase.invalidContext))
        return Lcp().decrypt(context!!, data)
    }

    // TODO : incomplete
    fun fetchStatusDocument()  {
        val statusLink = license.link("status") ?: return
        //TODO : finish this method
        val input = java.net.URL(statusLink.href.toString()).openStream()
        status = StatusDocument(input.readBytes())
    }

    // If start is null or before now, or if END is null or before now, throw invalidRights Exception
    override fun areRightsValid() {
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
        val database = LCPDatabase(androidContext)

        database.licenses.updateState(license.id, license.status)
        val date = database.licenses.dateOfLastUpdate(license.id)
        Log.i("VOLLEY date", date.toString())

        if (database.licenses.existingLicense(license.id)) return
        if (status == null) return

        val url = status?.link("register")?.href ?: return
        val registerUrl = URL(url.toString().replace("{?id,name}", ""))

        val requestQueue = Volley.newRequestQueue(androidContext)
        val params : MutableMap<String, String> =  mutableMapOf()
        params.put("id", getDeviceId())
        params.put("name", getDeviceName())

        val r2RegisterRequest =  R2StringRequest(androidContext, Request.Method.POST, registerUrl.toString(), params, Response.Listener { response ->
            Log.i("Volley status: ", R2StringRequest.status())
            if (R2StringRequest.statusCode().equals(400)) {
                Log.i("VOLLEY", LcpError().errorDescription(LcpErrorCase.registrationFailure))
            }
            else if (R2StringRequest.statusCode().equals(200)) {
                val jsonObject = JSONObject(response.toString())
                Log.i("VOLLEY", jsonObject.toString())
                database.licenses.insert(license, jsonObject["status"] as String)
            }
        }, Response.ErrorListener { error ->
            if (error.networkResponse != null && error.networkResponse.data != null) {
                val jsonError = String(error.networkResponse.data)
                val jsonObject = JSONObject(jsonError)
                Log.e("VOLLEY", jsonObject.toString())
            }
        })
        requestQueue.add(r2RegisterRequest)

    }

    // TODO : incomplete
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

    // TODO : incomplete
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

    fun getDeviceId() : String {
        var deviceId = UUID.randomUUID().toString()
        val prefs = androidContext.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        deviceId = prefs.getString("lcp_device_id", deviceId)
        prefs.edit().putString("lcp_device_id", deviceId).apply()
        return deviceId
    }

    fun getDeviceName() : String {
        val deviceName = BluetoothAdapter.getDefaultAdapter()
        return deviceName.name
    }

    fun getStatus() : StatusDocument.Status? {
        return status?.status
    }

    // TODO : incomplete
//    fun fetchPublicationA() {
//        val deferred = deferred<String,Exception>()
//        handlePromise(deferred.promise)
//
//        deferred.resolve("Hello World")
//        deferred.reject(Exception("Hello exceptional World"))
//    }
//
//    fun handlePromise(promise: Promise<String, Exception>) {
//        promise success {
//            msg -> println(msg)
//        }
//        promise fail {
//            e -> println(e)
//        }
//    }
    fun fetchPublication(): URL {


        return task {

            val publicationLink = license.link("publication")
            val requestQueue = Volley.newRequestQueue(androidContext)
            val params : MutableMap<String, String> =  mutableMapOf()

            val title = publicationLink.title

            val r2RegisterRequest = R2StringRequest(androidContext, Request.Method.GET, publicationLink.href.toString(), params, Response.Listener { response ->
                Log.i("Volley status: ", R2StringRequest.status())
                if (R2StringRequest.statusCode().equals(200)) {
                    val jsonObject = JSONObject(response.toString())
                    Log.i("VOLLEY", jsonObject.toString())
                }
            }, Response.ErrorListener { error ->
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    val jsonError = String(error.networkResponse.data)
                    val jsonObject = JSONObject(jsonError)
                    Log.e("VOLLEY", jsonObject.toString())
                }
            })

            requestQueue.add(r2RegisterRequest)

            //TODO doesn belong here
            URL("http://www.test.com")
        }.get()


//
//        return Promise<URL> { fulfill, reject in
//            guard let publicationLink = license.link(withRel: LicenseDocument.Rel.publication) else {
//                reject(LcpError.publicationLinkNotFound)
//                return
//            }
//            let request = URLRequest(url: publicationLink.href)
//            let title = publicationLink.title ?? "publication" //Todo
//            let fileManager = FileManager.default
//            // Document Directory always exists (hence try!).
//            var destinationUrl = try! fileManager.url(for: .documentDirectory,
//                    in: .userDomainMask,
//                appropriateFor: nil,
//                create: true)
//
//                destinationUrl.appendPathComponent("\(title).epub")
//                guard !FileManager.default.fileExists(atPath: destinationUrl.path) else {
//                    fulfill(destinationUrl)
//                    return
//                }
//
//                let task = URLSession.shared.downloadTask(with: request, completionHandler: { tmpLocalUrl, response, error in
//                    if let localUrl = tmpLocalUrl, error == nil {
//                        do {
//                            try FileManager.default.moveItem(at: localUrl, to: destinationUrl)
//                            } catch {
//                                print(error.localizedDescription)
//                                reject(error)
//                            }
//                            fulfill(destinationUrl)
//                        } else if let error = error {
//                        reject(error)
//                    } else {
//                        reject(LcpError.unknown)
//                    }
//                })
//                task.resume()
//            }
    }


    // TODO : incomplete
    fun updateLicenseDocument() {
        if (status == null)
            return
        val licenseLink = status!!.link("license") ?: return
        val latestUpdate = license.dateOfLastUpdate()
//        if (latestUpdate == LCPDatabase().shared.licenses.dateOfLastUpdate(license.id))
//            return
        //TODO : Http request
    }

    // TODO : incomplete
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

//        var destPath = URL(url.removeLastComponent().toString() + "extracted_file.tmp")

//        var destPath = url.deletingLastPathComponent()
//
//        destPath.appendPathComponent("extracted_file.tmp")
//
//        let destUrl = URL.init(fileURLWithPath: destPath.absoluteString)
//        let data: Data
//
//        // Extract file.
//        _ = try archive.extract(entry, to: destUrl)
//        data = try Data.init(contentsOf: destUrl)
//
//        // Remove temporary file.
//        try FileManager.default.removeItem(at: destUrl)

          return archive.getInputStream(entry).readBytes()
    }

    // TODO : incomplete
    fun moveLicense(licenseURL: URL, publicationURL: URL) {
        var urlMetaInf = publicationURL.removeLastComponent().toString()
        urlMetaInf = "${urlMetaInf}/META-INF"

        //Writes into the zipfile
        val fi = FileInputStream(urlMetaInf)
        val data = ByteArray(2048)
        val origin = BufferedInputStream(fi, 2048)
        val entry = ZipEntry("$publicationURL/META-INF/license.lcpl")
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(licenseURL.toString())))
        out.putNextEntry(entry)
        var count = origin.read(data, 0, 2048)
        while (count != -1){
            out.write(data, 0, count)
            count = origin.read(data, 0, 2048)
        }
        //Closes the zipfile
        origin.close()
    }


//    static public func moveLicense(from licenseUrl: URL, to publicationUrl: URL) throws {
//        guard let archive = Archive(url: publicationUrl, accessMode: .update) else  {
//            throw LcpError.archive
//        }
//        // Create local META-INF folder to respect the zip file hierachy.
//        let fileManager = FileManager.default
//        var urlMetaInf = publicationUrl.deletingLastPathComponent()
//
//        urlMetaInf.appendPathComponent("META-INF", isDirectory: true)
//        if !fileManager.fileExists(atPath: urlMetaInf.path) {
//            try fileManager.createDirectory(atPath: urlMetaInf.path, withIntermediateDirectories: false, attributes: nil)
//            }
//        // Move license in the META-INF local folder.
//        try fileManager.moveItem(atPath: licenseUrl.path, toPath: urlMetaInf.path + "/license.lcpl")
//            // Copy META-INF/license.lcpl to archive.
//            try archive.addEntry(with: urlMetaInf.lastPathComponent.appending("/license.lcpl"),
//                relativeTo: urlMetaInf.deletingLastPathComponent())
//                // Delete META-INF/license.lcpl from inbox.
//                try fileManager.removeItem(atPath: urlMetaInf.path)
//                }


    // TODO : incomplete
    override fun currentStatus(): String {
        return status?.status.toString()
    }

    override fun lastUpdate(): Date {
        return DateTime(license.dateOfLastUpdate()).toDate()
    }

    override fun issued(): Date {
        return DateTime(license.issued).toDate()
    }

    override fun provider(): URL {
        return license.provider
    }

    override fun rightsEnd(): Date? {
        return DateTime(license.rights.end).toDate()
    }

    override fun potentialRightsEnd(): Date? {
        return DateTime(license.rights.potentialEnd).toDate()
    }

    override fun rightsStart(): Date? {
        return DateTime(license.rights.start).toDate()
    }

    override fun rightsPrints(): Int? {
        return license.rights.print
    }

    override fun rightsCopies(): Int? {
        return license.rights.copy
    }

}