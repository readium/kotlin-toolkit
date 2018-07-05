package org.readium.r2.streamer.Parser

import android.util.Log
import org.readium.r2.shared.Link
import org.readium.r2.shared.PUBLICATION_TYPE
import org.readium.r2.shared.Publication
import java.io.File
import org.readium.r2.streamer.Containers.ContainerCbz

// Some constants useful to parse an Cbz document
const val mimetypeCBZ = "application/vnd.comicbook+zip"
const val mimetypeCBR = "application/x-cbr"

const val mimetypeJPEG = "image/jpeg"
const val mimetypePNG = "image/png"

/**
 *      CbzParser : Handle any CBZ file. Openning, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */

class CbzParser : PublicationParser {

    /**
     * Check if path exist, generate a container for CBZ file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String) : ContainerCbz {
        val container: ContainerCbz?

        if (!File(path).exists())
            throw Exception("Missing File")
        container = ContainerCbz(path)
        if (!container.successCreated)
            throw Exception("Missing File")
        return container
    }

    //TODO Comment that code
    /**
     *
     */
    override fun parse(fileAtPath: String) : PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Log.e("Error", "Could not generate container", e)
            return null
        }
        val listFiles = try {
            container.getFilesList()
        } catch (e: Exception) {
            Log.e("Error", "Missing File : META-INF/container.xml", e)
            return null
        }
/*
        Testing the container.getFilesList()
            it shall return the List<String> of file of a cbz file
*/
        println("###########################################")
        println("#  #  #  #  #  #  #  #  #  #  #  #  #  #  #")
        println("---      List of files ${fileAtPath}   ----")
        println(listFiles)
        println("-------------------------------------------")
        println("#  #  #  #  #  #  #  #  #  #  #  #  #  #  #")
        println("###########################################")

        val publication = Publication()

        listFiles.forEach {
            val link = Link()

            link.typeLink = container.getMimeType(it)
            link.href = container.rootFile.rootFilePath + "::" + it

            if(container.getMimeType(it) == mimetypeJPEG ||
                    container.getMimeType(it) == mimetypePNG) {
                publication.pageList.add(link)
            } else {
                publication.resources.add(link)         //List of eventual extra files ( .nfo, ect .. )
            }
        }
        publication.metadata.title = container.getTitle()
        publication.type = PUBLICATION_TYPE.CBZ
        return PubBox(publication, container)
    }

    /**
     * List all CBZ files in a particular path
     *
     * @return listCBZ: List<String>
     */
    fun getCbzFiles(path: String): List<String>{
        var listCBZ = emptyList<String>()
        try {
            val file = File(path)
            listCBZ = file.list().filter { it.endsWith(".cbz") }
        } catch (e: Exception){
            Log.e("Error", "Can't open the file (${e.message})")
        }
        return listCBZ
    }

}