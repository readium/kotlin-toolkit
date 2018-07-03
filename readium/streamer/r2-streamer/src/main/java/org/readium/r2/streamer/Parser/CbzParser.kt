package org.readium.r2.streamer.Parser

import android.util.Log
import java.io.File
import org.readium.r2.streamer.Containers.*

// Some constants useful to parse an Cbz document
const val mimetypeCBZ = "application/x-cbr"

/**
 *      CbzParser : Handle any CBZ file. Openning, listing files
 *                  get name of the resource, creating the Publication for rendering
 */

class CbzParser : PublicationParser {

    /**
     * Check if path exist, generate a container for CBZ file
     *              then check if creation was a success
     */
    private fun generateContainerFrom(path: String) : CbzContainer {
        val container: CbzContainer?

        if (!File(path).exists())
            throw Exception("Missing File")
        container = ContainerCbz(path)
        if (!container.successCreated)
            throw Exception("Missing File")
        return container
    }


    override fun parse(fileAtPath: String) : PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Log.e("Error", "Could not generate container", e)
            return null
        }
        val data = try {
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
        println(data)
        println("-------------------------------------------")
        println("#  #  #  #  #  #  #  #  #  #  #  #  #  #  #")
        println("###########################################")
        return null
    }

    /**
     * List all CBZ files of a particular path
     */
    fun getCbzFiles(path: String): List<String>{
        var listCBZ = emptyList<String>()
        try {
            val file = File(path)
            listCBZ = file.list().filter { it.endsWith(".cbz") }
        } catch (e: Exception){
            Log.e("Error", "Can't open the file")
        }
        return listCBZ
    }

}