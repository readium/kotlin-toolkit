/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.util.URITemplate
import org.readium.r2.shared.util.Url as SharedUrl
import timber.log.Timber

/**
 * An hypertext reference points to a resource in a [Publication].
 *
 * It is potentially templated, use [resolve] to get the actual URL.
 */
@Parcelize
public class Href private constructor(private val href: Url) : Parcelable {

    /**
     * Creates an [Href] from a valid URL.
     */
    public constructor(href: SharedUrl) : this(StaticUrl(href))

    public companion object {
        /**
         * Creates an [Href] from a valid URL or URL template (RFC 6570).
         *
         * @param templated Indicates whether [href] is a URL template.
         */
        public operator fun invoke(href: String, templated: Boolean = false): Href? =
            if (templated) {
                Href(TemplatedUrl(href))
            } else {
                SharedUrl(href)?.let { Href(StaticUrl(it)) }
            }
    }

    /**
     * Returns the URL represented by this HREF, resolved to the given [base] URL.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun resolve(
        base: SharedUrl? = null,
        parameters: Map<String, String> = emptyMap(),
    ): SharedUrl = href.resolve(base, parameters)

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
        abstract fun resolve(base: SharedUrl?, parameters: Map<String, String>): SharedUrl
        abstract val parameters: List<String>?
    }

    /**
     * A static hypertext reference to a publication resource.
     */
    @Parcelize
    private data class StaticUrl(val url: SharedUrl) : Url() {
        @IgnoredOnParcel
        override val parameters: List<String>? = null

        override fun resolve(base: SharedUrl?, parameters: Map<String, String>): SharedUrl =
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

        override fun resolve(base: SharedUrl?, parameters: Map<String, String>): SharedUrl {
            val url = checkNotNull(SharedUrl(URITemplate(template).expand(parameters)))
            return base?.resolve(url) ?: url
        }

        override fun toString(): String = template
    }
}
