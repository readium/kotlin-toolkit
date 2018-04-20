package org.readium.r2.lcp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import org.readium.r2.lcp.Tables.Licenses
import org.readium.r2.lcp.Tables.LicensesTable
import org.readium.r2.lcp.Tables.Transactions
import org.readium.r2.lcp.Tables.TransactionsTable


// Access property for Context
val Context.database: LCPDatabaseOpenHelper
    get() = LCPDatabaseOpenHelper.getInstance(getApplicationContext())

val Context.appContext: Context
    get() = getApplicationContext()


class LCPDatabase(context: Context) {

    val shared = LCPDatabaseOpenHelper(context)
    var licenses: Licenses
    var transactions: Transactions

    init {
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

        db.createTable("Licenses", true,
                LicensesTable.ID to TEXT + PRIMARY_KEY + UNIQUE,
                LicensesTable.PRINTSLEFT to INTEGER,
                LicensesTable.COPIESLEFT to INTEGER,
                LicensesTable.PROVIDER to TEXT,
                LicensesTable.ISSUED to TEXT,
                LicensesTable.UPDATED to TEXT,
                LicensesTable.END to TEXT,
                LicensesTable.STATE to TEXT)

        db.createTable("Transactions", true,
                TransactionsTable.ID to TEXT,
                TransactionsTable.ORIGIN to TEXT,
                TransactionsTable.USERID to TEXT,
                TransactionsTable.PASSPHRASE to TEXT)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
    }
}
