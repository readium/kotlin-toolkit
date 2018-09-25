///*
// * Module: r2-testapp-kotlin
// * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
// *
// * Copyright (c) 2018. Readium Foundation. All rights reserved.
// * Use of this source code is governed by a BSD-style license which is detailed in the
// * LICENSE file present in the project repository where this source code is maintained.
// */
//
//package org.readium.r2.testapp
//
//import android.content.Context
//import org.jetbrains.anko.db.parseList
//import org.jetbrains.anko.db.select
//import org.readium.r2.shared.Locator
//
//
///**
// * LocatorUtils : API used to get the current location as a Locator object
// *                  or to reach a Locator place in a publication
// */
//
//class LocatorUtils(context: Context) {
//
//    // Size of a segment in a publication, expressed in characters
//    val SEGMENT = 1024
//    var bookmarkDB: BookmarksDatabase = BookmarksDatabase(context)
//
//    fun getBookmarks(bookID: Long? = null): MutableList<Locator> {
//        if (bookID != null) {
//            return bookmarkDB.shared.use {
//                select(BOOKMARKSTable.NAME,
//                        BOOKMARKSTable.ID,
//                        BOOKMARKSTable.BOOK_ID,
//                        BOOKMARKSTable.RESOURCE_INDEX,
//                        BOOKMARKSTable.RESOURCE_HREF,
//                        BOOKMARKSTable.PROGRESSION,
//                        BOOKMARKSTable.CREATED)
//                        .whereArgs("bookID = {bookID}", "bookID" to bookID)
//                        .exec {
//                            parseList(BOOKMARKS.MyRowParser()).toMutableList()
//                        }
//            }
//        } else {
//            return bookmarkDB.shared.use {
//                select(BOOKMARKSTable.NAME,
//                        BOOKMARKSTable.ID,
//                        BOOKMARKSTable.BOOK_ID,
//                        BOOKMARKSTable.RESOURCE_INDEX,
//                        BOOKMARKSTable.RESOURCE_HREF,
//                        BOOKMARKSTable.PROGRESSION,
//                        BOOKMARKSTable.CREATED)
//                        .exec {
//                            parseList(BOOKMARKS.MyRowParser()).toMutableList()
//                        }
//                }
//        }
//    }
//
//    fun getAnnotations(resourceHref: String? = null) {
//
//    }
//
//
//    fun getCurrentLocator(): Locator {
//        return Locator("pid", 1, "href", "title", null)
//    }
//
//    fun goToLocator(locator: Locator) {
//
//    }
//
//
//    fun addLocator(locator: Locator, listOfLocators: MutableList<Locator>): Locator? {
//
//        var addedLocator: Locator? = null
//
//        if (listOfLocators.isEmpty()) {
//            when(locator) {
//                is Locator -> {
//                    bookmarkDB.bookmarks.insert(locator)
//                    addedLocator = locator
//                }
//                else -> {
//                    println("An error has occurred while adding the locator")
//                    addedLocator = null
//                }
//            }
//        } else {
//            for (loc in listOfLocators) {
//                if ( loc.location.toString() != locator.location.toString() && loc.location.toString() == listOfLocators.last().location.toString() ) {
//                    when(locator) {
//                        is Locator -> {
//                            bookmarkDB.bookmarks.insert(locator)
//                            addedLocator = locator
//                        }
//                        else -> {
//                            println("An error has occurred while adding the locator")
//                            addedLocator = null
//                        }
//                    }
//                } else if (loc.location == locator.location) {
//                    addedLocator = null
//                }
//            }
//        }
//
//        return addedLocator
//    }
//
//
//    fun deleteLocator(locator: Locator, listOfLocators: MutableList<Locator>): Locator? {
//
//        var deletedLocator: Locator? = null
//
//        if( listOfLocators.indexOf(locator) != -1 ) {
//            listOfLocators.remove(locator)
//            when(locator) {
//                is Locator -> {
//                    bookmarkDB.bookmarks.delete(locator)
//                    deletedLocator = locator
//                }
//                else -> {
//                    println("An error has occurred while deleting the locator")
//                    deletedLocator = null
//                }
//            }
//        } else {
//            println("Your locator does not exists, and thus, cannot be deleted")
//            deletedLocator = null
//        }
//
//        return deletedLocator
//
//    }
//
//}