package org.readium.r2.lcp.Tables

import android.util.Log
import org.jetbrains.anko.db.*
import org.readium.r2.lcp.LCPDatabaseOpenHelper
import org.readium.r2.lcp.Model.Documents.LicenseDocument
import java.sql.Date

object LicensesTable {
    val NAME = "Licenses"
    val ID = "id"
    val PRINTSLEFT = "printsLeft"
    val COPIESLEFT = "copiesLeft"
    val PROVIDER = "provider"
    val ISSUED = "issued"
    val UPDATED = "updated"
    val END = "end"
    val STATE = "state"
}

class Licenses(var database: LCPDatabaseOpenHelper) {

    fun dateOfLastUpdate(id: String): String? {
        val lastUpdated = database.use {
            return@use select(LicensesTable.NAME)
                    .whereSimple("${LicensesTable.ID} = ?", id)
                    .parseOpt(object : MapRowParser<String> {
                        override fun parseRow(columns: Map<String, Any?>): String {
                            val updated = columns.getValue(LicensesTable.UPDATED) as String?
                            if (updated != null) {
                                return updated
                            } else {
                                return String()
                            }
                        }
                    })
        }
        if (!lastUpdated.isNullOrEmpty()) {
            return lastUpdated
        }
        return null
    }

    fun updateState(id: String, state: String) {
        database.use {
            update(LicensesTable.NAME, LicensesTable.STATE to state).whereArgs("${LicensesTable.ID} = {id}",
                    "id" to id)
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
                    Log.i("count", count.toString())
                    return@rowParser count == 1
                }
                parseList(parser)[0]
            }
        }
    }

    /// Add a registered license to the database.
    ///
    /// - Parameters:
    ///   - license: <#license description#>
    ///   - status: <#status description#>
    fun insert(license: LicenseDocument, status: String) {
        database.use {
            insert(LicensesTable.NAME,
                    LicensesTable.ID to license.id,
                    LicensesTable.PRINTSLEFT to license.rights.print,
                    LicensesTable.COPIESLEFT to license.rights.copy,
                    LicensesTable.PROVIDER to license.provider.toString(),
                    LicensesTable.ISSUED to license.issued,
                    LicensesTable.UPDATED to license.updated,
                    LicensesTable.END to license.rights.end,
                    LicensesTable.STATE to status)

        }
    }
}