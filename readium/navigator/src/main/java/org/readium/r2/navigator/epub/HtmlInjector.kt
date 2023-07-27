/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.TransformingResource
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

/**
 * Injects the Readium CSS files and scripts in the HTML [Resource] receiver.
 *
 * @param baseHref Base URL where the Readium CSS and scripts are served.
 */
@OptIn(ExperimentalReadiumApi::class)
internal fun Resource.injectHtml(
    publication: Publication,
    css: ReadiumCss,
    baseHref: String,
    disableSelectionWhenProtected: Boolean
): Resource =
    TransformingResource(this) { bytes ->
        val mediaType = mediaType()
            .getOrElse {
                return@TransformingResource ResourceTry.failure(it)
            }
            ?.takeIf { it.isHtml }
            ?: return@TransformingResource ResourceTry.success(bytes)

        var content = bytes.toString(mediaType.charset ?: Charsets.UTF_8).trim()
        val injectables = mutableListOf<String>()

        val baseUri = baseHref.removeSuffix("/")
        if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE) {
            content = css.injectHtml(content)
            injectables.add(script("$baseUri/readium/scripts/readium-reflowable.js"))
        } else {
            injectables.add(script("$baseUri/readium/scripts/readium-fixed.js"))
        }

        // Disable the text selection if the publication is protected.
        // FIXME: This is a hack until proper LCP copy is implemented, see https://github.com/readium/kotlin-toolkit/issues/221
        if (disableSelectionWhenProtected && publication.isProtected) {
            injectables.add(
                """
                <style>
                *:not(input):not(textarea) {
                    user-select: none;
                    -webkit-user-select: none;
                }
                </style>
            """
            )
        }

        val headEndIndex = content.indexOf("</head>", 0, true)
        if (headEndIndex == -1) {
            Timber.e("</head> closing tag not found in resource with href: ${href ?: "null"}")
        } else {
            content = StringBuilder(content)
                .insert(headEndIndex, "\n" + injectables.joinToString("\n") + "\n")
                .toString()
        }

        Try.success(content.toByteArray())
    }

private fun script(src: String): String =
    """<script type="text/javascript" src="$src"></script>"""
