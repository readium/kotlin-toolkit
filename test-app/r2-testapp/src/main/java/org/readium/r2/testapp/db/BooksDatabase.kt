/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import org.jetbrains.anko.db.*
import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication
import java.net.URI
import java.nio.file.Paths

/**
 * Global Parameters
 */

lateinit var books: MutableList<Book>


// Access property for Context
val Context.database: BooksDatabaseOpenHelper
    get() = BooksDatabaseOpenHelper.getInstance(applicationContext)

val Context.appContext: Context
    get() = applicationContext

class Book(var id: Long? = null,
           val creation: Long = DateTime().toDate().time,
           val href: String,
           val title: String,
           val author: String? = null,
           val identifier: String,
           val cover: ByteArray? = null,
           val progression: String? = null,
           val utterance: Long? = null,
           val ext: Publication.EXTENSION
) {

    val fileName: String?
        get() {
            val url = URI(href)
            if (!url.scheme.isNullOrEmpty() && url.isAbsolute) {
                val uri = Uri.parse(href);
                return uri.lastPathSegment
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val path = Paths.get(href)
                path.fileName.toString()
            } else {
                val uri = Uri.parse(href);
                uri.lastPathSegment
            }
        }

    val url: URI?
        get() {
            val url = URI(href)
            if (url.isAbsolute && url.scheme.isNullOrEmpty()) {
                return null
            }
            return url
        }


}

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
        private const val DATABASE_VERSION = 4

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
                BOOKSTable.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                BOOKSTable.TITLE to TEXT,
                BOOKSTable.AUTHOR to TEXT,
                BOOKSTable.HREF to TEXT,
                BOOKSTable.IDENTIFIER to TEXT,
                BOOKSTable.COVER to BLOB,
                BOOKSTable.EXTENSION to TEXT,
                BOOKSTable.CREATION to INTEGER,
                BOOKSTable.PROGRESSION to TEXT,
                BOOKSTable.UTTERANCE to INTEGER)

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
                } catch (e: SQLiteException) {
                }
                try {
                    upgradeVersion3(db) {
                        //done
                    }
                } catch (e: SQLiteException) {
                }
                try {
                    upgradeVersion4(db) {
                    //done
                }
                } catch (e: SQLiteException) {
                }
            }
            2 -> {
                try {
                    upgradeVersion3(db) {
                        //done
                    }
                } catch (e: SQLiteException) {
                }
                try {
                    upgradeVersion4(db) {
                        //done
                    }
                } catch (e: SQLiteException) {
                }
            }
            3 -> {
                upgradeVersion4(db) {
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
                values.put(BOOKSTable.EXTENSION, Publication.EXTENSION.EPUB.value)
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
                values.put(BOOKSTable.CREATION, DateTime().toDate().time)
                db.update(BOOKSTable.NAME, values, "${BOOKSTable.ID}=?", arrayOf(id.toString()))
                hasItem = cursor.moveToNext()
            }
            cursor.close()
        }
        callback()
    }
    private fun upgradeVersion4(db: SQLiteDatabase, callback: () -> Unit) {
        db.execSQL("ALTER TABLE " + BOOKSTable.NAME + " ADD COLUMN " + BOOKSTable.UTTERANCE + " INTEGER DEFAULT 0;")
        callback()
    }

}

