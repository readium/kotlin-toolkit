/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.drm

import android.app.ProgressDialog
import android.net.Uri
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.testapp.db.Book
import java.io.File


data class DRMFulfilledPublication(
        val localURL: String,
        val suggestedFilename: String)

interface DRMLibraryService {
    val brand: DRM.Brand
    fun canFulfill(file: String) : Boolean
    fun fulfill(byteArray: ByteArray, completion: (Any?) -> Unit)
    fun loadPublication(publication: String, drm: DRM, completion: (Any?) -> Unit)
}

interface LCPLibraryActivityService {
    fun parseIntentLcpl(uriString: String, networkAvailable: Boolean)
    fun prepareAndStartActivityWithLCP(drm: DRM, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication, networkAvailable: Boolean)
    fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog, networkAvailable: Boolean)
}
