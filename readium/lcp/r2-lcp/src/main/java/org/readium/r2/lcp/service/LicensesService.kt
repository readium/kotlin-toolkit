/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.license.License
import org.readium.r2.lcp.license.LicenseValidation
import org.readium.r2.lcp.license.container.EPUBLicenseContainer
import org.readium.r2.lcp.license.container.LCPLLicenseContainer
import org.readium.r2.lcp.license.container.LicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.public.*
import timber.log.Timber
import java.net.URL


class LicensesService(private val licenses: LicensesRepository,
                            private val crl: CRLService,
                            private val device: DeviceService,
                            private val network: NetworkService,
                            private val passphrases: PassphrasesService,
                            private val context: android.content.Context):LCPService {


//    fun fetchPublication(): String? {
//        Timber.i("LCP fetchPublication")
//        val publicationLink = license.link("publication")
//        publicationLink?.let {
//            return lcpHttpService.publicationUrl(androidContext, publicationLink.href.toString()).get()
//        }
//        return null
//    }

    override fun importPublication(lcpl: URL, authentication: LCPAuthenticating?, completion: (LCPImportedPublication?, LCPError?) -> Unit) = runBlocking {
//        val progress = MutableObservable<DownloadProgress>(.infinite)
        val container = LCPLLicenseContainer(lcpl = lcpl)
        retrieveLicense(container, authentication) { license ->


            license?.fetchPublication(context)?.success {
                val publication = LCPImportedPublication(localURL = it, suggestedFilename = "${license.license.id}.epub")
                completion(publication, null)
            }?.fail {
                completion(null, LCPError.network(Error(it)))
            }


//            val downloadProgress = license.fetchPublication { result, error  ->
////                progress.value = .infinite
//                val result = result
//                if (result != null) {
//                    val publication = LCPImportedPublication(localURL = result.0, downloadTask = result.1, suggestedFilename = "${license.license.id}.epub")
//                    completion(publication, null)
//                } else {
//                    completion(null, error)
//                }
//            }
//            // Forwards the download progress to the global import progress
//            downloadProgress.observe(progress)
        }
//                resolve(LCPError.wrap(completion))
//        return progress
    }

    override fun retrieveLicense(publication: String, authentication: LCPAuthenticating?, completion: (LCPLicense?, LCPError?) -> Unit) = runBlocking {
        val container = EPUBLicenseContainer(epub = publication)
        retrieveLicense(container, authentication) { license ->


            Timber.d("license retrieved ${license?.license}")


            completion(license, null)
//            LCPError.wrap(completion)
//            completion(it)
//            completion(null, null)
        }
    }

    fun retrieveLicense(container: LicenseContainer, authentication: LCPAuthenticating?, completion: (License?) -> Unit) {

                var initialData = container.read()
                Timber.d("license ${LicenseDocument(data = initialData).json}")

                val validation = LicenseValidation(authentication = authentication, crl = this.crl,
                        device = this.device, network = this.network, passphrases = this.passphrases) { licenseDocument ->
                    try {
                        this.licenses.addLicense(licenseDocument)
                    } catch (error:Error) {
                        Timber.d("Failed to add the LCP License to the local database: ${error}")
                    }
//                    if (!licenseDocument.data.contentEquals(initialData)) {
                        try {
                            container.write(licenseDocument)
                            Timber.d("licenseDocument ${licenseDocument.json}")

                            initialData = container.read()
                            Timber.d("license ${LicenseDocument(data = initialData).json}")

                            Timber.d("Wrote updated License Document in container")
                        } catch (error:Error) {
                            Timber.d("Failed to write updated License Document in container: ${error}")
                        }
//                    }

                }
                validation.validate(LicenseValidation.Document.license(initialData)) { documents, error  ->
                    documents?.let {
                        Timber.d("validated documents $it")
                        documents.getContext()
                    }

                    completion(documents?.let { License(documents = it, validation = validation, licenses = this.licenses, device = this.device, network = this.network) })

                }

    }
}








//
//
//final class LicensesService: Loggable {
//    private val licenses: LicensesRepository
//    private val crl: CRLService
//    private val device: DeviceService
//    private val network: NetworkService
//    private val passphrases: PassphrasesService
//
//    constructor(licenses: LicensesRepository, crl: CRLService, device: DeviceService, network: NetworkService, passphrases: PassphrasesService) {
//        this.licenses = licenses
//        this.crl = crl
//        this.device = device
//        this.network = network
//        this.passphrases = passphrases
//    }
//
//    private fun retrieveLicense(container: LicenseContainer, authentication: LCPAuthenticating?) : Deferred<License> {
//        return Deferred {
//            val initialData = container.read()
//
//            fun onLicenseValidated(license: LicenseDocument) {
//                do {
//                    this.licenses.addLicense(license)
//                } catch {
//                    this.log(.error, "Failed to add the LCP License to the local database: ${error}")
//                }
//                if (license.data != initialData) {
//                    do {
//                        container.write(license)
//                        this.log(.debug, "Wrote updated License Document in container")
//                    } catch {
//                        this.log(.error, "Failed to write updated License Document in container: ${error}")
//                    }
//                }
//            }
//            val validation = LicenseValidation(authentication = authentication, crl = this.crl, device = this.device, network = this.network, passphrases = this.passphrases, onLicenseValidated = onLicenseValidated)
//            return validation.validate(.license(initialData)).map { documents  ->
//            _ = documents.getContext()
//            return License(documents = documents, validation = validation, licenses = this.licenses, device = this.device, network = this.network)
//        }
//        }
//    }
//}
////FIXME: @SwiftKotlin - Kotlin does not support inheritance clauses in extensions:  : LCPService
//
//fun LicensesService.importPublication(lcpl: URL, authentication: LCPAuthenticating?, completion: (LCPImportedPublication?, LCPError?) -> Unit) : Observable<DownloadProgress> {
//    val progress = MutableObservable<DownloadProgress>(.infinite)
//    val container = LCPLLicenseContainer(lcpl = lcpl)
//    retrieveLicense(from = container, authentication = authentication).asyncMap { license, completion  ->
//        val downloadProgress = license.fetchPublication { result, error  ->
//            progress.value = .infinite
//            val result = result
//            if (result != null) {
//                val publication = LCPImportedPublication(localURL = result.0, downloadTask = result.1, suggestedFilename = "${license.license.id}.epub")
//                completion(publication, null)
//            } else {
//                completion(null, error)
//            }
//        }
//        downloadProgress.observe(progress)
//    }.resolve(LCPError.wrap(completion))
//    return progress
//}
//
//fun LicensesService.retrieveLicense(publication: URL, authentication: LCPAuthenticating?, completion: (LCPLicense?, LCPError?) -> Unit) {
//    val container = EPUBLicenseContainer(epub = publication)
//    retrieveLicense(from = container, authentication = authentication).resolve(LCPError.wrap(completion))
//}
