/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.tables

import org.jetbrains.anko.db.*
import org.joda.time.DateTime
import org.readium.r2.lcp.LcpDatabaseOpenHelper
import org.readium.r2.lcp.model.documents.LicenseDocument
import timber.log.Timber

object LicensesTable {
    const val NAME = "Licenses"
    const val ID = "id"
    const val PRINTSLEFT = "printsLeft"
    const val COPIESLEFT = "copiesLeft"
    const val PROVIDER = "provider"
    const val ISSUED = "issued"
    const val UPDATED = "updated"
    const val END = "end"
    const val STATE = "state"
}

class Licenses(var database: LcpDatabaseOpenHelper) {

    fun dateOfLastUpdate(id: String): DateTime? {
        val lastUpdated = database.use {
            return@use select(LicensesTable.NAME)
                    .whereArgs("${LicensesTable.ID} = {id}", "id" to id)
                    .limit(1)
                    .orderBy(LicensesTable.UPDATED,SqlOrderDirection.DESC)
                    .parseOpt(object : MapRowParser<String> {
                        override fun parseRow(columns: Map<String, Any?>): String {
                            val updated = columns.getValue(LicensesTable.UPDATED) as String?
                            if (updated != null) {
                                return updated
                            }
                            return String()
                        }
                    })
        }

        if (!lastUpdated.isNullOrEmpty()) {
            return DateTime(lastUpdated)
        }
        return null
    }

    fun dateOfEnd(id: String): DateTime? {
        val endDate = database.use {
            return@use select(LicensesTable.NAME)
                    .whereArgs("${LicensesTable.ID} = {id}", "id" to id)
                    .limit(1)
                    .orderBy(LicensesTable.UPDATED,SqlOrderDirection.DESC)
                    .parseOpt(object : MapRowParser<String> {
                        override fun parseRow(columns: Map<String, Any?>): String {
                            val end = columns.getValue(LicensesTable.END) as String?
                            if (end != null) {
                                return end
                            }
                            return String()
                        }
                    })
        }

        if (!endDate.isNullOrEmpty()) {
            return DateTime(endDate)
        }
        return null
    }

    fun updateState(id: String, state: String) {
        database.use {
            update(LicensesTable.NAME, LicensesTable.STATE to state).whereArgs("${LicensesTable.ID} = {id}",
                    "id" to id)
                    .exec()
        }
    }

    fun getStatus(id: String): String? {
        return database.use {
            return@use select(LicensesTable.NAME,
                    LicensesTable.STATE)
                    .whereArgs("${LicensesTable.ID} = {id}", "id" to id)
                    .limit(1)
                    .parseOpt(object : MapRowParser<String> {
                        override fun parseRow(columns: Map<String, Any?>): String {
                            val status = columns.getValue(LicensesTable.STATE) as String?
                            if (status != null) {
                                return status
                            }
                            return String()
                        }
                    })
        }
    }

    /// Check if the table already contains an entry for the given ID.
    ///
    /// - Parameter id: The ID to check for.
    /// - Returns: A boolean indicating the result of the search, true if found.
    fun existingLicense(id: String): Boolean {
        return database.use {
            select(LicensesTable.NAME, "count(${LicensesTable.ID})").whereArgs("(${LicensesTable.ID} = {id})",
                    "id" to id).exec {
                val parser = rowParser { count: Int ->
                    Timber.i("count %s", count.toString())
                    return@rowParser count == 1
                }
                parseList(parser)[0]
            }
        }
    }

    /// Add/update a registered license to/in the database.
    ///
    /// - Parameters:
    ///   - license: <#license description#>
    ///   - status: <#status description#>
    fun updateLicense(license: LicenseDocument, status: String) {
        if (existingLicense(license.id)) {
            database.use {
                update(LicensesTable.NAME,
                        LicensesTable.PRINTSLEFT to license.rights?.print,
                        LicensesTable.COPIESLEFT to license.rights?.copy,
                        LicensesTable.PROVIDER to license.provider.toString(),
                        LicensesTable.ISSUED to license.issued.toString(),
                        LicensesTable.UPDATED to license.updated?.toString(),
                        LicensesTable.END to license.rights?.end?.toString(),
                        LicensesTable.STATE to status)
                        .whereArgs("${LicensesTable.ID} = {id}", "id" to license.id)
                        .exec()
            }
        } else {
            database.use {
                insert(LicensesTable.NAME,
                        LicensesTable.ID to license.id,
                        LicensesTable.PRINTSLEFT to license.rights?.print,
                        LicensesTable.COPIESLEFT to license.rights?.copy,
                        LicensesTable.PROVIDER to license.provider.toString(),
                        LicensesTable.ISSUED to license.issued.toString(),
                        LicensesTable.UPDATED to license.issued.toString(),
                        LicensesTable.END to license.rights?.end?.toString(),
                        LicensesTable.STATE to status)
            }
        }
    }
}