/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.comic

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.readium.r2.navigator.divina.R2DiViNaActivity
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.R
import org.readium.r2.testapp.library.LibraryActivity
import org.readium.r2.testapp.library.activitiesLaunched
import org.readium.r2.testapp.outline.R2OutlineActivity
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


/**
 * DiViNaActivity : Extension of the R2DiViNaActivity() from navigator
 *
 * That Activity manage everything related to the menu
 *      ( Table of content, User Settings, Drm, Bookmarks )
 *
 */
class DiViNaActivity : R2DiViNaActivity(), CoroutineScope {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var menuToc: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (activitiesLaunched.incrementAndGet() > 1 || !LibraryActivity.isServerStarted) {
            finish()
        }
        super.onCreate(savedInstanceState)

        bookId = intent.getLongExtra("bookId", -1)

        toggleActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_divina, menu)
        menuToc = menu?.findItem(R.id.toc)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            val locator = data.getParcelableExtra("locator") as Locator
            if (DEBUG) Timber.d("locator href ${locator.href}")

            // Call the player's goTo function with the considered href
            divinaWebView.evaluateJavascript("if (player) { player.goTo('${locator.href}'); };", null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activitiesLaunched.getAndDecrement()
    }


}