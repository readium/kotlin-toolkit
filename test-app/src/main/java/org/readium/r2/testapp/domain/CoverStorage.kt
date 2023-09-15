package org.readium.r2.testapp.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.Try

class CoverStorage(
    appStorageDir: File
) {

    private val coverDir: File =
        File(appStorageDir, "covers/")
            .apply { if (!exists()) mkdirs() }

    suspend fun storeCover(publication: Publication, overrideUrl: String?): Try<File, Exception> {
        val coverBitmap: Bitmap? = overrideUrl
            ?.let { getBitmapFromURL(it) }
            ?: publication.cover()
        return try {
            Try.success(storeCover(coverBitmap))
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    private suspend fun storeCover(cover: Bitmap?): File =
        withContext(Dispatchers.IO) {
            val coverImageFile = File(coverDir, "${UUID.randomUUID()}.png")
            val resized = cover?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
            coverImageFile
        }

    private suspend fun getBitmapFromURL(src: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(src)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
}
