package org.readium.r2.lcp

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.readium.downloads.DownloadManager
import org.readium.downloads.DownloadManagerProvider
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import timber.log.Timber

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "readium-lcp-licenses")

private val licensesKey: Preferences.Key<String> = stringPreferencesKey("licenses")

public class LcpPublicationRetriever(
    private val context: Context,
    private val listener: Listener,
    private val downloadManagerProvider: DownloadManagerProvider,
    private val mediaTypeRetriever: MediaTypeRetriever
) {

    @JvmInline
    public value class RequestId(public val value: Long)

    public interface Listener {

        public fun onAcquisitionCompleted(
            requestId: RequestId,
            acquiredPublication: LcpService.AcquiredPublication
        )

        public fun onAcquisitionProgressed(
            requestId: RequestId,
            downloaded: Long,
            total: Long
        )

        public fun onAcquisitionFailed(
            requestId: RequestId,
            error: LcpException
        )
    }

    private inner class DownloadListener : DownloadManager.Listener {

        private val coroutineScope: CoroutineScope =
            MainScope()

        override fun onDownloadCompleted(
            requestId: DownloadManager.RequestId,
            destUri: Uri
        ) {
            coroutineScope.launch {
                val acquisition = onDownloadCompleted(
                    requestId.value,
                    Url(destUri.toString())!!
                )
                listener.onAcquisitionCompleted(RequestId(requestId.value), acquisition)
            }
        }

        override fun onDownloadProgressed(
            requestId: DownloadManager.RequestId,
            downloaded: Long,
            total: Long
        ) {
            listener.onAcquisitionProgressed(
                RequestId(requestId.value),
                downloaded,
                total
            )
        }

        override fun onDownloadFailed(
            requestId: DownloadManager.RequestId,
            error: DownloadManager.Error
        ) {
            listener.onAcquisitionFailed(
                RequestId(requestId.value),
                LcpException.Network(Exception(error.message))
            )
        }
    }

    private val downloadManager: DownloadManager =
        downloadManagerProvider.createDownloadManager(DownloadListener())

    private val licenses: Flow<Map<Long, String>> =
        context.dataStore.data
            .map { data -> data[licensesKey]!! }
            .map { json -> json.toLicenseList() }

    public suspend fun retrieve(
        license: ByteArray,
        downloadTitle: String,
        downloadDescription: String
    ): Try<RequestId, LcpException> {
        return try {
            val licenseDocument = LicenseDocument(license)
            Timber.d("license ${licenseDocument.json}")
            fetchPublication(
                licenseDocument,
                downloadTitle,
                downloadDescription
            ).let { Try.success(it) }
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    public suspend fun retrieve(
        license: File,
        downloadTitle: String,
        downloadDescription: String
    ): Try<RequestId, LcpException> {
        return try {
            retrieve(license.readBytes(), downloadTitle, downloadDescription)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    private suspend fun fetchPublication(
        license: LicenseDocument,
        downloadTitle: String,
        downloadDescription: String
    ): RequestId {
        val link = license.link(LicenseDocument.Rel.publication)
        val url = link?.url
            ?: throw LcpException.Parsing.Url(rel = LicenseDocument.Rel.publication.value)

        val requestId = downloadManager.submit(
            DownloadManager.Request(
                Url(url.toString())!!,
                emptyMap(),
                downloadTitle,
                downloadDescription
            )
        )

        persistLicense(requestId.value, license.json.toString())

        return RequestId(requestId.value)
    }

    private suspend fun onDownloadCompleted(id: Long, dest: Url): LcpService.AcquiredPublication {
        val license = LicenseDocument(licenses.first()[id]!!.toByteArray())
        removeLicense(id)

        val link = license.link(LicenseDocument.Rel.publication)!!

        val mediaType = mediaTypeRetriever.retrieve(mediaType = link.type)
            ?: MediaType.EPUB

        val file = File(dest.path)

        // Saves the License Document into the downloaded publication
        val container = createLicenseContainer(file, mediaType)
        container.write(license)

        return LcpService.AcquiredPublication(
            localFile = file,
            suggestedFilename = "${license.id}.${mediaType.fileExtension}",
            mediaType = mediaType,
            licenseDocument = license
        )
    }

    private suspend fun persistLicense(id: Long, license: String) {
        context.dataStore.edit { data ->
            val newEntry = id to licenseToJson(id, license).toString()
            val licenses = licenses.first() + newEntry
            data[licensesKey] = licenses.toJson()
        }
    }

    private suspend fun removeLicense(id: Long) {
        context.dataStore.edit { data ->
            val uris = licenses.first() - id
            data[licensesKey] = uris.toJson()
        }
    }

    private fun licenseToJson(id: Long, license: String): JSONObject =
        JSONObject()
            .put("id", id)
            .put("license", license)

    private fun jsonToLicense(jsonObject: JSONObject): Pair<Long, String> =
        jsonObject.getLong("id") to jsonObject.getString("license")

    private fun Map<Long, String>.toJson(): String {
        val strings = map { licenseToJson(it.key, it.value) }
        val array = JSONArray(strings)
        return array.toString()
    }

    private fun String.toLicenseList(): Map<Long, String> {
        val array = JSONArray(this)
        val objects = (0 until array.length()).map { array.getJSONObject(it) }
        return objects.associate { jsonToLicense(it) }
    }
}
