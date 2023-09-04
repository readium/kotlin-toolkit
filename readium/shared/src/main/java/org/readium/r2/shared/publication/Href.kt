/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.URITemplate
import org.readium.r2.shared.util.Url

/**
 * An hypertext reference points to a resource in a [Publication].
 */
public sealed class Href : Parcelable {

    /**
     * Returns the URL represented by this HREF, resolved to the given [base] URL.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public abstract fun toUrl(
        base: Url? = null,
        parameters: Map<String, String> = emptyMap()
    ): Url?

    /**
     * Syntactic sugar for [toUrl].
     */
    public operator fun invoke(
        base: Url? = null,
        parameters: Map<String, String> = emptyMap()
    ): Url? = toUrl(base, parameters)

    @ExperimentalReadiumApi
    public fun copy(transformer: ManifestTransformer): Href =
        transformer.transform(this)
}

/**
 * A static hypertext reference to a publication resource.
 */
@Parcelize
public class UrlHref(public val url: Url) : Href() {
    override fun toUrl(base: Url?, parameters: Map<String, String>): Url =
        base?.resolve(url) ?: url

    override fun toString(): String = url.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Url) {
            throw IllegalArgumentException(
                "You cannot compare an Href to a Url. Use `href.toUrl() == url` instead."
            )
        }
        if (javaClass != other?.javaClass) return false

        other as UrlHref

        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int = url.hashCode()
}

/**
 * A templated hypertext reference to a publication resource.
 *
 * @param template The URL template, as defined in RFC 6570.
 */
@Parcelize
public class TemplatedHref(public val template: String) : Href() {

    override fun toUrl(base: Url?, parameters: Map<String, String>): Url? =
        Url(URITemplate(template).expand(parameters))
            ?.let { base?.resolve(it) ?: it }

    /**
     * List of URI template parameter keys.
     */
    public val parameters: List<String> get() =
        URITemplate(template).parameters

    override fun toString(): String = template

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Url) {
            throw IllegalArgumentException(
                "You cannot compare an Href to a Url. Use `href.toUrl() == url` instead."
            )
        }
        if (javaClass != other?.javaClass) return false

        other as TemplatedHref

        if (template != other.template) return false

        return true
    }

    override fun hashCode(): Int = template.hashCode()
}

/**
 * Returns a copy of the receiver after normalizing its HREFs to the link with `rel="self"`.
 */
@ExperimentalReadiumApi
public fun Manifest.normalizeHrefsToSelf(): Manifest =
    normalizeHrefsToSelfOrBase(null)

/**
 * Returns a copy of the receiver after normalizing its HREFs to the link with `rel="self"`, or the
 * given [baseUrl] if not found.
 */
@ExperimentalReadiumApi
public fun Manifest.normalizeHrefsToSelfOrBase(baseUrl: Url?): Manifest {
    val base = linkWithRel("self")?.href?.toUrl()
        ?: baseUrl
        ?: return this

    return copy(HrefNormalizer(base))
}

/**
 * Returns a copy of the receiver after normalizing its HREFs to the given [baseUrl].
 */
@ExperimentalReadiumApi
public fun Link.normalizeHrefsToBase(baseUrl: Url?): Link {
    baseUrl ?: return this
    return copy(HrefNormalizer(baseUrl))
}

@OptIn(ExperimentalReadiumApi::class)
private class HrefNormalizer(private val baseUrl: Url) : ManifestTransformer {
    override fun transform(href: Href): Href {
        return when (href) {
            is TemplatedHref ->
                Url(href.template)
                    ?.let { TemplatedHref(baseUrl.resolve(it).toString()) }
                    ?: href

            is UrlHref ->
                UrlHref(baseUrl.resolve(href.url))
        }
    }
}
