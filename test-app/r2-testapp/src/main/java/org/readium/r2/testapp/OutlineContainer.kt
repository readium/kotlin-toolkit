/*
 * // Copyright 2018 Readium Foundation. All rights reserved.
 * // Use of this source code is governed by a BSD-style license which is detailed in the LICENSE file
 * // present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Intent
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TabHost
import org.jetbrains.anko.intentFor

class OutlineContainer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline_container)

        val tabHost = findViewById<TabHost>(R.id.tabhost)

        tabHost.setup()

        //Table of Content Tab
        var spec: TabHost.TabSpec = tabHost.newTabSpec("Table Of Content")
        spec.setContent(R.id.toc)
        spec.setIndicator("Table Of Content")
        tabHost.addTab(spec)

        //Bookmarks Tab
        spec = tabHost.newTabSpec("Bookmarks")
        spec.setContent(R.id.bookmarks)
        spec.setIndicator("Bookmarks")
        tabHost.addTab(spec)



    }

}
