/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.URITemplate
import org.readium.r2.shared.util.Url as SharedUrl
import org.readium.r2.shared.util.toUrl
import timber.log.Timber

/**
 * An hypertext reference points to a resource in a [Publication].
 */
@Parcelize
public class Href private constructor(private val href: Url) : Parcelable {

    public companion object {
        public operator fun invoke(href: SharedUrl): Href =
            Href(StaticUrl(href))

        public operator fun invoke(href: String, templated: Boolean = false): Href? {
            val url =
                if (templated) {
                    TemplatedUrl(href)
                } else {
                    SharedUrl(href)?.let { StaticUrl(it) }
                }

            return url?.let { Href(it) }
        }
    }

    /**
     * Returns the URL represented by this HREF, resolved to the given [base] URL.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun toUrl(
        base: SharedUrl? = null,
        parameters: Map<String, String> = emptyMap()
    ): SharedUrl = href.toUrl(base, parameters)

    /**
     * Syntactic sugar for [toUrl].
     */
    public operator fun invoke(
        base: SharedUrl? = null,
        parameters: Map<String, String> = emptyMap()
    ): SharedUrl = href.toUrl(base, parameters)

    /**
     * Indicates whether this HREF is templated.
     */
    public val isTemplated: Boolean get() =
        href is TemplatedUrl

    /**
     * List of URI template parameter keys, if the HREF is templated.
     */
    public val parameters: List<String>? get() =
        href.parameters

    /**
     * Resolves the receiver HREF to the given [baseUrl].
     *
     * For example:
     *     href = "bar/baz"
     *     baseUrl = "http://example.com/foo/"
     *     result = "http://example.com/foo/bar/baz"
     */
    internal fun resolveTo(baseUrl: SharedUrl): Href =
        when (href) {
            is StaticUrl -> Href(StaticUrl(baseUrl.resolve(href.url)))
            is TemplatedUrl -> {
                Timber.w("Cannot safely resolve a URI template to a base URL before expanding it")
                this
            }
        }

    override fun toString(): String = href.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Href

        if (href != other.href) return false

        return true
    }

    override fun hashCode(): Int =
        href.hashCode()

    private sealed class Url : Parcelable {
        abstract fun toUrl(base: SharedUrl?, parameters: Map<String, String>): SharedUrl
        abstract val parameters: List<String>?
    }

    /**
     * A static hypertext reference to a publication resource.
     */
    @Parcelize
    private data class StaticUrl(val url: SharedUrl) : Url() {
        @IgnoredOnParcel
        override val parameters: List<String>? = null

        override fun toUrl(base: SharedUrl?, parameters: Map<String, String>): SharedUrl =
            base?.resolve(url) ?: url

        override fun toString(): String = url.toString()
    }

    /**
     * A templated hypertext reference to a publication resource.
     *
     * @param template The URL template, as defined in RFC 6570.
     */
    @Parcelize
    private data class TemplatedUrl(val template: String) : Url() {

        companion object {
            operator fun invoke(template: String): TemplatedUrl? {
                // Check that the produced URL is valid.
                SharedUrl(URITemplate(template).expand(emptyMap())) ?: return null
                return TemplatedUrl(template)
            }
        }

        @IgnoredOnParcel
        override val parameters: List<String> =
            URITemplate(template).parameters

        override fun toUrl(base: SharedUrl?, parameters: Map<String, String>): SharedUrl {
            val url = checkNotNull(SharedUrl(URITemplate(template).expand(parameters)))
            return base?.resolve(url) ?: url
        }

        override fun toString(): String = template
    }
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
public fun Manifest.normalizeHrefsToSelfOrBase(baseUrl: SharedUrl?): Manifest {
    val base = linkWithRel("self")?.href?.toUrl()
        ?: baseUrl
        ?: return this

    return copy(HrefNormalizer(base))
}

/**
 * Returns a copy of the receiver after normalizing its HREFs to the given [baseUrl].
 */
@ExperimentalReadiumApi
public fun Link.normalizeHrefsToBase(baseUrl: SharedUrl?): Link {
    baseUrl ?: return this
    return copy(HrefNormalizer(baseUrl))
}

@OptIn(ExperimentalReadiumApi::class)
private class HrefNormalizer(private val baseUrl: SharedUrl) : ManifestTransformer {
    override fun transform(href: Href): Href =
        href.resolveTo(baseUrl)
}
