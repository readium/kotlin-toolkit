package org.readium.r2.lcp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import org.readium.r2.lcp.Tables.Licenses
import org.readium.r2.lcp.Tables.LicensesTable
import org.readium.r2.lcp.Tables.Transactions


// Access property for Context
val Context.database: LCPDatabaseOpenHelper
    get() = LCPDatabaseOpenHelper.getInstance(getApplicationContext())

val Context.appContext: Context
    get() = getApplicationContext()

//db.createTable("Licenses", true,
//"id" to INTEGER + PRIMARY_KEY + UNIQUE,
//"PRINTSLEFT" to INTEGER,
//"COPIESLEFT" to INTEGER,
//"PROVIDER" to TEXT,
//"ISSUED" to TEXT,
//"UPDATED" to TEXT,
//"END" to TEXT,
//"STATE" to TEXT)


class LCPDatabase(context: Context) {

    val shared = LCPDatabaseOpenHelper(context)

    /// Connection.
//    lateinit val connection: Connection
    /// Tables.
    var licenses: Licenses
    var transactions: Transactions

    init {
//        val database: LCPDatabaseOpenHelper =  LCPDatabaseOpenHelper.getInstance(context)
        licenses = Licenses(shared)
        transactions = Transactions(shared)
    }
}



class LCPDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "lcpdatabase", null, 1) {
    companion object {
        private var instance: LCPDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): LCPDatabaseOpenHelper {
            if (instance == null) {
                instance = LCPDatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        db.createTable("Licenses", true,
                LicensesTable.ID to TEXT + PRIMARY_KEY + UNIQUE,
                LicensesTable.PRINTSLEFT to INTEGER,
                LicensesTable.COPIESLEFT to INTEGER,
                LicensesTable.PROVIDER to TEXT,
                LicensesTable.ISSUED to TEXT,
                LicensesTable.UPDATED to TEXT,
                LicensesTable.END to TEXT,
                LicensesTable.STATE to TEXT)

//        let id = Expression<String>("id")
//        let PRINTSLEFT = Expression<Int?>("PRINTSLEFT")
//        let COPIESLEFT = Expression<Int?>("COPIESLEFT")
//        let PROVIDER = Expression<String>("PROVIDER")
//        let ISSUED = Expression<Date>("ISSUED")
//        let UPDATED = Expression<Date?>("UPDATED")
//        let END = Expression<Date?>("END")
//        let STATE = Expression<String?>("STATE")


        db.createTable("Transactions", true,
                "licenseId" to TEXT,
                "origin" to TEXT,
                "userId" to TEXT,
                "passphrase" to TEXT)
//        let transactions = Table("Transactions")
//        /// Fields.
//        let licenseId = Expression<String>("licenseId")
//        let origin = Expression<String>("origin")
//        let userId = Expression<String?>("userId")
//        let passphrase = Expression<String>("passphrase") // hashed.

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
    }
}
