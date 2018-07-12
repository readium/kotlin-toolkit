/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*


// Access property for Context
val Context.database: BooksDatabaseOpenHelper
    get() = BooksDatabaseOpenHelper.getInstance(getApplicationContext())

val Context.appContext: Context
    get() = getApplicationContext()

class Book(val fileName: String, val title: String, val author: String, val fileUrl: String, val id: Long, val coverLink: String?, val identifier: String, val cover: ByteArray?)

class BooksDatabase {

    val shared:BooksDatabaseOpenHelper
    var books: BOOKS

    constructor(context: Context) {
        shared = BooksDatabaseOpenHelper(context)
        books = BOOKS(shared)
    }

}

class BooksDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "books_database", null, 1) {
    companion object {
        private var instance: BooksDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): BooksDatabaseOpenHelper {
            if (instance == null) {
                instance = BooksDatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(BOOKSTable.NAME, true,
                BOOKSTable.ID to INTEGER + PRIMARY_KEY + UNIQUE,
                BOOKSTable.FILENAME to TEXT,
                BOOKSTable.TITLE to TEXT,
                BOOKSTable.AUTHOR to TEXT,
                BOOKSTable.FILEURL to TEXT,
                BOOKSTable.IDENTIFIER to TEXT,
                BOOKSTable.COVER to BLOB,
                BOOKSTable.COVERURL to TEXT)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        db.dropTable(BOOKSTable.NAME, true)
    }
}

object BOOKSTable {
    val NAME = "BOOKS"
    val ID = "id"
    val IDENTIFIER = "identifier"
    val FILENAME = "href"
    val TITLE = "title"
    val AUTHOR = "author"
    val FILEURL = "fileUrl"
    val COVER = "cover"
    val COVERURL = "coverUrl"
}

class BOOKS(var database: BooksDatabaseOpenHelper) {

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
                        BOOKSTable.COVERURL to book.coverLink)
            }
        }
        return null
    }

    fun has(book: Book): List<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.FILENAME,BOOKSTable.TITLE,BOOKSTable.AUTHOR,BOOKSTable.FILEURL,BOOKSTable.ID, BOOKSTable.COVERURL, BOOKSTable.IDENTIFIER,BOOKSTable.COVER)
                    .whereArgs("identifier = {identifier}", "identifier" to book.identifier)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(book: Book) {
        database.use {
            delete(BOOKSTable.NAME, "id = {id}", "id" to book.id!! )
        }
    }

    fun list(): MutableList<Book> {
        return database.use {
            select(BOOKSTable.NAME, BOOKSTable.FILENAME,BOOKSTable.TITLE,BOOKSTable.AUTHOR,BOOKSTable.FILEURL,BOOKSTable.ID, BOOKSTable.COVERURL, BOOKSTable.IDENTIFIER,BOOKSTable.COVER)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    class MyRowParser : RowParser<Book> {
        override fun parseRow(columns: Array<Any?>): Book {
            val filename = columns[0]?.let {
                return@let it as String
            }?: kotlin.run { return@run "" }
            val title = columns[1]?.let {
                return@let it as String
            }?: kotlin.run { return@run "" }
            val author = columns[2]?.let {
                return@let it as String
            }?: kotlin.run { return@run "" }
            val fileUrl = columns[3]?.let {
                return@let it as String
            }?: kotlin.run { return@run "" }
            val id = columns[4]?.let {
                return@let it as Long
            }?: kotlin.run { return@run -1.toLong() }
            val coverUrl = columns[5]?.let {
                return@let it as String
            }
            val identifier = columns[6]?.let {
                return@let it as String
            }?: kotlin.run { return@run "" }
            val cover = columns[7]?.let {
                return@let it as ByteArray
            }

            return  Book(filename, title, author, fileUrl, id,  coverUrl, identifier, cover)

        }
    }

}