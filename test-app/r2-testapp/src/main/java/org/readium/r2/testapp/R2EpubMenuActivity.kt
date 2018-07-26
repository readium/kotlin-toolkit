/*
 * // Copyright 2018 Readium Foundation. All rights reserved.
 * // Use of this source code is governed by a BSD-style license which is detailed in the LICENSE file
 * // present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Intent
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import org.jetbrains.anko.intentFor
import org.readium.r2.navigator.DRMManagementActivity
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.pager.R2WebView

class R2EpubMenuActivity : R2EpubActivity() {

    private var menuBmk: MenuItem? = null
    lateinit var bmkDB: BookmarksDatabase


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_navigation, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bmk_list)
        menuDrm?.setVisible(false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publicationPath", publicationPath)
                intent.putExtra("publication", publication)
                intent.putExtra("epubName", epubName)
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
                val webView = findViewById<R2WebView>(R.id.webView)
                println("#####################################################")
                println("#############     Bookmark button !     #############")
                bmkDB = BookmarksDatabase(this)
                val bmk = Bookmark(
                    publicationIdentifier,
                    resourcePager.currentItem.toLong(),
//                  preferences.getString("$publicationIdentifier-documentProgression", 0.toString()).toDouble()
                    webView.progression
                )
                println(bmk)

                bmkDB.bookmarks.insert(bmk)
                println("#####################################################")
                return true
            }

            else -> return false
        }

    }


}