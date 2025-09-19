/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.injection

import java.nio.charset.Charset
import org.readium.navigator.web.internals.util.disableSelectionInjectable
import org.readium.navigator.web.internals.util.inject
import org.readium.navigator.web.internals.util.script
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource

/**
 * Injects scripts for fixed layout publications in the HTML [Resource] receiver.
 *
 * @param assetsBaseHref Base URL where and scripts are served.
 */
internal fun Resource.injectHtmlFixedLayout(
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
