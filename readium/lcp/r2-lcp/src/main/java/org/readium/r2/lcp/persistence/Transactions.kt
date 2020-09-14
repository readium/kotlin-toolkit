/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.persistence

import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select
import org.readium.r2.lcp.service.PassphrasesRepository

internal object TransactionsTable {
    const val NAME = "Transactions"
    const val ID = "id"
    const val ORIGIN = "origin"
    const val USERID = "userId"
    const val PASSPHRASE = "passphrase"
}

internal class Transactions(var database: LcpDatabaseOpenHelper) : PassphrasesRepository {

    override fun passphrase(licenseId: String): String? {
        return database.use {
            select(TransactionsTable.NAME, TransactionsTable.PASSPHRASE)
                    .whereArgs("${TransactionsTable.ORIGIN} = {id}", "id" to licenseId)
                    .exec {
                        val parser = rowParser { result: String ->
                            return@rowParser result
                        }
                        if (parseList(parser).isEmpty()) {
                            null
                        } else {
                            parseList(parser)[0]
                        }
                    }
        }
    }

    override fun passphrases(userId: String): List<String> {
        return database.use {
            select(TransactionsTable.NAME, TransactionsTable.PASSPHRASE)
                    .whereArgs("${TransactionsTable.USERID} = {userId}", "userId" to userId)
                    .exec {
                        val parser = rowParser { result: String ->
                            return@rowParser result
                        }
                        parseList(parser)
                    }
        }
    }

    override fun allPassphrases(): List<String> =
        database.use {
            select(TransactionsTable.NAME, TransactionsTable.PASSPHRASE)
                .exec {
                    val parser = rowParser { result: String ->
                        return@rowParser result
                    }
                    parseList(parser)
                }
        }

    override fun addPassphrase(passphraseHash: String, licenseId: String?, provider: String?, userId: String?) {
        database.use {
            insert(TransactionsTable.NAME,
                    TransactionsTable.ID to licenseId,
                    TransactionsTable.ORIGIN to provider,
                    TransactionsTable.USERID to userId,
                    TransactionsTable.PASSPHRASE to passphraseHash)
        }
    }

}


