/*
 * // Copyright 2018 Readium Foundation. All rights reserved.
 * // Use of this source code is governed by a BSD-style license which is detailed in the LICENSE file
 * // present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp


import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import org.jetbrains.anko.intentFor
import org.readium.r2.navigator.*
import org.readium.r2.navigator.R


class R2EpubMenuActivity : R2EpubActivity() {

    private var menuBmk: MenuItem? = null


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(org.readium.r2.testapp.R.menu.menu_navigation, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(org.readium.r2.testapp.R.id.bmk_list)
        menuDrm?.setVisible(false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, OutlineContainer::class.java)
                intent.putExtra("publicationPath", publicationPath)
                intent.putExtra("publication", publication)
                intent.putExtra("epubName", epubName)
                startActivityForResult(intent, 2)
                return false
            }
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.toc), 0, 0, Gravity.END)
                return false
            }
            R.id.drm -> {
                startActivity(intentFor<DRMManagementActivity>("drmModel" to drmModel))
                return false
            }
            R.id.bookmark -> {
                BookmarksActivity().addBookmark()
                return false
            }

            else -> return super.onOptionsItemSelected(item)
        }

    }

}