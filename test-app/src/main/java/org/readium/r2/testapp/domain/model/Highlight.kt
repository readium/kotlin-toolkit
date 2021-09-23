/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain.model

import androidx.annotation.ColorInt
import androidx.room.*
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.MapWithDefaultCompanion

/**
 * @param id Primary key, auto-incremented
 * @param style Look and feel of this annotation (highlight, underline)
 * @param title Provides additional context about the annotation
 * @param tint Color associated with the annotation
 * @param bookId Foreign key to the book
 * @param href References a resource within a publication
 * @param type References the media type of a resource within a publication
 * @param totalProgression Overall progression in the publication
 * @param locations Locator locations object
 * @param text Locator text object
 * @param annotation User-provided note attached to the annotation
 */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(entity = Book::class, parentColumns = [Book.ID], childColumns = [Highlight.BOOK_ID], onDelete = ForeignKey.CASCADE)
    ],
)
data class Highlight(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0,
    @ColumnInfo(name = CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    var creation: Long? = null,
    @ColumnInfo(name = BOOK_ID)
    val bookId: Long,
    @ColumnInfo(name = STYLE)
    var style: Style,
    @ColumnInfo(name = TINT, defaultValue = "0")
    @ColorInt var tint: Int,
    @ColumnInfo(name = HREF)
    var href: String,
    @ColumnInfo(name = TYPE)
    var type: String,
    @ColumnInfo(name = TITLE, defaultValue = "NULL")
    var title: String? = null,
    @ColumnInfo(name = TOTAL_PROGRESSION, defaultValue = "0")
    var totalProgression: Double = 0.0,
    @ColumnInfo(name = LOCATIONS, defaultValue = "{}")
    var locations: Locator.Locations = Locator.Locations(),
    @ColumnInfo(name = TEXT, defaultValue = "{}")
    var text: Locator.Text = Locator.Text(),
    @ColumnInfo(name = ANNOTATION, defaultValue = "")
    var annotation: String = "",
) {

    constructor(bookId: Long, style: Style, @ColorInt tint: Int, locator: Locator, annotation: String)
        : this(
            bookId = bookId,
            style = style,
            tint = tint,
            href = locator.href,
            type = locator.type,
            title = locator.title,
            totalProgression = locator.locations.totalProgression ?: 0.0,
            locations = locator.locations,
            text = locator.text,
            annotation = annotation
        )

    val locator: Locator get() = Locator(
        href = href,
        type = type,
        title = title,
        locations = locations,
        text = text,
    )

    enum class Style(val value: String) {
        HIGHLIGHT("highlight"), UNDERLINE("underline");

        companion object : MapWithDefaultCompanion<String, Style>(values(), Style::value, HIGHLIGHT)
    }

    companion object {
        const val TABLE_NAME = "HIGHLIGHTS"
        const val ID = "ID"
        const val CREATION_DATE = "CREATION_DATE"
        const val BOOK_ID = "BOOK_ID"
        const val STYLE = "STYLE"
        const val TINT = "TINT"
        const val HREF = "HREF"
        const val TYPE = "TYPE"
        const val TITLE = "TITLE"
        const val TOTAL_PROGRESSION = "TOTAL_PROGRESSION"
        const val LOCATIONS = "LOCATIONS"
        const val TEXT = "TEXT"
        const val ANNOTATION = "ANNOTATION"
    }
}

class HighlightConverters {
    @TypeConverter
    fun styleFromString(value: String?): Highlight.Style = Highlight.Style(value)
    @TypeConverter
    fun styleToString(style: Highlight.Style): String = style.value

    @TypeConverter
    fun textFromString(value: String?): Locator.Text = Locator.Text.fromJSON(value?.let { JSONObject(it) })
    @TypeConverter
    fun textToString(text: Locator.Text): String = text.toJSON().toString()

    @TypeConverter
    fun locationsFromString(value: String?): Locator.Locations = Locator.Locations.fromJSON(value?.let { JSONObject(it) })
    @TypeConverter
    fun locationsToString(text: Locator.Locations): String = text.toJSON().toString()
}
