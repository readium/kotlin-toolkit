package org.readium.r2.lcp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LcpDataBaseHandler(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        val DATABASE_VERSION = 1

        private val SQL_CREATE_ENTRIES =
                "CREATE TABLE lcpdatabase.sqlite (" +
                        "id TEXT PRIMARY KEY," +
                        "printsLeft INT," +
                        "copiesLeft INT," +
                        "provider TEXT," +
                        "issued DATE," +
                        "updated DATE," +
                        "end DATE," +
                        "state TEXT)"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}
