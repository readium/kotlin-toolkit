/*
 * Module: r2-testapp-kotlin
 * Developers: Taehyun Kim, Seongjin Kim
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.LocatorText

class Highlight(val highlightID: String,
                val publicationID: String,
                val style: String,
                val color: Int,
                val annotation: String,
                val annotationMarkStyle: String,
                val resourceIndex: Long,
                val resourceHref: String,
                val resourceType: String,
                val resourceTitle: String,
                val location: Locations,
                val locatorText: LocatorText,
                var creationDate: Long = DateTime().toDate().time,
                var id: Long? = null,
                val bookID: Long):
        Locator(resourceHref, resourceType, resourceTitle, location, locatorText)

class HighligtsDatabase(context: Context) {

    val shared: HighlightsDatabaseOpenHelper = HighlightsDatabaseOpenHelper(context)
    var highlights: HIGHLIGHTS

    init {
        highlights = HIGHLIGHTS(shared)
    }

}

class HighlightsDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "highlights_database", null, DATABASE_VERSION) {
    companion object {
        private var instance: HighlightsDatabaseOpenHelper? = null
        private const val DATABASE_VERSION = 3

        @Synchronized
        fun getInstance(ctx: Context): HighlightsDatabaseOpenHelper {
            if (instance == null) {
                instance = HighlightsDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(HIGHLIGHTSTable.NAME, true,
                HIGHLIGHTSTable.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                HIGHLIGHTSTable.HIGHLIGHT_ID to INTEGER,
                HIGHLIGHTSTable.PUBLICATION_ID to TEXT,
                HIGHLIGHTSTable.BOOK_ID to INTEGER,
                HIGHLIGHTSTable.RESOURCE_INDEX to INTEGER,
                HIGHLIGHTSTable.STYLE to TEXT,
                HIGHLIGHTSTable.COLOR to INTEGER,
                HIGHLIGHTSTable.ANNOTATION to TEXT,
                HIGHLIGHTSTable.ANNOTATION_MARK_STYLE to TEXT,
                HIGHLIGHTSTable.RESOURCE_HREF to TEXT,
                HIGHLIGHTSTable.RESOURCE_TYPE to TEXT,
                HIGHLIGHTSTable.RESOURCE_TITLE to TEXT,
                HIGHLIGHTSTable.LOCATION to TEXT,
                HIGHLIGHTSTable.LOCATOR_TEXT to TEXT,
                HIGHLIGHTSTable.CREATION_DATE to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {



    }


}

object HIGHLIGHTSTable {
    const val NAME = "HIGHLIGHTS"
    const val ID = "id"
    const val BOOK_ID = "bookID"
    const val HIGHLIGHT_ID = "highlightID"
    const val PUBLICATION_ID = "publicationID"
    const val STYLE = "style"
    const val COLOR = "color"
    const val ANNOTATION = "annotation"
    const val ANNOTATION_MARK_STYLE = "annotationMarkStyle"
    const val RESOURCE_INDEX = "resourceIndex"
    const val RESOURCE_HREF = "resourceHref"
    const val RESOURCE_TYPE = "resourceType"
    const val RESOURCE_TITLE = "resourceTitle"
    const val LOCATION = "location"
    const val LOCATOR_TEXT = "locatorText"
    const val CREATION_DATE = "creationDate"
    var RESULT_COLUMNS = arrayOf(ID, HIGHLIGHT_ID, PUBLICATION_ID, BOOK_ID, STYLE, COLOR, ANNOTATION, ANNOTATION_MARK_STYLE, RESOURCE_INDEX, RESOURCE_HREF, RESOURCE_TYPE, RESOURCE_TITLE, LOCATION, LOCATOR_TEXT, CREATION_DATE)

}

class HIGHLIGHTS(private var database: HighlightsDatabaseOpenHelper) {

    fun dropTable() {
        database.use {
            dropTable(HIGHLIGHTSTable.NAME, true)
        }
    }

    fun emptyTable() {
        database.use {
            delete(HIGHLIGHTSTable.NAME, "")
        }
    }

    fun insert(highlight: Highlight): Long? {
        if (highlight.highlightID.length < 0 ||
                highlight.resourceIndex < 0){
            return null
        }
        val exists = has(highlight)
        if (exists.isEmpty()) {
            return database.use {
                return@use insert(HIGHLIGHTSTable.NAME,
                        HIGHLIGHTSTable.HIGHLIGHT_ID to highlight.highlightID,
                        HIGHLIGHTSTable.PUBLICATION_ID to highlight.publicationID,
                        HIGHLIGHTSTable.BOOK_ID to highlight.bookID,
                        HIGHLIGHTSTable.STYLE to highlight.style,
                        HIGHLIGHTSTable.COLOR to highlight.color,
                        HIGHLIGHTSTable.ANNOTATION to highlight.annotation,
                        HIGHLIGHTSTable.ANNOTATION_MARK_STYLE to highlight.annotationMarkStyle,
                        HIGHLIGHTSTable.RESOURCE_INDEX to highlight.resourceIndex,
                        HIGHLIGHTSTable.RESOURCE_HREF to highlight.resourceHref,
                        HIGHLIGHTSTable.RESOURCE_TYPE to highlight.resourceType,
                        HIGHLIGHTSTable.RESOURCE_TITLE to highlight.resourceTitle,
                        HIGHLIGHTSTable.LOCATION to highlight.location.toJSON().toString(),
                        HIGHLIGHTSTable.LOCATOR_TEXT to highlight.locatorText.toJSON().toString(),
                        HIGHLIGHTSTable.CREATION_DATE to highlight.creationDate)
            }
        }
        else {
            return database.use {
                update(HIGHLIGHTSTable.NAME,
                        HIGHLIGHTSTable.HIGHLIGHT_ID to highlight.highlightID,
                        HIGHLIGHTSTable.PUBLICATION_ID to highlight.publicationID,
                        HIGHLIGHTSTable.BOOK_ID to highlight.bookID,
                        HIGHLIGHTSTable.STYLE to highlight.style,
                        HIGHLIGHTSTable.COLOR to highlight.color,
                        HIGHLIGHTSTable.ANNOTATION to highlight.annotation,
                        HIGHLIGHTSTable.ANNOTATION_MARK_STYLE to highlight.annotationMarkStyle,
                        HIGHLIGHTSTable.RESOURCE_INDEX to highlight.resourceIndex,
                        HIGHLIGHTSTable.RESOURCE_HREF to highlight.resourceHref,
                        HIGHLIGHTSTable.RESOURCE_TYPE to highlight.resourceType,
                        HIGHLIGHTSTable.RESOURCE_TITLE to highlight.resourceTitle,
                        HIGHLIGHTSTable.LOCATION to highlight.location.toJSON().toString(),
                        HIGHLIGHTSTable.LOCATOR_TEXT to highlight.locatorText.toJSON().toString(),
                        HIGHLIGHTSTable.CREATION_DATE to highlight.creationDate)
                        .whereArgs("(highlightID = {highlightID})",
                                "highlightID" to highlight.highlightID).exec()
                return@use null
            }
        }
        return null
    }

    fun has(highlight: Highlight): List<Highlight> {
        return database.use {

            select(HIGHLIGHTSTable.NAME,
                    HIGHLIGHTSTable.ID,
                    HIGHLIGHTSTable.HIGHLIGHT_ID,
                    HIGHLIGHTSTable.PUBLICATION_ID,
                    HIGHLIGHTSTable.STYLE,
                    HIGHLIGHTSTable.COLOR,
                    HIGHLIGHTSTable.ANNOTATION,
                    HIGHLIGHTSTable.ANNOTATION_MARK_STYLE,
                    HIGHLIGHTSTable.RESOURCE_INDEX,
                    HIGHLIGHTSTable.RESOURCE_HREF,
                    HIGHLIGHTSTable.RESOURCE_TYPE,
                    HIGHLIGHTSTable.RESOURCE_TITLE,
                    HIGHLIGHTSTable.LOCATION,
                    HIGHLIGHTSTable.LOCATOR_TEXT,
                    HIGHLIGHTSTable.CREATION_DATE,
                    HIGHLIGHTSTable.BOOK_ID)
                    .whereArgs("(highlightID = {highlightID})",
                            "highlightID" to highlight.highlightID)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(locator: Highlight) {
        database.use {
            delete(HIGHLIGHTSTable.NAME, "id = {id}",
                    "id" to locator.id!!)
        }
    }

    fun delete(highlight_id: String?) {
        highlight_id?.let {
            database.use {
                delete(HIGHLIGHTSTable.NAME, "${HIGHLIGHTSTable.HIGHLIGHT_ID} = {highlightID}",
                        "highlightID" to highlight_id)
            }
        }
    }

    fun listAll(): MutableList<Highlight> {
        return database.use {
            select(HIGHLIGHTSTable.NAME,
                    HIGHLIGHTSTable.ID,
                    HIGHLIGHTSTable.HIGHLIGHT_ID,
                    HIGHLIGHTSTable.PUBLICATION_ID,
                    HIGHLIGHTSTable.STYLE,
                    HIGHLIGHTSTable.COLOR,
                    HIGHLIGHTSTable.ANNOTATION,
                    HIGHLIGHTSTable.ANNOTATION_MARK_STYLE,
                    HIGHLIGHTSTable.RESOURCE_INDEX,
                    HIGHLIGHTSTable.RESOURCE_HREF,
                    HIGHLIGHTSTable.RESOURCE_TYPE,
                    HIGHLIGHTSTable.RESOURCE_TITLE,
                    HIGHLIGHTSTable.LOCATION,
                    HIGHLIGHTSTable.LOCATOR_TEXT,
                    HIGHLIGHTSTable.CREATION_DATE,
                    HIGHLIGHTSTable.BOOK_ID)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }
    
    fun listAll(bookID: Long, resourceHref: String): MutableList<Highlight> {
        return database.use {
            select(HIGHLIGHTSTable.NAME,
                    HIGHLIGHTSTable.ID,
                    HIGHLIGHTSTable.HIGHLIGHT_ID,
                    HIGHLIGHTSTable.PUBLICATION_ID,
                    HIGHLIGHTSTable.STYLE,
                    HIGHLIGHTSTable.COLOR,
                    HIGHLIGHTSTable.ANNOTATION,
                    HIGHLIGHTSTable.ANNOTATION_MARK_STYLE,
                    HIGHLIGHTSTable.RESOURCE_INDEX,
                    HIGHLIGHTSTable.RESOURCE_HREF,
                    HIGHLIGHTSTable.RESOURCE_TYPE,
                    HIGHLIGHTSTable.RESOURCE_TITLE,
                    HIGHLIGHTSTable.LOCATION,
                    HIGHLIGHTSTable.LOCATOR_TEXT,
                    HIGHLIGHTSTable.CREATION_DATE,
                    HIGHLIGHTSTable.BOOK_ID)
                    .whereArgs("${HIGHLIGHTSTable.BOOK_ID} = {bookID} AND ${HIGHLIGHTSTable.RESOURCE_HREF} = {resourceHref}",
                            "bookID" to bookID, "resourceHref" to resourceHref)
                    .orderBy(BOOKMARKSTable.RESOURCE_INDEX, SqlOrderDirection.ASC)
                    .orderBy(BOOKMARKSTable.CREATION_DATE, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    fun listAll(bookID: Long): MutableList<Highlight> {
        return database.use {
            select(HIGHLIGHTSTable.NAME,
                    HIGHLIGHTSTable.ID,
                    HIGHLIGHTSTable.HIGHLIGHT_ID,
                    HIGHLIGHTSTable.PUBLICATION_ID,
                    HIGHLIGHTSTable.STYLE,
                    HIGHLIGHTSTable.COLOR,
                    HIGHLIGHTSTable.ANNOTATION,
                    HIGHLIGHTSTable.ANNOTATION_MARK_STYLE,
                    HIGHLIGHTSTable.RESOURCE_INDEX,
                    HIGHLIGHTSTable.RESOURCE_HREF,
                    HIGHLIGHTSTable.RESOURCE_TYPE,
                    HIGHLIGHTSTable.RESOURCE_TITLE,
                    HIGHLIGHTSTable.LOCATION,
                    HIGHLIGHTSTable.LOCATOR_TEXT,
                    HIGHLIGHTSTable.CREATION_DATE,
                    HIGHLIGHTSTable.BOOK_ID)
                    .whereArgs("bookID = {bookID}", "bookID" to bookID as Any)
                    .orderBy(BOOKMARKSTable.RESOURCE_INDEX, SqlOrderDirection.ASC)
                    .orderBy(BOOKMARKSTable.CREATION_DATE, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    fun list(highlightID: String): MutableList<Highlight> {
        return database.use {
            select(HIGHLIGHTSTable.NAME,
                    HIGHLIGHTSTable.ID,
                    HIGHLIGHTSTable.HIGHLIGHT_ID,
                    HIGHLIGHTSTable.PUBLICATION_ID,
                    HIGHLIGHTSTable.STYLE,
                    HIGHLIGHTSTable.COLOR,
                    HIGHLIGHTSTable.ANNOTATION,
                    HIGHLIGHTSTable.ANNOTATION_MARK_STYLE,
                    HIGHLIGHTSTable.RESOURCE_INDEX,
                    HIGHLIGHTSTable.RESOURCE_HREF,
                    HIGHLIGHTSTable.RESOURCE_TYPE,
                    HIGHLIGHTSTable.RESOURCE_TITLE,
                    HIGHLIGHTSTable.LOCATION,
                    HIGHLIGHTSTable.LOCATOR_TEXT,
                    HIGHLIGHTSTable.CREATION_DATE,
                    HIGHLIGHTSTable.BOOK_ID)
                    .whereArgs("highlightID = {highlightID}", "highlightID" to highlightID as Any)
                    .orderBy(HIGHLIGHTSTable.RESOURCE_INDEX, SqlOrderDirection.ASC)
                    .orderBy(HIGHLIGHTSTable.CREATION_DATE, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    class MyRowParser : RowParser<Highlight> {
        override fun parseRow(columns: Array<Any?>): Highlight {
            val id = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val highlightID = columns[1]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val publicationID = columns[2]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val style = columns[3]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val color = columns[4]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val annotation = columns[5]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val annotationMarkStyle = columns[6]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val resourceIndex = columns[7]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val resourceHref = columns[8]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val resourceType = columns[9]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val resourceTitle = columns[10]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val location = columns[11]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val locatorText = columns[12]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val created = columns[13]?.let {
                return@let it
            } ?: kotlin.run { return@run null }
            val bookID = columns[14]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }

            return Highlight(highlightID as String, publicationID as String, style as String, (color as Long).toInt(), annotation as String, annotationMarkStyle as String, resourceIndex as Long, resourceHref as String, resourceType as String, resourceTitle as String, Locations.fromJSON(JSONObject(location as String)), LocatorText.fromJSON(JSONObject(locatorText as String)), created as Long,  id as Long, bookID =  bookID as Long)
        }
    }

}