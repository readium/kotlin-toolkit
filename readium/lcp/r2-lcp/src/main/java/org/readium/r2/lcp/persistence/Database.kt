/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.INTEGER
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.TEXT
import org.jetbrains.anko.db.createTable


// Access property for Context
internal val Context.database: LcpDatabaseOpenHelper
    get() = LcpDatabaseOpenHelper.getInstance(applicationContext)

internal val Context.appContext: Context
    get() = applicationContext


internal class Database(context: Context) {

    val shared: LcpDatabaseOpenHelper = LcpDatabaseOpenHelper(context)
    var licenses: Licenses
    var transactions: Transactions

    init {
        licenses = Licenses(shared)
        transactions = Transactions(shared)
    }

}

internal class LcpDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "lcpdatabase", null, 1) {
    companion object {
        private var instance: LcpDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): LcpDatabaseOpenHelper {
            if (instance == null) {
                instance = LcpDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(LicensesTable.NAME, true,
                LicensesTable.ID to TEXT,
                LicensesTable.PRINTSLEFT to INTEGER,
                LicensesTable.COPIESLEFT to INTEGER,
                LicensesTable.REGISTERED to INTEGER)

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
