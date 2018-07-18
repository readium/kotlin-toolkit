/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.tables

import org.jetbrains.anko.db.*
import org.readium.r2.lcp.LcpDatabaseOpenHelper
import org.readium.r2.lcp.LcpLicense

object TransactionsTable {
    val NAME = "Transactions"
    val ID = "licenseId"
    val ORIGIN = "origin"
    val USERID = "userId"
    val PASSPHRASE = "passphrase"
}

class Transactions(var database: LcpDatabaseOpenHelper) {

    fun add(license: LcpLicense, passphraseHash: String) {
        database.use {
            insert(TransactionsTable.NAME,
                    TransactionsTable.ID to license.license.id ,
                    TransactionsTable.ORIGIN to license.license.provider.toString(),
                    TransactionsTable.USERID to license.license.user.id,
                    TransactionsTable.PASSPHRASE to passphraseHash)
        }

    }

    /// Try to find the possible passphrases for the license/provider tuple.
    ///
    /// - Parameters:
    ///   - licenseId: <#licenseId description#>
    ///   - provider: <#provider description#>
    /// - Returns: <#return value description#>
    fun possiblePasshprases(licenseId: String, userId: String): List<String> {

        val possiblePassphrases = mutableListOf<String>()
        val licensePassphrase: String?
        val userIdPassphrases: List<String>

        licensePassphrase = passphrase(licenseId)
        userIdPassphrases = passphrases(userId)

        possiblePassphrases.addAll(userIdPassphrases)

        licensePassphrase?.let { pass ->
            possiblePassphrases.add(pass)
        }

        return possiblePassphrases
    }

    /// Returns the passphrase found for given license.
    ///
    /// - Parameter id: <#id description#>
    /// - Returns: <#return value description#>
    /// - Throws: <#throws value description#>
    fun passphrases(userId: String): List<String> {
        return database.use {
            select(TransactionsTable.NAME, TransactionsTable.PASSPHRASE )
                    .whereArgs("${TransactionsTable.USERID} = {userId}", "userId" to userId)
                    .exec {
                        val parser = rowParser {result:String ->
                            return@rowParser result
                        }
                        parseList(parser)
                    }
        }
    }
    
    /// Return a passphrases array found for the given provider.
    ///
    /// - Parameter provider: The book provider URL.
    /// - Returns: The passhrases found in DB for the given provider.
    fun passphrase(licenseId: String): String? {
        return database.use {
            select(TransactionsTable.NAME, TransactionsTable.PASSPHRASE )
                    .whereArgs("${TransactionsTable.ORIGIN} = {licenseId}", "licenseId" to licenseId)
                    .exec {
                        val parser = rowParser { result:String ->
                            return@rowParser result
                        }
                        if (parseList(parser).isEmpty()) {
                            null
                        }
                        else {
                            parseList(parser)[0]
                        }
                    }
        }

    }

}


