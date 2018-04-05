package org.readium.r2.lcp

import android.content.Context
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import java.lang.Exception
import java.net.URL

class LcpSession {

    val lcpLicense: LcpLicense
    val androidContext:Context

    constructor(url: URL, context:Context) {

        androidContext = context
        lcpLicense = LcpLicense(url, true, androidContext )

    }

//    init {
//        lcpLicense = LcpLicense(url, true, androidContext )
//    }

    // TODO : incomplete
    fun resolve(passphrase: String, pemCrl: String) : LcpLicense {

        val promise = task {
            lcpLicense.fetchStatusDocument()
        } then {
            lcpLicense.checkStatus()
            lcpLicense.updateLicenseDocument()
        } then {
            lcpLicense.areRightsValid()
            lcpLicense.register()
            getLcpContext(lcpLicense.license.json.toString(), passphrase, pemCrl)
        }
        return promise.get()


    }

    // TODO : incomplete
    fun getLcpContext(jsonLicense: String, passphrase: String, pemCrl: String) : LcpLicense {
        lcpLicense.context = Lcp().createContext(jsonLicense, passphrase, pemCrl)
        return lcpLicense
    }

    fun getHint() : String {
        return lcpLicense.license.getHint()
    }

    fun getProfile() : String {
        return lcpLicense.license.encryption.profile.toString()
    }

    // TODO : incomplete
    fun checkPassphrases(passphrases: List<String>) : String {
        return Lcp().findOneValidPassphrase(lcpLicense.license.json.toString(), passphrases.toTypedArray())
    }

    // TODO : incomplete
    fun passphraseFromDb() : String? {
        val passphrases: List<String>
//        val db = LCPDatabase().shared
        passphrases = mutableListOf<String>()//db.transactions.possiblePasshprases(lcpLicense.license.id, lcpLicense.license.user.id)
        if (passphrases.isEmpty())
            return null
        return checkPassphrases(passphrases)
    }

    // TODO : incomplete
    fun storePassphrase(passphraseHash: String) {
/*        LCPDatabase().shared.transactions.add(
                lcpLicense.license.id,
                lcpLicense.license.PROVIDER.toString(),
                lcpLicense.license.user.id,
                passphraseHash)
                */
    }

    // TODO : incomplete
    fun validateLicense() {
        //TODO JSON Schema or something
        return
    }

}