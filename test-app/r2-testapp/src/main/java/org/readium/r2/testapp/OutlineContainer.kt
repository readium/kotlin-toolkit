/*
 * // Copyright 2018 Readium Foundation. All rights reserved.
 * // Use of this source code is governed by a BSD-style license which is detailed in the LICENSE file
 * // present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp


import android.app.ActivityGroup
import android.content.Intent
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TabHost
import org.jetbrains.anko.intentFor
import org.readium.r2.shared.Publication

class OutlineContainer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline_container)

        val publication = intent.getSerializableExtra("publication") as Publication
        title = publication.metadata.title

        val tabHost = findViewById(R.id.tabhost) as TabHost

        val lam = ActivityGroup().localActivityManager
        lam.dispatchCreate(savedInstanceState)
        tabHost.setup(lam)


        //Table of Content Tab
        var tab1: TabHost.TabSpec = tabHost.newTabSpec("Table Of Content")
        tab1.setIndicator("Table Of Content")

        val intent = Intent(this, R2OutlineActivity::class.java)
        intent.putExtra("publicationPath", this.intent.getStringExtra("publicationPath"))
        intent.putExtra("publication", this.intent.getSerializableExtra("publication") as Publication)
        intent.putExtra("epubName", this.intent.getStringExtra("epubName"))

        tab1.setContent(intent)

        //Bookmarks Tab
        var tab2: TabHost.TabSpec = tabHost.newTabSpec("Bookmarks")
        tab2.setIndicator("Bookmarks")
        tab2.setContent(Intent(this, BookmarksActivity::class.java))


        tabHost.addTab(tab1)
        tabHost.addTab(tab2)

    }

}
