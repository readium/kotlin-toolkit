/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.io

import android.content.ContentResolver
import java.io.File
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.ContainerFactory
import org.readium.r2.shared.resource.FileResource
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.DirectoryContainer

class FileResourceFactory : ResourceFactory {

    override suspend fun create(url: Url): Try<Resource, Exception> {
        if (url.scheme != ContentResolver.SCHEME_FILE) {
            Try.failure(Exception("Scheme not supported"))
        }

        val file = File(url.path)

        return Try.success(FileResource(file))
    }
}

class DirectoryContainerFactory : ContainerFactory {

    override suspend fun create(url: Url): Try<Container, Exception> {
        if (url.scheme != ContentResolver.SCHEME_FILE) {
            Try.failure(Exception("Scheme not supported"))
        }

        val file = File(url.path)
        return Try.success(DirectoryContainer(file))
    }
}
