/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.web.reflowable.injection

import java.nio.charset.Charset
import org.readium.navigator.web.internals.util.disableSelectionInjectable
import org.readium.navigator.web.internals.util.inject
import org.readium.navigator.web.internals.util.script
import org.readium.navigator.web.reflowable.css.ReadiumCssInjector
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource

/**
 * Injects scripts for reflowable publications in the HTML [Resource] receiver.
 *
 * @param assetsBaseHref Base URL where and scripts are served.
 */
internal fun Resource.injectHtmlReflowable(
    charset: Charset?,
    readiumCss: ReadiumCssInjector,
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
                    assetsBaseHref.resolve(injectableScript)
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
