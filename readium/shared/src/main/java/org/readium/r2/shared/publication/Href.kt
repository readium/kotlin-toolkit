/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.util.URITemplate
import org.readium.r2.shared.util.Url

public sealed class Href : Parcelable {

    /**
     * Returns the URL represented by this HREF, resolved to the given [base] URL.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public abstract fun toUrl(base: Url? = null, parameters: Map<String, String> = emptyMap()): Url?
}

@Parcelize
public data class UrlHref(val url: Url) : Href() {
    override fun toString(): String = url.toString()

    override fun toUrl(base: Url?, parameters: Map<String, String>): Url =
        base?.resolve(url) ?: url
}

@Parcelize
public data class TemplatedHref(val template: String) : Href() {

    override fun toString(): String = template

    override fun toUrl(base: Url?, parameters: Map<String, String>): Url? =
        Url(URITemplate(template).expand(parameters))
            ?.let { base?.resolve(it) ?: it }
}

/**
 * Resolves the given [Href] to this URL.
 */
internal fun Url?.resolve(href: Href): Href {
    this ?: return href

    return when (href) {
        is TemplatedHref -> Url(href.template)
            ?.let { TemplatedHref(resolve(it).toString()) }
            ?: href
        is UrlHref -> UrlHref(resolve(href.url))
    }
}
