// TODO password validation

/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import com.mcxiaoke.koi.ext.fileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.textInputLayout
import org.readium.r2.lcp.public.*
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRM
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.testapp.drm.DRMFulfilledPublication
import org.readium.r2.testapp.drm.DRMLibraryService
import org.readium.r2.testapp.drm.LCPLibraryActivityService
import timber.log.Timber
import java.io.File
import java.net.URL
import kotlin.coroutines.CoroutineContext

class CatalogActivity : LibraryActivity(), LCPLibraryActivityService, CoroutineScope, DRMLibraryService, LCPAuthenticating, LCPAuthenticationDelegate {


    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    lateinit var lcpService: LCPService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = this
        lcpService = R2MakeLCPService(this)
    }

    internal var authenticationCallbacks: MutableMap<String, (String?) -> Unit> = mutableMapOf()

    override val brand: DRM.Brand
        get() = DRM.Brand.lcp

    override fun canFulfill(file: String): Boolean =
            file.fileExtension().toLowerCase() == "lcpl"

    override fun fulfill(byteArray: ByteArray, completion: (DRMFulfilledPublication) -> Unit) {
        lcpService.importPublication(byteArray, this) { result, error ->
            result?.let {
                val publication = DRMFulfilledPublication(localURL = result.localURL, suggestedFilename = result.suggestedFilename)
                lcpService.retrieveLicense(result.localURL, this) { license, error ->
                    completion(publication)
                }
            }
            error?.let {
                throw error
            }
        }
    }

    override fun loadPublication(publication: String, drm: DRM, completion: (Any?) -> Unit) {
        lcpService.retrieveLicense(publication, this) { license, error ->
            license?.let {
                drm.license = license
                completion(drm)
            } ?: run {
                error?.let {
                    completion(error)
                }
            }
        }
    }

    override fun authenticate(license: LCPAuthenticatedLicense, passphrase: String) {
        val callback = authenticationCallbacks.remove(license.document.id) ?: return
        callback(passphrase)
    }

    override fun didCancelAuthentication(license: LCPAuthenticatedLicense) {
        val callback = authenticationCallbacks.remove(license.document.id) ?: return
        callback(null)
    }

    override fun requestPassphrase(license: LCPAuthenticatedLicense, reason: LCPAuthenticationReason, completion: (String?) -> Unit) {

        fun promptPassphrase(reason: String? = null, callback: (pass: String) -> Unit) {
            launch {
                var editTextTitle: EditText? = null

                alert(Appcompat, "Hint: " + license.hint, reason ?: "LCP Passphrase") {
                    customView {
                        verticalLayout {
                            textInputLayout {
                                padding = dip(10)
                                editTextTitle = editText {
                                    hint = "Passphrase"
                                }
                            }
                        }
                    }
                    positiveButton("OK") { }
                    negativeButton("Cancel") { }
                }.build().apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    setOnShowListener {
                        val b = getButton(AlertDialog.BUTTON_POSITIVE)
                        b.setOnClickListener {
                            callback(editTextTitle!!.text.toString())
//                            session.checkPassphrases(listOf(passphraseHash))?.let {pass ->
//                                session.storePassphrase(pass)
//                                callback(pass)
                            dismiss()
//                            } ?:run {
//                                launch {
//                                    editTextTitle!!.error = "You entered a wrong passphrase."
//                                    editTextTitle!!.requestFocus()
//                                }
//                            }
                        }
                    }

                }.show()
            }
        }

        promptPassphrase(reason.name) {
            completion(it)
        }

    }

    override fun parseIntentLcpl(uriString: String, networkAvailable: Boolean) {
        val uri: Uri? = Uri.parse(uriString)
        uri?.let {
            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()
            val bytes = URL(uri.toString()).openStream().readBytes()
            fulfill(bytes) {
                Timber.d(it.localURL)
                Timber.d(it.suggestedFilename)
                val file = File(it.localURL)
                launch {
                    val parser = EpubParser()
                    val pub = parser.parse(it.localURL)
                    pub?.let {
                        val pair = parser.parseEncryption(pub.container, pub.publication, pub.container.drm)
                        pub.container = pair.first
                        pub.publication = pair.second
                        prepareToServe(pub, file.name, file.absolutePath, true, true)
                        progress.dismiss()
                        catalogView.longSnackbar("publication added to your library")
                    }
                }
            }
        }
    }

    override fun prepareAndStartActivityWithLCP(drm: DRM, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication, networkAvailable: Boolean) {
        loadPublication(file.absolutePath, drm) {
            launch {

                if (it is Exception) {

                    catalogView.longSnackbar("${(it as LCPError).errorDescription}")

                } else {

                    prepareToServe(pub, book.fileName, file.absolutePath, false, true)
                    server.addEpub(publication, pub.container, "/" + book.fileName, applicationContext.getExternalFilesDir(null)?.path + "/styles/UserProperties.json")

                    this@CatalogActivity.startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath, "epubName" to book.fileName, "publication" to publication, "bookId" to book.id, "drm" to true))
                }
            }
        }
    }

    override fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog, networkAvailable: Boolean) {
        val bytes = contentResolver.openInputStream(uri)?.readBytes()
        bytes?.let {
            fulfill(bytes) {
                Timber.d(it.localURL)
                Timber.d(it.suggestedFilename)
                val file = File(it.localURL)
                launch {
                    val parser = EpubParser()
                    val pub = parser.parse(it.localURL)
                    pub?.let {
                        val pair = parser.parseEncryption(pub.container, pub.publication, pub.container.drm)
                        pub.container = pair.first
                        pub.publication = pair.second
                        prepareToServe(pub, file.name, file.absolutePath, true, true)
                        progress.dismiss()
                        catalogView.longSnackbar("publication added to your library")
                    }
                }
            }
        }
    }
}