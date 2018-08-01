/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import org.readium.r2.lcp.tables.Licenses
import org.readium.r2.lcp.tables.LicensesTable
import org.readium.r2.lcp.tables.Transactions
import org.readium.r2.lcp.tables.TransactionsTable


// Access property for Context
val Context.database: LcpDatabaseOpenHelper
    get() = LcpDatabaseOpenHelper.getInstance(getApplicationContext())

val Context.appContext: Context
    get() = getApplicationContext()


class LcpDatabase {

    val shared:LcpDatabaseOpenHelper
    var licenses: Licenses
    var transactions: Transactions

    constructor(context: Context) {
        shared = LcpDatabaseOpenHelper(context)
        licenses = Licenses(shared)
        transactions = Transactions(shared)
    }

}


class LcpDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "lcpdatabase", null, 1) {
    companion object {
        private var instance: LcpDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): LcpDatabaseOpenHelper {
            if (instance == null) {
                instance = LcpDatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(LicensesTable.NAME, true,
                LicensesTable.ID to TEXT,
                LicensesTable.PRINTSLEFT to INTEGER,
                LicensesTable.COPIESLEFT to INTEGER,
                LicensesTable.PROVIDER to TEXT,
                LicensesTable.ISSUED to TEXT,
                LicensesTable.UPDATED to TEXT,
                LicensesTable.END to TEXT,
                LicensesTable.STATE to TEXT)

        db.createTable(TransactionsTable.NAME, true,
                TransactionsTable.ID to TEXT,
                TransactionsTable.ORIGIN to TEXT,
                TransactionsTable.USERID to TEXT,
                TransactionsTable.PASSPHRASE to TEXT)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
    }
}
