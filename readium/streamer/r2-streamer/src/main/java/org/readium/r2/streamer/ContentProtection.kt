/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Try

/**
 * Bridge between a Content Protection technology and the Streamer.
 *
 * Its responsibilities are to:
 * - Unlock a publication by returning a customized Fetcher.
 * - Create a ContentProtectionService publication service.
 */
interface ContentProtection {

    /**
     * Attempts to unlock a potentially protected file.
     *
     * @param file
     * @param fetcher The Streamer will create a leaf Fetcher for the low-level file access (e.g. ArchiveFetcher for a ZIP archive),
     *  to avoid having each Content Protection open the file to check if it's protected or not.
     *  A publication might be protected in such a way that the package format can't be recognized,
     *  in which case the Content Protection will have the responsibility of creating a new leaf Fetcher.
     * @return a ProtectedFile in case of success, null if the file is not protected by this technology,
     *  a Streamer.Error if the file can't be successfully opened.
     */
    suspend fun open(
        file: File,
        fetcher: Fetcher,
        askCredentials: Boolean,
        credentials: String?,
        sender: Any?,
        onAskCredentials: OnAskCredentialsCallback?
    ): Try<ProtectedFile, Streamer.Error>?

    /**
     * Holds the result of opening a File with a ContentProtection.
     *
     * @property file  Protected file which will be provided to the parsers.
     * In most cases, this will be the file provided to ContentProtection::open(),
     * but a Content Protection might modify it in some cases:
     * - If the original file has a media type that can't be recognized by parsers,
     *   the Content Protection must return a file with the matching unprotected media type.
     * - If the original file has a media type that can't be recognized by parsers,
     *   the Content Protection must return a file with the matching unprotected media type.
     *
     * @property fetcher Primary leaf fetcher to be used by parsers.
     * The Content Protection can unlock resources by modifying the Fetcher provided to ContentProtection::open(),
     * for example by:
     * - Wrapping the given fetcher in a TransformingFetcher with a decryption Resource.Transformer function.
     * - Discarding the provided fetcher altogether and creating a new one to handle access restrictions.
     *   For example, by creating an HTTPFetcher which will inject a Bearer Token in requests.
     *
     * @property contentProtectionServiceFactory Factory for the Content Protection Publication Service
     * that will be added to the created Publication by the Streamer.
     */
    data class ProtectedFile(
        val file: File,
        val fetcher: Fetcher,
        val contentProtectionServiceFactory: ((Publication.Service.Context) -> ContentProtectionService?)?
    )
}