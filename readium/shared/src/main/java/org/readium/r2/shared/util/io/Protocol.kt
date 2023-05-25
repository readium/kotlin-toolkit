/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.io

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.ExplodedPackage
import org.readium.r2.shared.util.archive.Package

fun interface Protocol {

    suspend fun open(url: Url): Either<Resource, Package>?
}

class FileProtocol(
    private val archiveFactory: ArchiveFactory
) : Protocol {

    override suspend fun open(url: Url): Either<Resource, Package>? {
        if (url.scheme != ContentResolver.SCHEME_FILE) {
            return null
        }

        return open(File(url.path))
    }

    private suspend fun open(file: File): Either<Resource, Package>? {
        if (file.isDirectory) {
            return Either.Right(ExplodedPackage(file))
        }

        val resource = FileFetcher.FileResource(Link(file.path), file)

        archiveFactory.open(resource, password = null)
            .getOrNull()
            ?.let { return Either.Right(it) }

        if (tryOr(false) { file.exists() }) {
            return Either.Left(resource)
        }

        return null
    }
}

class ContentProtocol(
    private val contentResolver: ContentResolver
) : Protocol {

    override suspend fun open(url: Url): Either<Resource, Package>? {
        if (url.scheme != ContentResolver.SCHEME_CONTENT) {
            return null
        }

        return open(url)
    }

    @Suppress("Unused_parameter")
    private fun open(uri: Uri): Either<Resource, Package>? {
        return null
    }
}
