package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.TransformingResource
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isProtected
import timber.log.Timber

/**
 * Injects the Readium CSS files and scripts in the HTML [Resource] receiver.
 *
 * @param baseHref Base URL where the Readium CSS and scripts are served.
 */
@OptIn(ExperimentalReadiumApi::class)
internal fun Resource.injectHtml(publication: Publication, css: ReadiumCss, baseHref: String): Resource =
    TransformingResource(this) { bytes ->
        val link = link()
        check(link.mediaType.isHtml)

        var content = bytes.toString(link.mediaType.charset ?: Charsets.UTF_8).trim()
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
        if (publication.isProtected) {
            injectables.add("""
                <style>
                *:not(input):not(textarea) {
                    user-select: none;
                    -webkit-user-select: none;
                }
                </style>
            """)
        }

        val headEndIndex = content.indexOf("</head>", 0, true)
        if (headEndIndex == -1) {
            Timber.e("</head> closing tag not found in ${link.href}")
        } else {
            content = StringBuilder(content)
                .insert(headEndIndex, "\n" + injectables.joinToString("\n") + "\n")
                .toString()
        }

        content.toByteArray()
    }

private fun script(src: String): String =
    """<script type="text/javascript" src="$src"></script>"""
