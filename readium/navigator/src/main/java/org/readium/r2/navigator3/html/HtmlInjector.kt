/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumannn, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator3.html

import org.readium.r2.shared.fetcher.*
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.layoutOf
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isProtected
import timber.log.Timber

internal class HtmlInjector(
    val publication: Publication,
) {

    fun transform(resource: Resource): Resource = LazyResource {

        val link = resource.link()
        if (link.mediaType.isHtml)
            inject(resource)
        else
            resource
    }

    private suspend fun inject(resource: Resource): Resource = object : TransformingResource(resource) {

        override suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray> =
            resource.read().mapCatching {
                val trimmedText = it.toString(link().mediaType.charset ?: Charsets.UTF_8).trim()
                val res = if (publication.metadata.presentation.layoutOf(link()) == EpubLayout.REFLOWABLE)
                    injectReflowableHtml(trimmedText)
                else
                    injectFixedLayoutHtml(trimmedText)
                res.toByteArray()
            }

    }

    private fun injectReflowableHtml(content: String): String {
        var resourceHtml = content
        // Inject links to css and js files
        val head = regexForOpeningHTMLTag("head").find(resourceHtml, 0)
        if (head == null) {
            Timber.e("No <head> tag found in this resource")
            return resourceHtml
        }
        var beginHeadIndex = head.range.last + 1
        var endHeadIndex = resourceHtml.indexOf("</head>", 0, true)
        if (endHeadIndex == -1)
            return content

        val layout = ReadiumCssLayout(publication.metadata)

        val endIncludes = mutableListOf<String>()
        val beginIncludes = mutableListOf<String>()
        beginIncludes.add(getHtmlLink("/assets/readium-css/${layout.readiumCSSPath}ReadiumCSS-before.css"))
        endIncludes.add(getHtmlLink("/assets/readium-css/${layout.readiumCSSPath}ReadiumCSS-after.css"))
        endIncludes.add(getHtmlScript("/assets/scripts/readium-reflowable.js"))

        for (element in beginIncludes) {
            resourceHtml = StringBuilder(resourceHtml).insert(beginHeadIndex, element).toString()
            beginHeadIndex += element.length
            endHeadIndex += element.length
        }
        for (element in endIncludes) {
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
            endHeadIndex += element.length
        }
        resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, getHtmlFont(fontFamily = "OpenDyslexic", href = "/assets/fonts/OpenDyslexic-Regular.otf")).toString()
        resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, "<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>\n").toString()

        // Disable the text selection if the publication is protected.
        // FIXME: This is a hack until proper LCP copy is implemented, see https://github.com/readium/r2-testapp-kotlin/issues/266
        if (publication.isProtected) {
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, """
                <style>
                *:not(input):not(textarea) {
                    user-select: none;
                    -webkit-user-select: none;
                }
                </style>
            """).toString()
        }

        resourceHtml = applyDirectionAttribute(resourceHtml, publication)

        return resourceHtml
    }

    private fun applyDirectionAttribute(resourceHtml: String, publication: Publication): String {
        var resourceHtml1 = resourceHtml
        fun addRTLDir(tagName: String, html: String): String {
            return regexForOpeningHTMLTag(tagName).find(html, 0)?.let { result ->
                Regex("""dir=""").find(result.value, 0)?.let {
                    html
                } ?: run {
                    val beginHtmlIndex = html.indexOf("<$tagName", 0, true) + 5
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

    private fun regexForOpeningHTMLTag(name: String): Regex =
        Regex("""<$name.*?>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    private fun injectFixedLayoutHtml(content: String): String {
        var resourceHtml = content
        val endHeadIndex = resourceHtml.indexOf("</head>", 0, true)
        if (endHeadIndex == -1)
            return content
        val includes = mutableListOf<String>()
        includes.add(getHtmlScript("/assets/scripts/readium-fixed.js"))
        for (element in includes) {
            resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
        }
        return resourceHtml
    }

    private fun getHtmlFont(fontFamily: String, href: String): String {
        val prefix = "<style type=\"text/css\"> @font-face{font-family: \"$fontFamily\"; src:url(\""
        val suffix = "\") format('truetype');}</style>\n"
        return prefix + href + suffix
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
}
