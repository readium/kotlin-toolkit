/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
@Throws
fun ReadiumCss.injectHtml(html: String, metadata: Metadata): String {
    val htmlBuilder = StringBuilder(html)

    // Inject links to css and js files
    val head = regexForOpeningHtmlTag("head").find(htmlBuilder, 0)
        ?: throw IllegalArgumentException("No <head> opening tag found in this resource")

    var beginHeadIndex = head.range.last + 1
    var endHeadIndex = htmlBuilder.indexOf("</head>", 0, true).takeIf { it != -1 }
        ?: throw IllegalArgumentException("No </head> closing tag found in this resource")

    val layout = CssLayout(metadata)

    val endIncludes = mutableListOf<String>()
    val beginIncludes = mutableListOf<String>()
    beginIncludes.add(htmlLink("/assets/readium-css/${layout.readiumCSSPath}ReadiumCSS-before.css"))

    // Fix Readium CSS issue with the positioning of <audio> elements.
    // https://github.com/readium/readium-css/issues/94
    // https://github.com/readium/r2-navigator-kotlin/issues/193
    beginIncludes.add("""
        <style>
        audio[controls] {
            width: revert;
            height: revert;
        }
        </style>
    """.trimIndent())

    endIncludes.add(htmlLink("/assets/readium-css/${layout.readiumCSSPath}ReadiumCSS-after.css"))

    for (element in beginIncludes) {
        htmlBuilder.insert(beginHeadIndex, element)
        beginHeadIndex += element.length
        endHeadIndex += element.length
    }
    for (element in endIncludes) {
        htmlBuilder.insert(endHeadIndex, element)
        endHeadIndex += element.length
    }
    htmlBuilder.insert(endHeadIndex, htmlFont(fontFamily = "OpenDyslexic", href = "/assets/fonts/OpenDyslexic-Regular.otf"))
    htmlBuilder.insert(endHeadIndex, "<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>\n")

    // Inject userProperties
//    getProperties(publication.userSettingsUIPreset)?.let { propertyPair ->
//        val htmlTag = regexForOpeningHtmlTag("html").find(html, 0)
//        htmlTag?.let {
//            val match = Regex("""(style=("([^"]*)"[ >]))|(style='([^']*)'[ >])""").find(htmlTag.value, 0)
//            if (match != null) {
//                val beginStyle = match.range.first + 7
//                var newHtml = htmlTag.value
//                newHtml = StringBuilder(newHtml).insert(beginStyle, "${buildStringProperties(propertyPair)} ").toString()
//                html = StringBuilder(html).replace(regexForOpeningHtmlTag("html"), newHtml)
//            } else {
//                val beginHtmlIndex = htmlTag.indexOf("<html", 0, true) + 5
//                html = StringBuilder(html).insert(beginHtmlIndex, " style=\"${buildStringProperties(propertyPair)}\"").toString()
//            }
//        } ?:run {
//            val beginHtmlIndex = htmlTag.indexOf("<html", 0, true) + 5
//            html = StringBuilder(html).insert(beginHtmlIndex, " style=\"${buildStringProperties(propertyPair)}\"").toString()
//        }
//    }

    htmlBuilder.applyDirectionAttribute(layout)

    return htmlBuilder.toString()
}

private fun htmlLink(href: String): String = """
    <link rel="stylesheet" type="text/css" href="$href"/>
""".trimIndent()

private fun htmlFont(fontFamily: String, href: String): String = """
    <style type="text/css">
        @font-face {
            font-family: "$fontFamily";
            src: url("$href") format('truetype');
        }
    </style>
""".trimIndent()

private fun StringBuilder.applyDirectionAttribute(layout: CssLayout) {
    if (layout == CssLayout.Rtl) {
        fun addRTLDir(tagName: String) {
            val match = regexForOpeningHtmlTag(tagName).find(this, 0) ?: return
            if (match.value.contains("dir=")) return

            val beginHtmlIndex = indexOf("<$tagName", 0, true) + 5
            insert(beginHtmlIndex, " dir=\"rtl\"")
        }

        addRTLDir("html")
        addRTLDir("body")
    }
}

private fun regexForOpeningHtmlTag(name: String): Regex =
    Regex("""<$name.*?>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
