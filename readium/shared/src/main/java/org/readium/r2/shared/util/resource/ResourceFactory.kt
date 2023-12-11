/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import kotlin.String
import kotlin.let
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A factory to read [Resource]s from [Url]s.
 */
public interface ResourceFactory {

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Url scheme $scheme is not supported.", cause)
    }

    /**
     * Creates a [Resource] to access [url].
     *
     * @param url The url the resource will access.
     * @param mediaType media type of the resource if known.
     */
    public suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType? = null
    ): Try<Resource, Error>
}

/**
 * A composite [ResourceFactory] which tries several factories until it finds one which supports
 * the url scheme.
 */
public class CompositeResourceFactory(
    private val factories: List<ResourceFactory>
) : ResourceFactory {

    public constructor(vararg factories: ResourceFactory) : this(factories.toList())

    override suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType?
    ): Try<Resource, ResourceFactory.Error> {
        for (factory in factories) {
            factory.create(url, mediaType)
                .getOrNull()
                ?.let { return Try.success(it) }
        }

        return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))
    }
}
