/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

@Entity(
    tableName = Bookmark.TABLE_NAME, indices = [Index(
        value = ["BOOK_ID", "LOCATION"],
        unique = true
    )]
)
data class Bookmark(
    @PrimaryKey
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    var creation: Long? = null,
    @ColumnInfo(name = BOOK_ID)
    val bookId: Long,
    @ColumnInfo(name = PUBLICATION_ID)
    val publicationId: String,
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

    val locator
        get() = Locator(
            href = resourceHref,
            type = resourceType,
            title = resourceTitle,
            locations = Locator.Locations.fromJSON(JSONObject(location)),
            text = Locator.Text.fromJSON(JSONObject(locatorText))
        )

    companion object {

        const val TABLE_NAME = "BOOKMARKS"
        const val ID = "ID"
        const val CREATION_DATE = "CREATION_DATE"
        const val BOOK_ID = "BOOK_ID"
        const val PUBLICATION_ID = "PUBLICATION_ID"
        const val RESOURCE_INDEX = "RESOURCE_INDEX"
        const val RESOURCE_HREF = "RESOURCE_HREF"
        const val RESOURCE_TYPE = "RESOURCE_TYPE"
        const val RESOURCE_TITLE = "RESOURCE_TITLE"
        const val LOCATION = "LOCATION"
        const val LOCATOR_TEXT = "LOCATOR_TEXT"
    }
}