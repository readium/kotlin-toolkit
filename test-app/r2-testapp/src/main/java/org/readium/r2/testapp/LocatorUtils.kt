/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.select
import org.json.JSONObject
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication


/**
 * LocatorUtils : API used to get the current location as a Locator object
 *                  or to reach a Locator place in a publication
 */

class LocatorUtils(context: Context) {

    // Size of a segment in a publication, expressed in characters
    val SEGMENT = 1024
    var bookmarkDB: BookmarksDatabase = BookmarksDatabase(context)

    fun getBookmarks(bookID: Long? = null): MutableList<Locator> {
        if (bookID != null) {
            return bookmarkDB.shared.use {
                select(BOOKMARKSTable.NAME,
                        BOOKMARKSTable.ID,
                        BOOKMARKSTable.BOOK_ID,
                        BOOKMARKSTable.RESOURCE_INDEX,
                        BOOKMARKSTable.RESOURCE_HREF,
                        BOOKMARKSTable.PROGRESSION,
                        BOOKMARKSTable.CREATED)
                        .whereArgs("bookID = {bookID}", "bookID" to bookID)
                        .exec {
                            parseList(BOOKMARKS.MyRowParser()).toMutableList()
                        }
            }
        } else {
            return bookmarkDB.shared.use {
                select(BOOKMARKSTable.NAME,
                        BOOKMARKSTable.ID,
                        BOOKMARKSTable.BOOK_ID,
                        BOOKMARKSTable.RESOURCE_INDEX,
                        BOOKMARKSTable.RESOURCE_HREF,
                        BOOKMARKSTable.PROGRESSION,
                        BOOKMARKSTable.CREATED)
                        .exec {
                            parseList(BOOKMARKS.MyRowParser()).toMutableList()
                        }
                }
        }
    }

    fun getAnnotations(resourceHref: String? = null) {

    }


    fun getCurrentLocator(): Locator {
        return Locator("pid", 1, "href", "title", null)
    }

    fun goToLocator(){

    }


    fun addLocator(locator: Locator, listOfLocators: MutableList<Locator>): Boolean {

        var added = false

        if (listOfLocators.isEmpty()) {
            when(locator) {
                is Bookmark -> {
                    bookmarkDB.bookmarks.insert(locator)
                    added = true
                }
                else -> {
                    println("An error has occurred while adding the locator")
                    added = false
                }
            }
        } else {
            for (loc in listOfLocators) {
                print("C1 : ")
                println( loc.location.toString() != locator.location.toString() )
                print("C2 : ")
                println( loc.location.toString() == listOfLocators.last().location.toString() )

                if ( loc.location.toString() != locator.location.toString() && loc.location.toString() == listOfLocators.last().location.toString() ) {
                    when(locator) {
                        is Bookmark -> {
                            bookmarkDB.bookmarks.insert(locator)
                            added = true
                        }
                        else -> {
                            println("An error has occurred while adding the locator")
                            added = false
                        }
                    }
                } else if (loc.location == locator.location) {
                    println("is someone there ?")
                    return false
                }
            }
        }

        return added
    }


    fun deleteLocator(locator: Locator, listOfLocators: MutableList<Locator>): Boolean {

        if( listOfLocators.indexOf(locator) != -1 ) {
            listOfLocators.remove(locator)
            when(locator) {
                is Bookmark -> {
                    bookmarkDB.bookmarks.delete(locator)
                    return true
                }
                else -> {
                    println("An error has occurred while deleting the locator")
                    return false
                }
            }
        } else {
            println("Your locator does not exists, and thus, cannot be deleted")
            return false
        }

    }

}