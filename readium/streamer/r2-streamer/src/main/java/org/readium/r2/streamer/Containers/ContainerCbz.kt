package org.readium.r2.streamer.Containers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.webkit.MimeTypeMap
import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.Drm
import org.readium.r2.streamer.Parser.mimetype
import org.readium.r2.streamer.Parser.mimetypeCBZ
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


class ContainerCbz : CbzContainer, ZipArchiveContainer {

    override var rootFile: RootFile
    override var zipFile: ZipFile
    override var drm: Drm? = null
    override var successCreated: Boolean = false

    constructor(path: String) {

        if (File(path).exists()) {
            successCreated = true
        }
        zipFile = ZipFile(path)
        rootFile = RootFile(path, mimetypeCBZ)
    }

    /**
     * Return a list of all files in a CBZ archive
     *
     * @return fileList: List<String>
     */
    override fun getFilesList(): List<String> {
        val filesList = mutableListOf<String>()
        zipFile.let {
            val listEntries = it.entries()
            listEntries.toList().forEach { filesList.add(it.toString()) }
        }
        return filesList
    }

    override fun data(relativePath: String): ByteArray {
        return super.data(relativePath)
    }


    /**
     * Return the ZipEntry of the path or null if the failed
     * Caution ! path has named like that : " zip_file_path::file_name "
     *     e.g.: assets/ComicBook::ComicPage-001
     *
     * @params path: String, path to file in cbz
     * @return image: ZipEntry?
     */
//    fun getPageEntry(path: String): ZipEntry?{
//        return try{
//            zipFile.getEntry(path.substringAfterLast("::"))
//        } catch (e: Exception){
//            Log.e("Error", "Couldn't find $path in the zipFile (${e.message})")
//            null
//        }
//    }

    /**
     * Return the image of a ZipEntry into a Bitmap
     *               or null if the decoding failed
     *
     * @params entry: ZipEntry
     * @return image: Bitmap?
     */
//    fun getImage(entry: ZipEntry): Bitmap?{
//        return try{
//            val input = zipFile.getInputStream(entry)
//            BitmapFactory.decodeStream(input)
//        } catch (e: Exception){
//            Log.e("Error", "Couldn't read in $entry from zipFile (${e.message})")
//            null
//        }
//    }

    /**
     * Return content of the file in a ByteArray
     *
     * @params relativePath: String
     * @return content: ByteArray
     */
//    override fun data(path: String): ByteArray{
//        return try {
//            val entry = getPageEntry(path)
//            val input = zipFile.getInputStream(entry)
//            input.readBytes()
//        } catch (e: Exception){
//            Log.e("Error", "Couldn't read in $path from zipFile (${e.message})")
//            byteArrayOf()
//        }
//    }

    /**
     * Return the mime type of a fileName
     * It can fail if the file name is not conventional
     * ( like spaces and special characters )
     *
     * @return mimetype: String
     */
//    fun getMimeType(fileName: String): String?{
//        return try {
//            val name = fileName.replace(" ", "").replace("'", "").replace(",", "")
//            val extension = MimeTypeMap.getFileExtensionFromUrl(name)
//            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
//        } catch (e: Exception) {
//            Log.e("Error", "Something went wrong while getMimeType() : ${e.message}")
//            null
//        }
//    }

    /**
     * Return the name of CBZ file
     *
     * @return fileName: String
     */
//    fun getFileName(): String{
//        return zipFile.name
//    }
}