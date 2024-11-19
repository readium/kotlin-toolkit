package org.readium.r2.testapp.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder
import org.readium.r2.testapp.utils.tryOrLog

class CoverStorage(
    private val appStorageDir: File,
    private val httpClient: HttpClient,
) {

    suspend fun storeCover(publication: Publication, overrideUrl: AbsoluteUrl?): Try<File, Exception> {
        val coverBitmap: Bitmap? = overrideUrl?.fetchBitmap()
            ?: publication.cover()
        return try {
            Try.success(storeCover(coverBitmap))
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    private suspend fun AbsoluteUrl.fetchBitmap(): Bitmap? =
        tryOrLog {
            when {
                isFile -> toFile()?.toBitmap()
                isHttp -> httpClient.fetchBitmap(HttpRequest(this)).getOrNull()
                else -> null
            }
        }

    private suspend fun File.toBitmap(): Bitmap? =
        withContext(Dispatchers.IO) {
            tryOrLog {
                BitmapFactory.decodeFile(path)
            }
        }

    private suspend fun HttpClient.fetchBitmap(request: HttpRequest): Try<Bitmap, HttpError> =
        fetchWithDecoder(request) { response ->
            BitmapFactory.decodeByteArray(response.body, 0, response.body.size)
        }

    private suspend fun storeCover(cover: Bitmap?): File =
        withContext(Dispatchers.IO) {
            val coverImageFile = File(coverDir(), "${UUID.randomUUID()}.png")
            val resized = cover?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
            coverImageFile
        }

    private fun coverDir(): File =
        File(appStorageDir, "covers/")
            .apply { if (!exists()) mkdirs() }
}
