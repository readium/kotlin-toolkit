/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain.model

import android.net.Uri
import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.readium.r2.shared.util.mediatype.MediaType
import java.net.URI
import java.nio.file.Paths


@Entity(tableName = Book.TABLE_NAME)
data class Book(
    @PrimaryKey
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = Bookmark.CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    val creation: Long? = null,
    @ColumnInfo(name = HREF)
    val href: String,
    @ColumnInfo(name = TITLE)
    val title: String,
    @ColumnInfo(name = AUTHOR)
    val author: String? = null,
    @ColumnInfo(name = IDENTIFIER)
    val identifier: String,
    @ColumnInfo(name = PROGRESSION)
    val progression: String? = null,
    @ColumnInfo(name = TYPE)
    val type: String
) {

    val fileName: String?
        get() {
            val url = URI(href)
            if (!url.scheme.isNullOrEmpty() && url.isAbsolute) {
                val uri = Uri.parse(href)
                return uri.lastPathSegment
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val path = Paths.get(href)
                path.fileName.toString()
            } else {
                val uri = Uri.parse(href)
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

    suspend fun mediaType(): MediaType? = MediaType.of(type)

    companion object {

        const val TABLE_NAME = "books"
        const val ID = "id"
        const val CREATION_DATE = "creation_date"
        const val HREF = "href"
        const val TITLE = "title"
        const val AUTHOR = "author"
        const val IDENTIFIER = "identifier"
        const val PROGRESSION = "progression"
        const val TYPE = "type"
    }
}