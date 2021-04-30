/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject
import org.readium.r2.navigator.epub.Style
import org.readium.r2.shared.publication.Locator

@Entity(tableName = Highlight.TABLE_NAME)
data class Highlight(
    @PrimaryKey
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    var creation: Long? = null,
    @ColumnInfo(name = BOOK_ID)
    val bookId: Long,
    @ColumnInfo(name = HIGHLIGHT_ID)
    val highlightId: String,
    @ColumnInfo(name = PUBLICATION_ID)
    val publicationId: String,
    @ColumnInfo(name = STYLE)
    val style: String,
    @ColumnInfo(name = COLOR)
    val color: Int,
    @ColumnInfo(name = ANNOTATION)
    val annotation: String,
    @ColumnInfo(name = ANNOTATION_MARK_STYLE)
    val annotationMarkStyle: String,
    @ColumnInfo(name = RESOURCE_INDEX)
    val resourceIndex: Long,
    @ColumnInfo(name = RESOURCE_HREF)
    val resourceHref: String,
    @ColumnInfo(name = RESOURCE_TYPE)
    val resourceType: String,
    @ColumnInfo(name = RESOURCE_TITLE)
    val resourceTitle: String,
    @ColumnInfo(name = LOCATION)
    val location: String,
    @ColumnInfo(name = LOCATOR_TEXT)
    val locatorText: String
) {

    fun toNavigatorHighlight() =
        org.readium.r2.navigator.epub.Highlight(
            highlightId,
            locator,
            color,
            Style.highlight,
            annotationMarkStyle
        )

    val locator: Locator
        get() = Locator(
            href = resourceHref,
            type = resourceType,
            title = resourceTitle,
            locations = Locator.Locations.fromJSON(JSONObject(location)),
            text = Locator.Text.fromJSON(JSONObject(locatorText))
        )

    companion object {

        const val TABLE_NAME = "HIGHLIGHTS"
        const val ID = "ID"
        const val CREATION_DATE = "CREATION_DATE"
        const val PUBLICATION_ID = "PUBLICATION_ID"
        const val RESOURCE_INDEX = "RESOURCE_INDEX"
        const val RESOURCE_HREF = "RESOURCE_HREF"
        const val RESOURCE_TYPE = "RESOURCE_TYPE"
        const val RESOURCE_TITLE = "RESOURCE_TITLE"
        const val LOCATION = "LOCATION"
        const val LOCATOR_TEXT = "LOCATOR_TEXT"
        const val BOOK_ID = "BOOK_ID"
        const val HIGHLIGHT_ID = "HIGHLIGHT_ID"
        const val STYLE = "STYLE"
        const val COLOR = "COLOR"
        const val ANNOTATION = "ANNOTATION"
        const val ANNOTATION_MARK_STYLE = "ANNOTATION_MARK_STYLE"
    }
}