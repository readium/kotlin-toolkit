/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumannn, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.layout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.streamer.server.Resources
import java.io.File

class HtmlInjector(
    val publication: Publication,
    val userPropertiesPath: String?,
    val customResources: Resources? = null
) {

    fun transform(resource: Resource): Resource =
        if (resource.link.mediaType?.isHtml == true)
            inject(resource)
        else
            resource

    private fun inject(resource: Resource): Resource {
        val link = resource.link
        val result = resource.readAsString(link.mediaType?.charset)
        if (result.isFailure)
            return FailureResource(link, result.failure)

        val trimmedText = result.success.trim()
        val injector = if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE && link.properties.layout == null
            || link.properties.layout == EpubLayout.REFLOWABLE)
            HtmlInjector::injectReflowableHtml
        else
            HtmlInjector::injectFixedLayoutHtml
        return StringResource(link) { injector(this, trimmedText) }
    }

    private fun injectReflowableHtml(content: String): String {
        var resourceHtml = content
        // Inject links to css and js files
        var beginHeadIndex = resourceHtml.indexOf("<head>", 0, false) + 6
        var endHeadIndex = resourceHtml.indexOf("</head>", 0, false)
        if (endHeadIndex == -1)
            return content

        val cssStyle = publication.cssStyle

        val endIncludes = mutableListOf<String>()
        val beginIncludes = mutableListOf<String>()
        beginIncludes.add("<meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\" />")

        beginIncludes.add(getHtmlLink("/"+ Injectable.Style.rawValue +"/$cssStyle-before.css"))
        beginIncludes.add(getHtmlLink("/"+ Injectable.Style.rawValue +"/$cssStyle-default.css"))
        endIncludes.add(getHtmlLink("/"+ Injectable.Style.rawValue +"/$cssStyle-after.css"))
        endIncludes.add(getHtmlScript("/"+ Injectable.Script.rawValue +"/touchHandling.js"))
        endIncludes.add(getHtmlScript("/"+ Injectable.Script.rawValue +"/utils.js"))

        customResources?.let {
            // Inject all custom resourses
            for ((key, value) in it.resources) {
                if (value is Pair<*, *>) {
                    val res = value as Pair<String, String>
                    if (Injectable(res.second) == Injectable.Script) {
                        endIncludes.add(getHtmlScript("/${Injectable.Script.rawValue}/$key"))
                    } else if (Injectable(res.second) == Injectable.Style) {
                        endIncludes.add(getHtmlLink("/${Injectable.Style.rawValue}/$key"))
                    }
                }
            }
        }

        for (element in beginIncludes) {
            resourceHtml = StringBuilder(resourceHtml).insert(beginHeadIndex, element).toString()
            beginHeadIndex += element.length
            endHeadIndex += element.length
        }
        for (element in endIncludes) {
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
            endHeadIndex += element.length
        }
        resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, getHtmlFont("/fonts/OpenDyslexic-Regular.otf")).toString()
        resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, "<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>\n").toString()

        // Inject userProperties
        getProperties(publication.userSettingsUIPreset)?.let { propertyPair ->
            val html = Regex("""<html.*>""").find(resourceHtml, 0)
            html?.let {
                val match = Regex("""(style=("([^"]*)"[ >]))|(style='([^']*)'[ >])""").find(html.value, 0)
                if (match != null) {
                    val beginStyle = match.range.first + 7
                    var newHtml = html.value
                    newHtml = StringBuilder(newHtml).insert(beginStyle, "${buildStringProperties(propertyPair)} ").toString()
                    resourceHtml = StringBuilder(resourceHtml).replace(Regex("""<html.*>"""), newHtml)
                } else {
                    val beginHtmlIndex = resourceHtml.indexOf("<html", 0, false) + 5
                    resourceHtml = StringBuilder(resourceHtml).insert(beginHtmlIndex, " style=\"${buildStringProperties(propertyPair)}\"").toString()
                }
            } ?:run {
                val beginHtmlIndex = resourceHtml.indexOf("<html", 0, false) + 5
                resourceHtml = StringBuilder(resourceHtml).insert(beginHtmlIndex, " style=\"${buildStringProperties(propertyPair)}\"").toString()
            }
        }

        resourceHtml = applyDirectionAttribute(resourceHtml, publication)

        return resourceHtml
    }

    private fun applyDirectionAttribute(resourceHtml: String, publication: Publication): String {
        var resourceHtml1 = resourceHtml
        fun addRTLDir(tagName: String, html: String): String {
            return Regex("""<$tagName.*>""").find(html, 0)?.let { result ->
                Regex("""dir=""").find(result.value, 0)?.let {
                    html
                } ?: run {
                    val beginHtmlIndex = html.indexOf("<$tagName", 0, false) + 5
                    StringBuilder(html).insert(beginHtmlIndex, " dir=\"rtl\"").toString()
                }
            } ?: run {
                html
            }
        }

        if (publication.cssStyle == "rtl") {
            resourceHtml1 = addRTLDir("html", resourceHtml1)
            resourceHtml1 = addRTLDir("body", resourceHtml1)
        }

        return resourceHtml1
    }

    private fun injectFixedLayoutHtml(content: String): String {
        var resourceHtml = content
        val endHeadIndex = resourceHtml.indexOf("</head>", 0, false)
        if (endHeadIndex == -1)
            return content
        val includes = mutableListOf<String>()
        includes.add(getHtmlScript("/"+ Injectable.Script.rawValue +"/touchHandling.js"))
        includes.add(getHtmlScript("/"+ Injectable.Script.rawValue +"/utils.js"))
        for (element in includes) {
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
        }
        return resourceHtml
    }

    private fun getHtmlFont(resourceName: String): String {
        val prefix = "<style type=\"text/css\"> @font-face{font-family: \"OpenDyslexic\"; src:url(\""
        val suffix = "\") format('truetype');}</style>\n"
        return prefix + resourceName + suffix
    }

    private fun getHtmlLink(resourceName: String): String {
        val prefix = "<link rel=\"stylesheet\" type=\"text/css\" href=\""
        val suffix = "\"/>\n"
        return prefix + resourceName + suffix
    }

    private fun getHtmlScript(resourceName: String): String {
        val prefix = "<script type=\"text/javascript\" src=\""
        val suffix = "\"></script>\n"

        return prefix + resourceName + suffix
    }

    private fun getProperties(preset: MutableMap<ReadiumCSSName, Boolean>): MutableMap<String, String>? {

        // userProperties is a JSON string containing the css userProperties
        var userPropertiesString: String? = null
        userPropertiesPath?.let {
            userPropertiesString = String()
            val file = File(it)
            if (file.isFile && file.canRead()) {
                for (i in file.readLines()) {
                    userPropertiesString += i
                }
            }
        }

        return userPropertiesString?.let {
            // Parsing of the String into a JSONArray of JSONObject with each "name" and "value" of the css properties
            // Making that JSONArray a MutableMap<String, String> to make easier the access of data
            return@let try {
                val propertiesArray = JSONArray(userPropertiesString)
                val properties: MutableMap<String, String> = mutableMapOf()
                for (i in 0 until propertiesArray.length()) {
                    val value = JSONObject(propertiesArray.getString(i))
                    var isInPreset = false

                    for (property in preset) {
                        if (property.key.ref == value.getString("name")) {
                            isInPreset = true
                            val presetPair = Pair(property.key, preset[property.key])
                            val presetValue = applyPreset(presetPair)
                            properties[presetValue.getString("name")] = presetValue.getString("value")
                        }
                    }

                    if (!isInPreset) {
                        properties[value.getString("name")] = value.getString("value")
                    }

                }
                properties
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun applyPreset(preset: Pair<ReadiumCSSName, Boolean?>): JSONObject {
        val readiumCSSProperty = JSONObject()

        readiumCSSProperty.put("name", preset.first.ref)

        when(preset.first) {
            ReadiumCSSName.hyphens -> {
                readiumCSSProperty.put("value", "")
            }
            ReadiumCSSName.fontOverride -> {
                readiumCSSProperty.put("value", "readium-font-off")
            }
            ReadiumCSSName.appearance -> {
                readiumCSSProperty.put("value", "readium-default-on")
            }
            ReadiumCSSName.publisherDefault -> {
                readiumCSSProperty.put("value", "")
            }
            ReadiumCSSName.columnCount -> {
                readiumCSSProperty.put("value", "auto")
            }
            ReadiumCSSName.pageMargins -> {
                readiumCSSProperty.put("value", "0.5")
            }
            ReadiumCSSName.lineHeight -> {
                readiumCSSProperty.put("value", "1.0")
            }
            ReadiumCSSName.ligatures -> {
                readiumCSSProperty.put("value", "")
            }
            ReadiumCSSName.fontFamily -> {
                readiumCSSProperty.put("value", "Original")
            }
            ReadiumCSSName.fontSize -> {
                readiumCSSProperty.put("value", "100%")
            }
            ReadiumCSSName.wordSpacing -> {
                readiumCSSProperty.put("value", "0.0rem")
            }
            ReadiumCSSName.letterSpacing -> {
                readiumCSSProperty.put("value", "0.0em")
            }
            ReadiumCSSName.textAlignment -> {
                readiumCSSProperty.put("value", "justify")
            }
            ReadiumCSSName.paraIndent -> {
                readiumCSSProperty.put("value", "")
            }
            ReadiumCSSName.scroll -> {
                if (preset.second!!) {
                    readiumCSSProperty.put("value", "readium-scroll-on")
                } else {
                    readiumCSSProperty.put("value", "readium-scroll-off")
                }
            }
        }

        return readiumCSSProperty
    }

    private fun buildStringProperties(list: MutableMap<String, String>): String {
        var string = ""
        for (property in list) {
            string = string + " " + property.key + ": " + property.value + ";"
        }
        return string
    }

}