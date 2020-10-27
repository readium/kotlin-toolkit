/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Try

/**
 * Bridge between a Content Protection technology and the Readium toolkit.
 *
 * Its responsibilities are to:
 * - Unlock a publication by returning a customized [Fetcher].
 * - Create a [ContentProtectionService] publication service.
 */
interface ContentProtection {

    /**
     * Attempts to unlock a potentially protected file.
     *
     * The Streamer will create a leaf [fetcher] for the low-level [file] access (e.g.
     * ArchiveFetcher for a ZIP archive), to avoid having each Content Protection open the file to
     * check if it's protected or not.
     *
     * A publication might be protected in such a way that the package format can't be recognized,
     * in which case the Content Protection will have the responsibility of creating a new leaf
     * [Fetcher].
     *
     * @return A [ProtectedFile] in case of success, null if the file is not protected by this
     * technology or a [Publication.OpeningException] if the file can't be successfully opened,
     * even in restricted mode.
     */
    suspend fun open(
        file: File,
        fetcher: Fetcher,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ProtectedFile, Publication.OpeningException>?

    /**
     * Holds the result of opening a [File] with a [ContentProtection].
     *
     * @property file Protected file which will be provided to the parsers.
     * In most cases, this will be the file provided to ContentProtection::open(),
     * but a Content Protection might modify it in some cases:
     * - If the original file has a media type that can't be recognized by parsers,
     *   the Content Protection must return a file with the matching unprotected media type.
     * - If the Content Protection technology needs to redirect the Streamer to a different file.
     *   For example, this could be used to decrypt a publication to a temporary secure location.
     *
     * @property fetcher Primary leaf fetcher to be used by parsers.
     * The Content Protection can unlock resources by modifying the Fetcher provided to
     * ContentProtection::open(), for example by:
     * - Wrapping the given fetcher in a TransformingFetcher with a decryption Resource.Transformer
     *   function.
     * - Discarding the provided fetcher altogether and creating a new one to handle access
     *   restrictions. For example, by creating an HTTPFetcher which will inject a Bearer Token in
     *   requests.
     *
     * @property onCreatePublication Called on every parsed Publication.Builder.
     * It can be used to modify the `Manifest`, the root [Fetcher] or the list of service factories
     * of a [Publication].
     */
    data class ProtectedFile(
        val file: File,
        val fetcher: Fetcher,
        val onCreatePublication: Publication.Builder.() -> Unit = {}
    )

}
