package org.readium.r2.testapp.drm

import android.app.ProgressDialog
import android.net.Uri
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRM
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.testapp.Book
import java.io.File


data class DRMFulfilledPublication(
        val localURL: String,
        val suggestedFilename: String) {}

interface DRMLibraryService {
    val brand: DRM.Brand
    fun canFulfill(file: String) : Boolean
    fun fulfill(byteArray: ByteArray, completion: (DRMFulfilledPublication) -> Unit)
    fun loadPublication(publication: String, drm: DRM, completion: (DRM?) -> Unit)
}

interface LCPLibraryActivityService {
    fun parseIntentLcpl(uriString: String, networkAvailable: Boolean)
    fun prepareAndStartActivityWithLCP(drm: DRM, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication, networkAvailable: Boolean)
    fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog, networkAvailable: Boolean)
}
