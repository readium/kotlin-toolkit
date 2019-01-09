/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import android.content.Context
import kotlinx.coroutines.runBlocking
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.readium.lcp.sdk.Lcp
import org.readium.r2.lcp.model.documents.StatusDocument
import java.io.File

class LcpSession {

    val lcpLicense: LcpLicense
    val androidContext:Context
    val database:LcpDatabase

    constructor(file: String, context:Context) {
        androidContext = context
        database = LcpDatabase(androidContext)
        lcpLicense = LcpLicense(File(file).toURI().toURL(), true, androidContext )
    }

    fun resolve(passphrase: String, pemCrl: String) : Promise<Any, Exception> {
         return runBlocking {
             lcpLicense.resolve()
             val lcpContext = getLcpContext(lcpLicense.license.json.toString(), passphrase, pemCrl).get()
              Promise.of(lcpContext)
         }
    }

    fun getLcpContext(jsonLicense: String, passphrase: String, pemCrl: String) : Promise<Any, Exception> {
        return Promise.of(
            lcpLicense.status?.let {statusDocument ->
                if ((statusDocument.status == StatusDocument.Status.active) || (statusDocument.status == StatusDocument.Status.ready)) {

                    lcpLicense.context = Lcp().createContext(jsonLicense, passphrase, pemCrl)
                    lcpLicense
                } else {
                    statusDocument.status.toString()
                }
            } ?: run {
                database.licenses.getStatus(lcpLicense.license.id)?.let {licenseStatus ->
                    if ( (licenseStatus == StatusDocument.Status.active.toString()) || (licenseStatus == StatusDocument.Status.ready.toString()) ) {

                        lcpLicense.context = Lcp().createContext(jsonLicense, passphrase, pemCrl)
                        lcpLicense
                    } else {
                        licenseStatus
                    }
                } ?: run {
                    "invalid"
                }
            }
        )
    }

    fun getHint() : String {
        return lcpLicense.license.getHint()
    }

    fun getProfile() : String {
        return lcpLicense.license.encryption.profile.toString()
    }

    fun checkPassphrases(passphrases: List<String>) : String? {
        try {
            return Lcp().findOneValidPassphrase(lcpLicense.license.json.toString(), passphrases.toTypedArray())
        } catch(e: Exception) {
            return null
        }
    }

    fun passphraseFromDb() : String? {
        val passphrases: List<String> = database.transactions.possiblePasshprases(lcpLicense.license.id, lcpLicense.license.user?.id)
        if (passphrases.isEmpty())
            return null
        return checkPassphrases(passphrases)
    }

    fun storePassphrase(passphraseHash: String) {
        database.transactions.add(lcpLicense, passphraseHash)
    }

    fun validateLicense() {
        //TODO JSON Schema or something
    }
}