object BOOKSTable {
    const val NAME = "BOOKS"
    const val ID = "id"
    const val IDENTIFIER = "identifier"
    const val TITLE = "title"
    const val AUTHOR = "author"
    const val HREF = "href"
    const val COVER = "cover"
    const val EXTENSION = "extension"
    const val CREATION = "creationDate"
    const val PROGRESSION = "progression"
    const val UTTERANCE = "utterance"
    var RESULT_COLUMNS = arrayOf(ID, IDENTIFIER, TITLE, AUTHOR, HREF, COVER, EXTENSION, CREATION, PROGRESSION, UTTERANCE)

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
                        BOOKSTable.TITLE to book.title,
                        BOOKSTable.AUTHOR to book.author,
                        BOOKSTable.HREF to book.href,
                        BOOKSTable.IDENTIFIER to book.identifier,
                        BOOKSTable.COVER to book.cover,
                        BOOKSTable.EXTENSION to book.ext.value,
                        BOOKSTable.CREATION to book.creation)
            }
        }
        return null
    }

    private fun has(book: Book): List<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.HREF, BOOKSTable.ID, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION, BOOKSTable.PROGRESSION, BOOKSTable.UTTERANCE)
                    .whereArgs("identifier = {identifier}", "identifier" to book.identifier)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun has(identifier: String): List<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.HREF, BOOKSTable.ID, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION, BOOKSTable.PROGRESSION, BOOKSTable.UTTERANCE)
                    .whereArgs("identifier = {identifier}", "identifier" to identifier)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    private fun has(id: Long): List<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.HREF, BOOKSTable.ID, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION, BOOKSTable.PROGRESSION, BOOKSTable.UTTERANCE)
                    .whereArgs("id = {id}", "id" to id)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }
    fun currentLocator(id: Long): Locator? {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.HREF, BOOKSTable.ID, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION, BOOKSTable.PROGRESSION, BOOKSTable.UTTERANCE)
                    .whereArgs("id = {id}", "id" to id)
                    .exec {
                        parseList(MyRowParser()).firstOrNull()?.progression?.let {
                            Locator.fromJSON(JSONObject(it))
                        } ?: run {
                            null
                        }
                    }
        }
    }

    fun getSavedUtterance(id: Long): Long? {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.HREF, BOOKSTable.ID, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION, BOOKSTable.PROGRESSION, BOOKSTable.UTTERANCE).whereArgs("id = {id}", "id" to id).exec {
                parseList(MyRowParser()).firstOrNull()?.utterance
            }
        }
    }

    fun delete(book: Book): Int {
        return database.use {
            return@use delete(BOOKSTable.NAME, "id = {id}", "id" to book.id!!)
        }
    }

    fun list(): MutableList<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.TITLE, BOOKSTable.AUTHOR, BOOKSTable.HREF, BOOKSTable.ID, BOOKSTable.IDENTIFIER, BOOKSTable.COVER, BOOKSTable.EXTENSION, BOOKSTable.CREATION, BOOKSTable.PROGRESSION, BOOKSTable.UTTERANCE)
                    .orderBy(BOOKSTable.CREATION, SqlOrderDirection.DESC)
                    .orderBy(BOOKSTable.TITLE, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    fun saveCurrentUtterance(bookId: Long, utterance: Long): Boolean {
        val exists = has(bookId)
        if (exists.isEmpty()) {
            return false
        }
        return database.use {
            return@use update(BOOKSTable.NAME, BOOKSTable.UTTERANCE to utterance)
                .whereArgs("${BOOKSTable.ID} = {id}", "id" to bookId)
                .exec() > 0
        }
    }

    fun saveProgression(locator: Locator?, bookId: Long) : Boolean {
        val exists = has(bookId)
        if (exists.isEmpty()) {
            return false
        }
        return database.use {
            return@use update(BOOKSTable.NAME,BOOKSTable.PROGRESSION to locator?.toJSON().toString())
                    .whereArgs("${BOOKSTable.ID} = {id}", "id" to bookId)
                    .exec() > 0
        }
    }

    class MyRowParser : RowParser<Book> {
        override fun parseRow(columns: Array<Any?>): Book {

            val title = columns[0]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }

            val author = columns[1]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }

            val href = columns[2]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }

            val id = columns[3]?.let {
                return@let it as Long
            } ?: kotlin.run { return@run (-1).toLong() }

            val identifier = columns[4]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }

            val cover = columns[5]?.let {
                return@let it as ByteArray
            }

            val ext = columns[6]?.let {
                return@let it as String
            } ?: kotlin.run { return@run "" }

            val creation = columns[7]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }

            val progression = columns[8]?.let {
                return@let it as String
            } ?: kotlin.run { return@run null }

            val utterance = columns[9]?.let {
                return@let it as Long
            } ?: kotlin.run { return@run null }

            return Book(id, creation as Long, href, title, author, identifier, cover, progression, utterance, Publication.EXTENSION.fromString(ext)!!)
        }
    }
}
