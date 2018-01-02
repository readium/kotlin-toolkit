package org.readium.r2.lcp.Tables

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.sql.Date

class Licenses(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        val DATABASE_VERSION = 1
        val TABLE_NAME = "lcpdatabase"
        val id = "_id"
        val printsLeft = "printsLeft"
        val copiesLeft = "copiesLeft"
        val provider = "provider"
        val issued = "issued"
        val updated = "updated"
        val end = "end"
        val state = "state"

        private val SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME +
                        " (_id INTEGER PRIMARY KEY," +
                        "$printsLeft INTEGER," +
                        "$copiesLeft INTEGER," +
                        "$provider TEXT," +
                        "$issued DATE," +
                        "$updated DATE," +
                        "$end DATE," +
                        "$state TEXT);"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME;")
        onCreate(db)
    }

    fun dateOfLastUpdate(id: String) : Date? {
        return null
    }

    fun existingLicenes(id: String) : Boolean {
        return true
    }

}