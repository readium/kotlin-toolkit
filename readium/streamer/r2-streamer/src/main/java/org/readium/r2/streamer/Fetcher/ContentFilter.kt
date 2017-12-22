package org.readium.r2.streamer.Fetcher

import org.readium.r2.shared.Publication
import org.readium.r2.shared.RenditionLayout
import org.readium.r2.shared.removeLastComponent
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL

interface ContentFilters{
    var fontDecoder: FontDecoder

    fun apply(input: InputStream, publication: Publication, path: String) : InputStream {
        return input
    }

    fun apply(input: ByteArray, publication: Publication, path: String) : ByteArray {
        return input
    }
}

class ContentFiltersEpub: ContentFilters {

    override var fontDecoder = FontDecoder()

    override fun apply(input: InputStream, publication: Publication, path: String): InputStream {
        var decodedInputStream = fontDecoder.decoding(input, publication, path)
        val link = publication.linkWithHref(path)
        val baseUrl = publication.baseUrl()?.removeLastComponent()
        if ((link?.typeLink == "application/xhtml+xml" || link?.typeLink == "text/html")
                && baseUrl != null){
            if (publication.metadata.rendition.layout == RenditionLayout.reflowable && link.properties?.layout == null
                    || link.properties?.layout == "reflowable"){
                decodedInputStream = injectReflowableHtml(decodedInputStream, baseUrl) as ByteArrayInputStream
            } else {
                decodedInputStream = injectFixedLayohtHtml(decodedInputStream, baseUrl) as ByteArrayInputStream
            }
        }
        return decodedInputStream
    }

    override fun apply(input: ByteArray, publication: Publication, path: String): ByteArray {
        val inputStream = ByteArrayInputStream(input)
        var decodedInputStream = fontDecoder.decoding(inputStream, publication, path)
        val link = publication.linkWithHref(path)
        val baseUrl = publication.baseUrl()?.removeLastComponent()
        if ((link?.typeLink == "application/xhtml+xml" || link?.typeLink == "text/html")
                && baseUrl != null){
            decodedInputStream =
                    if (publication.metadata.rendition.layout == RenditionLayout.reflowable && (link.properties?.layout == null
                        || link.properties?.layout == "reflowable"))
                    {
                        injectReflowableHtml(decodedInputStream, baseUrl) as ByteArrayInputStream
                    } else {
                        injectFixedLayohtHtml(decodedInputStream, baseUrl) as ByteArrayInputStream
                    }
        }
        return decodedInputStream.readBytes()
    }

    private fun injectReflowableHtml(stream: InputStream, baseUrl: URL) : InputStream {
        val data = stream.readBytes()
        var resourceHtml = String(data)
        var beginHeadIndex = resourceHtml.indexOf("<head>", 0, false) + 6
        var endHeadIndex = resourceHtml.indexOf("</head>", 0, false)
        if (endHeadIndex == -1)
            return stream
        val endIncludes = mutableListOf<String>()
        val beginIncludes = mutableListOf<String>()
        beginIncludes.add("<meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0;\"/>")
        beginIncludes.add(getHtmlLink("/styles/before.css"))
        beginIncludes.add(getHtmlLink("/styles/default.css"))
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
        return ByteArrayInputStream(resourceHtml.toByteArray())
    }

    private fun injectFixedLayohtHtml(stream: InputStream, baseUrl: URL) : InputStream {
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
        return ByteArrayInputStream(resourceHtml.toByteArray())
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

}

class ContentFiltersCbz : ContentFilters {
    override var fontDecoder: FontDecoder = FontDecoder()
}

