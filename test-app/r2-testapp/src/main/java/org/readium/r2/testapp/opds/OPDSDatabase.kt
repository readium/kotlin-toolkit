/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.opds

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import java.io.Serializable


// Access property for Context
val Context.database: OPDSDatabaseOpenHelper
    get() = OPDSDatabaseOpenHelper.getInstance(applicationContext)

val Context.appContext: Context
    get() = applicationContext


class OPDSDatabase(context: Context) {

    val shared: OPDSDatabaseOpenHelper = OPDSDatabaseOpenHelper(context)
    var opds: OPDS

    init {
        opds = OPDS(shared)
    }

}

data class OPDSModel(var title: String, var href: String, var type: Int) : Serializable

class OPDSDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "opdsdatabase", null, 1) {
    companion object {
        private var instance: OPDSDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): OPDSDatabaseOpenHelper {
            if (instance == null) {
                instance = OPDSDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(OPDSTable.NAME, true,
                OPDSTable.ID to TEXT,
                OPDSTable.TITLE to TEXT,
                OPDSTable.HREF to TEXT,
                OPDSTable.TYPE to INTEGER)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
    }
}

object OPDSTable {
    const val NAME = "OPDS"
    const val ID = "id"
    const val HREF = "href"
    const val TITLE = "title"
    const val TYPE = "type"
}

class OPDS(private var database: OPDSDatabaseOpenHelper) {

    fun insert(opds: OPDSModel) {
        val exists = list(opds)
        if (exists.isEmpty()) {
            database.use {
                insert(OPDSTable.NAME,
                        OPDSTable.TITLE to opds.title,
                        OPDSTable.HREF to opds.href,
                        OPDSTable.TYPE to opds.type)
            }
        }
    }

    fun list(opds: OPDSModel): List<OPDSModel> {
        return database.use {
            select(OPDSTable.NAME, OPDSTable.TITLE, OPDSTable.HREF, OPDSTable.TYPE)
                    .whereArgs("title = {title} AND href = {href} AND type = {type}", "title" to opds.title, "href" to opds.href, "type" to opds.type)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(opds: OPDSModel) {
        database.use {
            delete(OPDSTable.NAME, "title = {title} AND href = {href} AND type = {type}", "title" to opds.title, "href" to opds.href, "type" to opds.type)
        }
    }

    fun list(): List<OPDSModel> {
        return database.use {
            select(OPDSTable.NAME, OPDSTable.TITLE, OPDSTable.HREF, OPDSTable.TYPE)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    class MyRowParser : RowParser<OPDSModel> {
        override fun parseRow(columns: Array<Any?>): OPDSModel {
            return OPDSModel(columns[0] as String, columns[1] as String, (columns[2] as Long).toInt())
        }
    }

}