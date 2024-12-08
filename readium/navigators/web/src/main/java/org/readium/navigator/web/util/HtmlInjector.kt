/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.util

import java.nio.charset.Charset
import org.readium.navigator.web.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import timber.log.Timber

/**
 * Injects scripts in the HTML [Resource] receiver.
 *
 * @param assetsBaseHref Base URL where and scripts are served.
 */
internal fun Resource.injectHtml(
    charset: Charset?,
    injectableScript: RelativeUrl,
    assetsBaseHref: AbsoluteUrl,
    disableSelection: Boolean,
): Resource =
    TransformingResource(this) { bytes ->
        var content = bytes.toString(charset ?: Charsets.UTF_8).trim()
        val injectables = buildList {
            add(script(assetsBaseHref.resolve(injectableScript)))

            if (disableSelection) {
                add(disableSelectionInjectable)
            }
        }

        content = content.inject(sourceUrl, injectables)
        Try.success(content.toByteArray())
    }

/**
 * Injects scripts in the HTML [Resource] receiver.
 *
 * @param assetsBaseHref Base URL where and scripts are served.
 */
@OptIn(ExperimentalReadiumApi::class)
internal fun Resource.injectHtmlReflowable(
    charset: Charset?,
    readiumCss: ReadiumCss,
    injectableScript: RelativeUrl,
    assetsBaseHref: AbsoluteUrl,
    disableSelection: Boolean,
): Resource =
    TransformingResource(this) { bytes ->
        var content = bytes.toString(charset ?: Charsets.UTF_8).trim()

        content = try {
            readiumCss.injectHtml(content)
        } catch (e: Exception) {
            return@TransformingResource Try.failure(ReadError.Decoding(e))
        }

        val injectables = buildList {
            add(
                script(
                    assetsBaseHref.resolve(
                        Url("readium/navigators/web/reflowable-injectable-script.js")!!
                    )
                )
            )

            // Disable the text selection if the publication is protected.
            if (disableSelection) {
                add(disableSelectionInjectable)
            }
        }

        content = content.inject(sourceUrl, injectables)

        Try.success(content.toByteArray())
    }

// FIXME: This is a hack until proper LCP copy is implemented, see https://github.com/readium/kotlin-toolkit/issues/221
private const val disableSelectionInjectable: String =
    """
        <style>
        *:not(input):not(textarea) {
            user-select: none;
            -webkit-user-select: none;
        }
        </style>
    """

private fun script(src: Url): String =
    """<script type="text/javascript" src="$src"></script>"""

private fun String.inject(
    sourceUrl: AbsoluteUrl?,
    injectables: List<String>,
): String {
    val headEndIndex = this.indexOf("</head>", 0, true)
    return if (headEndIndex == -1) {
        Timber.e("</head> closing tag not found in resource with href: $sourceUrl")
        this
    } else {
        StringBuilder(this)
            .insert(headEndIndex, "\n" + injectables.joinToString("\n") + "\n")
            .toString()
    }
}
