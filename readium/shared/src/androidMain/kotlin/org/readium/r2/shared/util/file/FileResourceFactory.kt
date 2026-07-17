/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.file

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory

/**
 * Creates [FileResource] instances granting access to `file://` URLs stored on the file system.
 */
public class FileResourceFactory : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl,
    ): Try<Resource, ResourceFactory.Error> {
        val file = url.toFile()
            ?: return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))

        val resource = FileResource(file)

        return Try.success(resource)
    }
}
