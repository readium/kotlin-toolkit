/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import android.content.Context
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.readium.lcp.sdk.Lcp
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

    fun resolve(passphrase: String, pemCrl: String) : Promise<LcpLicense, Exception> {
        return task {
            lcpLicense.fetchStatusDocument().get()
        } then {
            lcpLicense.checkStatus()
            lcpLicense.updateLicenseDocument().get()
        } then {
            lcpLicense.areRightsValid()
            lcpLicense.register()
            getLcpContext(lcpLicense.license.json.toString(), passphrase, pemCrl).get()
        }
    }

    fun getLcpContext(jsonLicense: String, passphrase: String, pemCrl: String) : Promise<LcpLicense, Exception> {
        return task {
            lcpLicense.context = Lcp().createContext(jsonLicense, passphrase, pemCrl)
            lcpLicense
        }
    }

    fun getHint() : String {
        return lcpLicense.license.getHint()
    }

    fun getProfile() : String {
        return lcpLicense.license.encryption.profile.toString()
    }

    fun checkPassphrases(passphrases: List<String>) : String {
        return Lcp().findOneValidPassphrase(lcpLicense.license.json.toString(), passphrases.toTypedArray())
    }

    fun passphraseFromDb() : String? {
        val passphrases: List<String>
        passphrases = database.transactions.possiblePasshprases(lcpLicense.license.id, lcpLicense.license.user.id)
        if (passphrases.isEmpty())
            return null
        return checkPassphrases(passphrases)
    }

    fun storePassphrase(passphraseHash: String) {
        database.transactions.add(lcpLicense, passphraseHash)
    }

    fun validateLicense() :Promise<Unit, Exception> {
        //TODO JSON Schema or something
        return task {}
    }
}