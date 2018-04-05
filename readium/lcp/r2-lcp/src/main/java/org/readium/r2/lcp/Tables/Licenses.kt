package org.readium.r2.lcp.Tables

import android.util.Log
import org.jetbrains.anko.db.*
import org.readium.r2.lcp.LCPDatabaseOpenHelper
import org.readium.r2.lcp.Model.Documents.LicenseDocument
import java.sql.Date

object LicensesTable {
    val NAME = "Licenses"
    val ID = "_id"
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
                            val state = columns.getValue(LicensesTable.STATE) as String?
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

    fun updateState(id: String, state: String)  {
        database.use {
            update(LicensesTable.NAME,LicensesTable.STATE to state).whereArgs("(_id = {_id})",
                    "_id" to id)
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
                    LicensesTable.ID to license.id ,
                    LicensesTable.PRINTSLEFT to license.rights.print,
                    LicensesTable.COPIESLEFT to license.rights.copy,
                    LicensesTable.PROVIDER to license.provider.toString(),
                    LicensesTable.ISSUED to license.issued,
                    LicensesTable.UPDATED to license.updated,
                    LicensesTable.END to license.rights.end,
                    LicensesTable.STATE to status)

        }
    }


    /// Check if the table already contains an entry for the given ID.
    ///
    /// - Parameter id: The ID to check for.
    /// - Returns: A boolean indicating the result of the search, true if found.
    fun existingLicense(id: String): Boolean {
        return database.use {
            select(LicensesTable.NAME, "count(_id)").whereArgs("(_id = {_id})",
                    "_id" to id).exec {
                val parser = rowParser { count: Long ->
                    Log.i("count", count.toString())
                    return@rowParser count > 0
                }
                parseList(parser)[0]
            }
        }
    }

//        val query = licenses.select(UPDATED).order(UPDATED.desc)
//
//        do {
//            for result in try db.prepare(query) {
//                do {
//                    return try result.get(UPDATED)
//                    } catch {
//                        return nil
//                    }
//                }
//            } catch {
//                return nil
//            }
//            return nil
//        }
//
//        internal func updateState(forLicenseWith id: String, to STATE: String) throws {
//            let db = LCPDatabase.shared.connection
//                    let license = licenses.filter(self.id == id)
//
//            // Check if empty.
//            guard try db.scalar(license.count) > 0 else {
//            throw LcpError.licenseNotFound
//        }
//            try db.run(license.update(self.STATE <- STATE))
//            }
//
//        /// Check if the table already contains an entry for the given ID.
//        ///
//        /// - Parameter id: The ID to check for.
//        /// - Returns: A boolean indicating the result of the search, true if found.
//        /// - Throws: .
//        internal func existingLicense(with id: String) throws -> Bool {
//            let db = LCPDatabase.shared.connection
//                    // Check if empty.
//                    guard try db.scalar(licenses.count) > 0 else {
//            return false
//        }
//            let query = licenses.filter(self.id == id)
//            let count = try db.scalar(query.count)
//
//                return count == 1
//            }
//
//        /// Add a registered license to the database.
//        ///
//        /// - Parameters:
//        ///   - license: <#license description#>
//        ///   - status: <#status description#>
//        /// - Throws: <#throws value description#>
//        internal func insert(_ license: LicenseDocument, with status: StatusDocument.Status?) throws {
//            let db = LCPDatabase.shared.connection
//
//                    let insertQuery = licenses.insert(
//                    id <- license.id,
//            PRINTSLEFT <- license.rights.print,
//            COPIESLEFT <- license.rights.copy,
//            PROVIDER <- license.PROVIDER.absoluteString,
//            ISSUED <- license.ISSUED,
//            UPDATED <- license.UPDATED,
//            END <- license.rights.END,
//            STATE <- status?.rawValue ?? nil
//            )
//            try db.run(insertQuery)
//            }

}

//class Licenses(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {
//
//    companion object {
//        val DATABASE_VERSION = 1
//        val TABLE_NAME = "lcpdatabase"
//        val id = "_id"
//        val PRINTSLEFT = "PRINTSLEFT"
//        val COPIESLEFT = "COPIESLEFT"
//        val PROVIDER = "PROVIDER"
//        val ISSUED = "ISSUED"
//        val UPDATED = "UPDATED"
//        val END = "END"
//        val STATE = "STATE"
//
//        private val SQL_CREATE_ENTRIES =
//                "CREATE TABLE " + TABLE_NAME +
//                        " (_id INTEGER PRIMARY KEY," +
//                        "$PRINTSLEFT INTEGER," +
//                        "$COPIESLEFT INTEGER," +
//                        "$PROVIDER TEXT," +
//                        "$ISSUED DATE," +
//                        "$UPDATED DATE," +
//                        "$END DATE," +
//                        "$STATE TEXT);"
//    }
//
//    override fun onCreate(db: SQLiteDatabase) {
//        db.execSQL(SQL_CREATE_ENTRIES)
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
//        db ?: return
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME;")
//        onCreate(db)
//    }
//
//    fun dateOfLastUpdate(id: String) : Date? {
//        return null
//    }
//
//    fun existingLicenes(id: String) : Boolean {
//        return true
//    }
//
//}