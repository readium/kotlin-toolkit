/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import org.jetbrains.anko.db.*
import org.joda.time.DateTime
import org.readium.r2.shared.Publication


// Access property for Context
val Context.database: BooksDatabaseOpenHelper
    get() = BooksDatabaseOpenHelper.getInstance(applicationContext)

val Context.appContext: Context
    get() = applicationContext

class Book(val fileName: String, val title: String, val author: String?, val fileUrl: String, var id: Long?, val coverLink: String?, val identifier: String, val cover: ByteArray?, val ext: Publication.EXTENSION, val creation:Long = DateTime().toDate().time)

class BooksDatabase(context: Context) {

    val shared: BooksDatabaseOpenHelper = BooksDatabaseOpenHelper(context)
    var books: BOOKS

    init {
        books = BOOKS(shared)
    }

}

class BooksDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "books_database", null, DATABASE_VERSION) {
    companion object {
        private var instance: BooksDatabaseOpenHelper? = null
        private const val DATABASE_VERSION = 3

        @Synchronized
        fun getInstance(ctx: Context): BooksDatabaseOpenHelper {
            if (instance == null) {
                instance = BooksDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(BOOKSTable.NAME, true,
                BOOKSTable.ID to INTEGER + PRIMARY_KEY  + AUTOINCREMENT,
                BOOKSTable.FILENAME to TEXT,
                BOOKSTable.TITLE to TEXT,
                BOOKSTable.AUTHOR to TEXT,
                BOOKSTable.FILEURL to TEXT,
                BOOKSTable.IDENTIFIER to TEXT,
                BOOKSTable.COVER to BLOB,
                BOOKSTable.COVERURL to TEXT,
                BOOKSTable.EXTENSION to TEXT,
                BOOKSTable.CREATION to INTEGER)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        // migration = add extension column
        when (oldVersion) {
            1 -> {
                try {
                    upgradeVersion2(db) {
                        //done
                    }
                } catch (e: SQLiteException) { }
                try {
                    upgradeVersion3(db) {
                        //done
                    }
                } catch (e: SQLiteException) { }
            }
            2 -> {
                upgradeVersion3(db) {
                    //done
                }
            }
        }
    }

    private fun upgradeVersion2(db: SQLiteDatabase, callback: () -> Unit) {
        db.execSQL("ALTER TABLE " + BOOKSTable.NAME + " ADD COLUMN " + BOOKSTable.EXTENSION + " TEXT DEFAULT '.epub';")
        val cursor = db.query(BOOKSTable.NAME, BOOKSTable.RESULT_COLUMNS, null, null, null, null, null, null)
        if (cursor != null) {
            var hasItem = cursor.moveToFirst()
            while (hasItem) {
                val id = cursor.getInt(cursor.getColumnIndex(BOOKSTable.ID))
                val values = ContentValues()
                values.put(BOOKSTable.EXTENSION, ".epub")
                db.update(BOOKSTable.NAME, values, "${BOOKSTable.ID}=?", arrayOf(id.toString()))
                hasItem = cursor.moveToNext()
            }
            cursor.close()
        }
        callback()
    }
    private fun upgradeVersion3(db: SQLiteDatabase, callback: () -> Unit) {
        db.execSQL("ALTER TABLE " + BOOKSTable.NAME + " ADD COLUMN " + BOOKSTable.CREATION + " INTEGER DEFAULT ${DateTime().toDate().time};")
        val cursor = db.query(BOOKSTable.NAME, BOOKSTable.RESULT_COLUMNS, null, null, null, null, null, null)
        if (cursor != null) {
            var hasItem = cursor.moveToFirst()
            while (hasItem) {
                val id = cursor.getInt(cursor.getColumnIndex(BOOKSTable.ID))
                val values = ContentValues()
                values.put(BOOKSTable. CREATION, DateTime().toDate().time)
                db.update(BOOKSTable.NAME, values, "${BOOKSTable.ID}=?", arrayOf(id.toString()))
                hasItem = cursor.moveToNext()
            }
            cursor.close()
        }
        callback()
    }

}

object BOOKSTable {
    const val NAME = "BOOKS"
    const val ID = "id"
    const val IDENTIFIER = "identifier"
    const val FILENAME = "href"
    const val TITLE = "title"
    const val AUTHOR = "author"
    const val FILEURL = "fileUrl"
    const val COVER = "cover"
    const val COVERURL = "coverUrl"
    const val EXTENSION = "extension"
    const val CREATION = "creantionDate"
    var RESULT_COLUMNS = arrayOf(ID, IDENTIFIER, FILENAME, TITLE, AUTHOR, FILEURL, COVER, COVERURL, EXTENSION, CREATION)

}

class BOOKS(private var database: BooksDatabaseOpenHelper) {

    fun dropTable() {
        database.use {
            dropTable(BOOKSTable.NAME, true)
        }
    }

    fun insert(book: Book, allowDuplicates: Boolean): Long? {
        val exists = has(book)
        if (exists.isEmpty() || allowDuplicates) {
            return database.use {
                return@use insert(BOOKSTable.NAME,
                        BOOKSTable.FILENAME to book.fileName,
                        BOOKSTable.TITLE to book.title,
                        BOOKSTable.AUTHOR to book.author,
                        BOOKSTable.FILEURL to book.fileUrl,
                        BOOKSTable.IDENTIFIER to book.identifier,
                        BOOKSTable.COVER to book.cover,
                        BOOKSTable.COVERURL to book.coverLink,
                        BOOKSTable.EXTENSION to book.ext.value,
                        BOOKSTable.CREATION to book.creation)
            }
        }
        return null
    }

    private fun has(book: Book): List<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.FILENAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.FILEURL, BOOKSTable.ID, BOOKSTable.COVERURL, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION)
                    .whereArgs("identifier = {identifier}", "identifier" to book.identifier)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun has(identifier: String): List<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.FILENAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.FILEURL, BOOKSTable.ID, BOOKSTable.COVERURL, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION)
                    .whereArgs("identifier = {identifier}", "identifier" to identifier)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(book: Book) : Int {
        return database.use {
            return@use delete(BOOKSTable.NAME, "id = {id}", "id" to book.id!!)
        }
    }

    fun list(): MutableList<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.FILENAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.FILEURL, BOOKSTable.ID, BOOKSTable.COVERURL, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION)
                    .orderBy(BOOKSTable.CREATION, SqlOrderDirection.DESC)
                    .orderBy(BOOKSTable.TITLE, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    class MyRowParser : RowParser<Book> {
        override fun parseRow(columns: Array<Any?>): Book {
            val filename = columns[0]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }
            val title = columns[1]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }
            val author = columns[2]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }
            val fileUrl = columns[3]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }
            val id = columns[4]?.let {
                return@let it as Long
            } ?: kotlin.run { return@run (-1).toLong() }
            val coverUrl = columns[5]?.let {
                return@let it as String
            }
            val identifier = columns[6]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }
            val cover = columns[7]?.let {
                return@let it as ByteArray
            }
            val ext = columns[8]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }
            val creation = columns[9]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }

            return Book(filename, title, author, fileUrl, id, coverUrl, identifier, cover, Publication.EXTENSION.fromString(ext)!!, creation as Long)

        }
    }

}
