package org.readium.r2.streamer.Containers

import org.readium.r2.shared.drm.Drm
import java.io.File
import java.util.zip.ZipFile
import org.readium.r2.shared.RootFile
import org.readium.r2.streamer.Parser.mimetype


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
     * @return fileList List<String>
     */
    override fun getFilesList(): List<String> {
        var filesList = mutableListOf<String>()
        zipFile.let {
            val listEntries = it.entries()
            listEntries.toList().forEach { filesList.add(it.toString()) }
        }
        return filesList
    }

}