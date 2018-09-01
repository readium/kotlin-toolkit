/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import com.mcxiaoke.koi.HASH
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.textInputLayout
import org.readium.r2.lcp.LcpHttpService
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpSession
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRMModel
import org.readium.r2.shared.drm.Drm
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import java.io.File
import java.net.URL

class CatalogActivity : LibraryActivity(), LcpFunctions {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = this
    }

//    Import a DRM license ( needs to be online )
//
//    An app which imports a DRM license will follow these steps (see the previous section for more details):
//
//    1/ Validate the license structure and check its profile identifier
//
//    2/ Get the passphrase associated with the license
//
//    3/ Validate the license integrity
//
//    4/ Check the license status
//
//    5/ Get an updated license if needed
//
//    6/ Fetch the encrypted publication
//
//    7/ Register the device / license
//
//    8/ Open the publication


//    Open a protected publication stored in the app catalog ( can work offline as well )
//
//    The process is a simpler than when the protected publication is imported, as some information about the license is stored in the database, especially the license identifier.
//
//    4/ Check the license status
//
//    5/ Get an updated license if needed
//
//    8/ Open the publication


    override fun parseIntentLcpl(uriString: String) {
        val uri: Uri? = Uri.parse(uriString)
        if (uri != null) {
            try {
                val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                progress.show()
                Thread {
                    try {
                        val bytes = URL(uri.toString()).openStream().readBytes()
                        lcpThread(bytes, progress)
                    } catch (e: Exception) {
                        e.localizedMessage?.let {
                            longSnackbar(catalogView, it)
                        } ?: run {
                            longSnackbar(catalogView, "An error occurred")
                        }
                        progress.dismiss()
                    }
                }.start()
            } catch (e: Exception) {
            }
        }
    }

    override fun prepareAndStartActivityWithLCP(drm: Drm, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication) {
        if (drm.brand == Drm.Brand.Lcp) {
            prepareToServe(pub, book.fileName, file.absolutePath, false, true)

            handleLcpPassphrase(publicationPath, drm, { drm1 ->
                val pair = parser.parseRemainingResource(pub.container, publication, drm1)
                pub.container = pair.first
                pub.publication = pair.second
            }, { drm2 ->
                if (supportedProfiles.contains(drm2.profile)) {
                    server.addEpub(publication, pub.container, "/" + book.fileName, applicationContext.getExternalFilesDir(null).path + "/styles/UserProperties.json")

                    val license = drm.license as LcpLicense
                    val drmModel = DRMModel(drm.brand.name,
                            license.archivePath!!)

                    startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath, "epubName" to book.fileName, "drmModel" to drmModel))
                } else {
                    alert(Appcompat, "The profile of this DRM is not supported.") {
                        negativeButton("Ok") { }
                    }.show()
                }
            }, {
                // Do nothing
            }).get()

        }
    }

    override fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog) {
        Thread {
            val bytes = contentResolver.openInputStream(uri).readBytes()
            lcpThread(bytes, progress)
        }.start()
    }

    private fun lcpThread(bytes: ByteArray, progress: ProgressDialog) {
        val lcpLicense = LcpLicense(bytes, this)
        task {
            lcpLicense.fetchStatusDocument().get()
        } then {
            try {
                lcpLicense.checkStatus()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lcpLicense.updateLicenseDocument().get()
        } then {
            try {
                lcpLicense.areRightsValid()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lcpLicense.register()
            lcpLicense.fetchPublication()
        } then {publcationPath ->
            publcationPath?.let { path ->
                lcpLicense.moveLicense(path, bytes)
                publcationPath
            }?: run {
                null
            }
        } successUi { publcationPath ->
            publcationPath?.let {path ->
                val file = File(path)
                runOnUiThread {
                    val parser = EpubParser()
                    val pub = parser.parse(path)
                    if (pub != null) {
                        val pair = parser.parseRemainingResource(pub.container, pub.publication, pub.container.drm)
                        pub.container = pair.first
                        pub.publication = pair.second
                        prepareToServe(pub, file.name, file.absolutePath, true, true)
                        progress.dismiss()
                        handleLcpPassphrase(file.absolutePath, Drm(Drm.Brand.Lcp), {
                            // Do nothing
                        }, {
                            // Do nothing
                        }, {
                            // Do nothing
                        }).get()
                    }
                }
            }
            progress.dismiss()
        } fail { exception ->
            exception.printStackTrace()
            exception.localizedMessage?.let { message ->
                longSnackbar(catalogView, message)
            } ?: run {
                longSnackbar(catalogView, "An error occurred")
            }
            progress.dismiss()
        }
    }

    private fun handleLcpPassphrase(publicationPath: String, drm: Drm, parsingCallback: (drm: Drm) -> Unit, callback: (drm: Drm) -> Unit, callbackUI: () -> Unit): Promise<Unit, Exception> {
        val lcpHttpService = LcpHttpService()
        val session = LcpSession(publicationPath, this)

        fun validatePassphrase(passphraseHash: String): Promise<LcpLicense, Exception> {

            val preferences = getSharedPreferences("org.readium.r2.lcp", Context.MODE_PRIVATE)

            return task {
                try {
                    lcpHttpService.certificateRevocationList("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl").get()
                } catch (e: Exception) {
                    null
                }
            } then { pemCrtl ->
                if (pemCrtl != null) {
                    preferences.edit().putString("pemCrtl", pemCrtl).apply()
                    session.resolve(passphraseHash, pemCrtl).get()
                } else {
                    session.resolve(passphraseHash, preferences.getString("pemCrtl", "")).get()
                }
            } fail { exception ->
                exception.printStackTrace()
            }
        }

        fun promptPassphrase(reason: String? = null, callback: (pass: String) -> Unit) {
            runOnUiThread {
                val hint = session.getHint()
                alert(Appcompat, hint, reason ?: "LCP Passphrase") {
                    var editText: EditText? = null
                    customView {
                        verticalLayout {
                            textInputLayout {
                                editText = editText { }
                            }
                        }
                    }
                    positiveButton("OK") {
                        task {
                            editText!!.text.toString()
                        } then { clearPassphrase ->
                            val passphraseHash = HASH.sha256(clearPassphrase)
                            session.checkPassphrases(listOf(passphraseHash))
                        } then { validPassphraseHash ->
                            session.storePassphrase(validPassphraseHash)
                            callback(validPassphraseHash)
                        } fail { exception ->
                            exception.printStackTrace()
                        }
                    }
                    negativeButton("Cancel") { }
                }.show()
            }
        }

        return task {
            val passphrases = session.passphraseFromDb()
            passphrases?.let { passphraseHash ->
                val lcpLicense = validatePassphrase(passphraseHash).get()
                drm.license = lcpLicense
                drm.profile = session.getProfile()
                parsingCallback(drm)
                callback(drm)
            } ?: run {
                promptPassphrase(null) { passphraseHash ->
                    val lcpLicense = validatePassphrase(passphraseHash).get()
                    drm.license = lcpLicense
                    drm.profile = session.getProfile()
                    parsingCallback(drm)
                    callback(drm)
                    callbackUI()
                }
            }
        }
    }

}