package org.readium.r2.streamer.Fetcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.Publication
import org.readium.r2.shared.RenditionLayout
import org.readium.r2.shared.removeLastComponent
import org.readium.r2.streamer.Containers.Container
import java.io.File
import java.io.InputStream
import java.net.URL

interface ContentFilters{
    var fontDecoder: FontDecoder
    var drmDecoder: DrmDecoder

    fun apply(input: InputStream, publication: Publication, container:Container, path: String) : InputStream {
        return input
    }

    fun apply(input: ByteArray, publication: Publication, container:Container, path: String) : ByteArray {
        return input
    }
}

class ContentFiltersEpub(val context: Context) : ContentFilters {

    override var fontDecoder = FontDecoder()
    override var drmDecoder = DrmDecoder()

    override fun apply(input: InputStream, publication: Publication, container:Container, path: String): InputStream {
        publication.linkWithHref(path)?.let {resourceLink ->

            var decodedInputStream = drmDecoder.decoding(input, resourceLink, container.drm)
            decodedInputStream = fontDecoder.decoding(decodedInputStream, publication, path)
            val baseUrl = publication.baseUrl()?.removeLastComponent()
            if ((resourceLink.typeLink == "application/xhtml+xml" || resourceLink.typeLink == "text/html")
                    && baseUrl != null){
                if (publication.metadata.rendition.layout == RenditionLayout.reflowable && resourceLink.properties.layout == null
                        || resourceLink.properties.layout == "reflowable") {
                    decodedInputStream = injectReflowableHtml(decodedInputStream, baseUrl)
                } else {
                    decodedInputStream = injectFixedLayoutHtml(decodedInputStream, baseUrl)
                }
            }

            return decodedInputStream
        }?: run {
            return input
        }

    }

    override fun apply(input: ByteArray, publication: Publication, container:Container, path: String): ByteArray {
        publication.linkWithHref(path)?.let {resourceLink ->
            val inputStream = input.inputStream()
            var decodedInputStream = drmDecoder.decoding(inputStream, resourceLink, container.drm)
            decodedInputStream = fontDecoder.decoding(decodedInputStream, publication, path)
            val baseUrl = publication.baseUrl()?.removeLastComponent()
            if ((resourceLink.typeLink == "application/xhtml+xml" || resourceLink.typeLink == "text/html")
                    && baseUrl != null){
                decodedInputStream =
                        if (publication.metadata.rendition.layout == RenditionLayout.reflowable && (resourceLink.properties.layout == null
                                        || resourceLink.properties.layout == "reflowable")) {
                            injectReflowableHtml(decodedInputStream, baseUrl)
                        } else {
                            injectFixedLayoutHtml(decodedInputStream, baseUrl)
                        }
            }
            return decodedInputStream.readBytes()
        }?: run {
            return input
        }
    }

    private fun injectReflowableHtml(stream: InputStream, baseUrl: URL) : InputStream {
        val data = stream.readBytes()
        var resourceHtml = String(data)
        // Inject links to css and js files
        var beginHeadIndex = resourceHtml.indexOf("<head>", 0, false) + 6
        var endHeadIndex = resourceHtml.indexOf("</head>", 0, false)
        if (endHeadIndex == -1)
            return stream
        val endIncludes = mutableListOf<String>()
        val beginIncludes = mutableListOf<String>()
        beginIncludes.add("<meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0;\"/>")
        beginIncludes.add(getHtmlLink("/styles/before.css"))
        beginIncludes.add(getHtmlLink("/styles/default.css"))
        beginIncludes.add(getHtmlLink("/styles/transition.css"))
        endIncludes.add(getHtmlLink("/styles/after.css"))
        endIncludes.add(getHtmlScript("/scripts/touchHandling.js"))
        endIncludes.add(getHtmlScript("/scripts/utils.js"))
        for (element in beginIncludes){
            resourceHtml = StringBuilder(resourceHtml).insert(beginHeadIndex, element).toString()
            beginHeadIndex += element.length
            endHeadIndex += element.length
        }
        for (element in endIncludes){
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
            endHeadIndex += element.length
        }
        resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, "<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style> ").toString()
        // Inject userProperties
        val beginHtmlIndex = resourceHtml.indexOf("<html", 0, false) + 5
        getProperties()?.let {
            resourceHtml = StringBuilder(resourceHtml).insert(beginHtmlIndex, buildStringProperties(it)).toString()
        }
        return resourceHtml.toByteArray().inputStream()
    }

    private fun injectFixedLayoutHtml(stream: InputStream, baseUrl: URL) : InputStream {
        val data = stream.readBytes()
        var resourceHtml = String(data) //UTF-8
        val endHeadIndex = resourceHtml.indexOf("</head>", 0, false)
        if (endHeadIndex == -1)
            return stream
        val includes = mutableListOf<String>()
        val url = baseUrl.toString()
        includes.add("<meta name=\"viewport\" content=\"width=1024; height=768; left=50%; top=50%; bottom=auto; right=auto; transform=translate(-50%, -50%);\"/>\n")
        includes.add(getHtmlScript(url + "scripts/touchHandling.js"))
        includes.add(getHtmlScript(url + "scripts/utils.js"))
        for (element in includes){
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
        }
        return resourceHtml.toByteArray().inputStream()
    }

    fun getHtmlLink(ressourceName: String) : String {
        val prefix = "<link rel=\"stylesheet\" type=\"text/css\" href=\""
        val suffix = "\"/>\n"
        return prefix + ressourceName + suffix
    }

    fun getHtmlScript(ressourceName: String) : String {
        val prefix = "<script type=\"text/javascript\" src=\""
        val suffix = "\"></script>\n"

        return prefix + ressourceName + suffix
    }

    private fun getProperties(): MutableList<Pair<String, String>>? {
        // Storing the content of the file in a String
        val propertiesContent: String
        val dir = File(context.getExternalFilesDir(null).path + "/styles/")
        val file = File(dir, "UserProperties.json")
        if (file.isFile() && file.canRead()) {
            propertiesContent = file.readText(Charsets.UTF_8)
        } else {
            return null
        }

        // Parsing of the String into a JSONArray of JSONObject with each "name" and "value" of the css properties
        val propertiesArray = JSONArray(propertiesContent)

        // Making that JSONArray a MutableMap<String, String> to make easier the access of data
        val properties: MutableList<Pair<String, String>> = arrayListOf()
        for (i in 0..(propertiesArray.length() - 1) ) {
            val value = propertiesArray.getJSONObject(i)
            properties.add(Pair(value.getString("name"), value.getString("value")))
            println("##~~~~~~~~")
            println("##~~~~~~~~    properties[${i}]      :     ${properties[i]}")
            println("##~~~~~~~~    properties[${i}].name :     ${properties[i].first}")
            println("##~~~~~~~~    properties[${i}].value:     ${properties[i].second}")
        }
        return properties
    }

    private fun buildStringProperties(list: MutableList<Pair<String, String>>) : String {
        var string = ""
        for (property in list) {
            string = string + " " + property.first + ": " + property.second + ";"
        }
        return string
    }


}

class ContentFiltersCbz : ContentFilters {
    override var fontDecoder: FontDecoder = FontDecoder()
    override var drmDecoder: DrmDecoder = DrmDecoder()
}

