package org.readium.r2.streamer.Containers

import android.util.Log
import android.webkit.MimeTypeMap
import org.readium.r2.shared.drm.Drm
import java.io.File
import java.util.zip.ZipFile
import org.readium.r2.shared.RootFile
import org.readium.r2.streamer.Parser.mimetype
import org.zeroturnaround.zip.ZipUtil
import java.util.zip.ZipEntry


class ContainerCbz : CbzContainer, ZipArchiveContainer {

    override var rootFile: RootFile
    override var zipFile: ZipFile
    override var drm: Drm? = null
    override var successCreated: Boolean = false    // Used to check if construction is a success

    constructor(path: String) {

        if (File(path).exists()) {
            successCreated = true
        }
        zipFile = ZipFile(path)
        rootFile = RootFile(rootPath = path, mimetype = mimetype)
    }

    /**
     * Return a list of all files in a CBZ archive
     *
     * @return fileList: List<String>
     */
    override fun getFilesList(): List<String> {
        var filesList = mutableListOf<String>()
        zipFile.let {
            val listEntries = it.entries()
            listEntries.toList().forEach { filesList.add(it.toString()) }
        }
        return filesList
    }

    /**
     * Return the content of a ZipEntry into a ByteArray
     *
     * @params entry: ZipEntry
     * @return content: ByteArray
     */
    fun getContent(entry: ZipEntry): ByteArray{
        var content = byteArrayOf()
        try{
            content = ZipUtil.unpackEntry(zipFile, entry.name)
        } catch (e: Exception){
            Log.e("Error", "Couldn't extract $entry from zipFile (${e.message})")
        }
        return content
    }

    /**
     * Determines a Title from the name of the CBZ
     *
     * @return title: String
     */
    fun getTitle(): String{
        var title = ""
        try {
            title = zipFile.name.removeSuffix(".cbz")
        } catch (e: Exception){
            Log.e("Error", "Couldn't catch zipFile name (${e.message}")
        }
        return title
    }

    fun getMimetype(nameOfFile: String): String{
        val extension = MimeTypeMap.getFileExtensionFromUrl(nameOfFile)
        val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimetype
    }
}