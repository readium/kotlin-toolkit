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
import org.readium.r2.shared.util.Dialog
import org.readium.r2.shared.util.Try

/** Called when a content protection wants to prompt the user for its credentials. */
typealias OnAskCredentials = (dialog: Dialog, sender: Any?, callback: (String?) -> Unit) -> Unit

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
     * technology or a [Publication.OpeningError] if the file can't be successfully opened.
     */
    suspend fun open(
        file: File,
        fetcher: Fetcher,
        askCredentials: Boolean,
        credentials: String?,
        sender: Any?,
        onAskCredentials: OnAskCredentials?
    ): Try<ProtectedFile, Publication.OpeningError>?

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
     * @property onCreateServices Called before creating the Publication, to modify its list of service
     * factories. Can be use to add a Content Protection Service to the Publication that will be created
     * by the Streamer.
     *
     * @property onCreateManifest Called before creating the Publication, to modify the parsed [Manifest]
     * if desired.
     */
    data class ProtectedFile(
        val file: File,
        val fetcher: Fetcher,
        val onCreateServices: OnCreateServices? = null,
        val onCreateManifest: OnCreateManifest? = null
    )

}
