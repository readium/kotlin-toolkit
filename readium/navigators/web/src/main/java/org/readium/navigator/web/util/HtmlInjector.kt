/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.util

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import timber.log.Timber

/**
 * Injects scripts in the HTML [Resource] receiver.
 *
 * @param assetsBaseHref Base URL where and scripts are served.
 */
internal fun Resource.injectHtml(
    injectableScript: RelativeUrl,
    mediaType: MediaType,
    assetsBaseHref: AbsoluteUrl,
    disableSelection: Boolean,
): Resource =
    TransformingResource(this) { bytes ->
        if (!mediaType.isHtml) {
            return@TransformingResource Try.success(bytes)
        }

        var content = bytes.toString(mediaType.charset ?: Charsets.UTF_8).trim()
        val injectables = mutableListOf<String>()

        injectables.add(
            script(assetsBaseHref.resolve(injectableScript))
        )

        // Disable the text selection if the publication is protected.
        // FIXME: This is a hack until proper LCP copy is implemented, see https://github.com/readium/kotlin-toolkit/issues/221
        if (disableSelection) {
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
            Timber.e("</head> closing tag not found in resource with href: $sourceUrl")
        } else {
            content = StringBuilder(content)
                .insert(headEndIndex, "\n" + injectables.joinToString("\n") + "\n")
                .toString()
        }

        Try.success(content.toByteArray())
    }

private fun script(src: Url): String =
    """<script type="text/javascript" src="$src"></script>"""
