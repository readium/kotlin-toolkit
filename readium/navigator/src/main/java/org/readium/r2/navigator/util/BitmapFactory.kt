package org.readium.r2.navigator.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object BitmapFactory : android.graphics.BitmapFactory() {

    suspend fun decodeByteArray(data: ByteArray, options: BitmapFactory.Options? = null): Bitmap? =
        withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        }

    suspend fun decodeByteArrayFitting(data: ByteArray, maxSize: Size): Bitmap? =
        BitmapFactory.Options().run {
            inJustDecodeBounds = true
            decodeByteArray(data, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(maxSize.width, maxSize.height, this.outWidth, this.outHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            decodeByteArray(data, this)
        }

    private fun calculateInSampleSize(reqWidth: Int, reqHeight: Int, width: Int, height: Int): Int =
        when {
            reqHeight <= height && reqWidth <= width -> 1
            reqHeight == 0 -> width / reqWidth
            reqWidth == 0 -> height / reqHeight
            else -> Math.min(height / reqHeight, width / reqWidth)
        }


    /**
     * A one color image.
     * @param width
     * @param height
     * @param color
     * @return A one color image with the given width and height.
     */
    fun createBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
