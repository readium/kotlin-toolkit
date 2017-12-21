package org.readium.r2.lcp

import java.net.URL

class LcpSession(url: URL) {

    val lcpLicense: LcpLicense

    init {
        lcpLicense = LcpLicense(url, true)
    }

    suspend fun resolve(passphrase: String, pemCrl: String) : LcpLicense {
        lcpLicense.fetchStatusDocument()
        lcpLicense.checkStatus()
        //lcpLicense.updateLicenseDocument()
        lcpLicense.areRightsValid()
        lcpLicense.register()
        return getLcpContext(lcpLicense.license.json.toString(), passphrase, pemCrl)
    }

    suspend fun getLcpContext(jsonLicense: String, passphrase: String, pemCrl: String) : LcpLicense {
        lcpLicense.context = Lcp().createContext(jsonLicense, passphrase, pemCrl)
        return lcpLicense
    }

    fun getHint() : String {
        return lcpLicense.license.getHint()
    }

    fun getProfile() : String {
        return lcpLicense.license.encryption.profile.toString()
    }

    suspend fun checkPassphrases(passphrases: List<String>) : String {
        return Lcp().findOneValidPassphrase(lcpLicense.license.json.toString(), passphrases.toTypedArray())
    }

    suspend fun passphraseFromDb() : String? {
        val passphrases: List<String>
        val db = LCPDatabase().shared
        passphrases = mutableListOf<String>()//db.transactions.possiblePasshprases(lcpLicense.license.id, lcpLicense.license.user.id)
        if (passphrases.isEmpty())
            return null
        return checkPassphrases(passphrases)
    }

    fun storePassphrase(passphraseHash: String) {
/*        LCPDatabase().shared.transactions.add(
                lcpLicense.license.id,
                lcpLicense.license.provider.toString(),
                lcpLicense.license.user.id,
                passphraseHash)
                */
    }

    suspend fun validateLicense() {
        //TODO JSON Schema or something
        return
    }

}