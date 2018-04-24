package org.readium.r2.lcp

import android.os.Environment
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.json.JSONObject
import org.readium.r2.lcp.Model.Documents.LicenseDocument
import org.readium.r2.lcp.Model.Documents.StatusDocument
import org.readium.r2.shared.contentTypeEncoding
import org.readium.r2.shared.promise
import java.io.File
import java.nio.charset.Charset
import java.util.*

class LcpHttpService {

    val rootDir: String = Environment.getExternalStorageDirectory().path + "/r2reader/"
    fun statusDocument(url: String): Promise<StatusDocument, Exception> {
        return Fuel.get(url,null).promise() then {
            val (request, response, result) = it
            StatusDocument(result)
        }
    }
    fun fetchUpdatedLicense(url: String): Promise<LicenseDocument, Exception> {
        return Fuel.get(url,null).promise() then {
            val (request, response, result) = it
            LicenseDocument(result)
        }
    }

    fun publicationUrl(url: String, parameters: List<Pair<String, Any?>>? = null): Promise<String, Exception> {
        val fileName = UUID.randomUUID().toString()
        return Fuel.download(url).destination { response, destination ->
            Log.i("LCP  destination ", rootDir + fileName)
            File(rootDir, fileName)
        }.promise() then {
            val (request, response, result) = it
            Log.i("LCP destination ", rootDir + fileName)
            Log.i("LCP then ", response.url.toString())
            rootDir + fileName
        }
    }

    fun certificateRevocationList(url: String): Promise<String, Exception> {
        return Fuel.get(url,null).promise() then {
            val (request, response, result) = it
            "-----BEGIN X509 CRL-----${Base64.getEncoder().encodeToString(result)}-----END X509 CRL-----";
        }
    }
    
    fun register(registerUrl: String, params: List<Pair<String, Any?>>): Promise<String?, Exception> {
        return Fuel.post(registerUrl.toString(), params).promise() then {
            val (request, response, result) = it
            var status:String? = null
                if (response.statusCode.equals(200)) {
                val jsonObject = JSONObject(String(result, Charset.forName(response.contentTypeEncoding)))
                status = jsonObject["status"] as String
            }
            status
        }
    }

    fun renewLicense(url: String): Promise<String?, Exception> {
        return task { null }
    }

    fun returnLicense(url: String): Promise<String?, Exception> {
        return task { null }
    }

}
