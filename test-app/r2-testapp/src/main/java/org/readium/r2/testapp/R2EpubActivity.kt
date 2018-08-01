/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Intent
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.intentFor
import org.readium.r2.navigator.DRMManagementActivity
import org.readium.r2.navigator.R2EpubActivity


class R2EpubActivity : R2EpubActivity() {

    private var menuBmk: MenuItem? = null
    lateinit var bookmarkkDB: BookmarksDatabase

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(org.readium.r2.testapp.R.menu.menu_navigation, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bmk_list)
        menuDrm?.setVisible(false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val bkId = intent.getLongExtra("bookId", -1)
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publicationPath", publicationPath)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bkId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.toc), 0, 0, Gravity.END)
                return true
            }
            R.id.drm -> {
                startActivity(intentFor<DRMManagementActivity>("drmModel" to drmModel))
                return true
            }
            R.id.bookmark -> {
                val bookId = intent.getLongExtra("bookId", -1)
                val resourceIndex = resourcePager.currentItem.toLong()
                val resourceHref = publication.spine[resourcePager.currentItem].href!!
                val progression = preferences.getString("$publicationIdentifier-documentProgression", 0.toString()).toDouble()

                bookmarkkDB = BookmarksDatabase(this)
                val bookmark = Bookmark(
                        bookId,
                        resourceIndex,
                        resourceHref,
                        progression
                )

                bookmarkkDB.bookmarks.insert(bookmark)?.let {
                    bookmark.id = it
                }
                if (bookmark.id != null) {
                    snackbar(super.resourcePager.findViewById(R.id.webView), "Bookmark added")
                } else {
                    snackbar(super.resourcePager.findViewById(R.id.webView), "Bookmark already exist")
                }

                return true
            }

            else -> return false
        }

    }


}