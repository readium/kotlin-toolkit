/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.content

import android.content.ContentResolver
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.shared.util.toUri

/**
 * Creates [Resource] instances granting access to `content://` URLs provided by the given
 * [contentResolver].
 */
public class ContentResourceFactory(
    private val contentResolver: ContentResolver,
) : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl,
    ): Try<Resource, ResourceFactory.Error> {
        if (!url.isContent) {
            return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))
        }

        val resource = ContentResource(url.toUri(), contentResolver)

        return Try.success(resource)
    }
}
