/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_outline_container.*
import kotlinx.android.synthetic.main.bookmark_item.view.*
import kotlinx.android.synthetic.main.list_item_toc.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.Link
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication
import kotlin.math.roundToInt


class R2OutlineActivity : AppCompatActivity() {

    private lateinit var preferences:SharedPreferences
    lateinit var bookmarkDB: BookmarksDatabase
    lateinit var locatorUtils: LocatorUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline_container)
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        locatorUtils = LocatorUtils(this)

        val tabHost = findViewById<TabHost>(R.id.tabhost)
        tabHost.setup()

        val publication = intent.getSerializableExtra("publication") as Publication

        title = publication.metadata.title


        /*
         * Retrieve the Table of Content
         */

        val tableOfContents: MutableList<Link> = publication.tableOfContents
        val allElements = mutableListOf<Link>()

        for (link in tableOfContents) {
            val children = childrenOf(link)
            // Append parent.
            allElements.add(link)
            // Append children, and their children... recursive.
            allElements.addAll(children)
        }

        val listAdapter = TOCAdapter(this, allElements)

        toc_list.adapter = listAdapter

        toc_list.setOnItemClickListener { _, _, position, _ ->

            //Link to the resource in the publication
            val tocItemUri = allElements[position].href

            val intent = Intent()
            intent.putExtra("toc_item_uri", tocItemUri)
            intent.putExtra("item_progression", 0.0)
            setResult(Activity.RESULT_OK, intent)
            finish()

        }


        /*
         * Retrieve the list of bookmarks
         */
        bookmarkDB = BookmarksDatabase(this)

        val bookID = intent.getLongExtra("bookId", -1)
        val bookmarks = locatorUtils.getBookmarks(bookID)
        val bookmarkskAdapter = BookMarksAdapter(this, bookmarks, allElements)

        bookmark_list.adapter = bookmarkskAdapter


        bookmark_list.setOnItemClickListener { _, _, position, _ ->

            //Link to the resource in the publication
            val bmkItemUri = bookmarks[position].resourceHref
            //Progression of the selected bookmark
            val bmkProgression = bookmarks[position].location!!.progression

            val intent = Intent()
            intent.putExtra("toc_item_uri", bmkItemUri)
            intent.putExtra("item_progression", bmkProgression)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        actionBar?.setDisplayHomeAsUpEnabled(true)


        // Setting up tabs

        val tabTOC: TabHost.TabSpec = tabHost.newTabSpec("Table Of Content")
        tabTOC.setIndicator("Table Of Content")
        tabTOC.setContent(R.id.toc_tab)


        val tabBookmarks: TabHost.TabSpec = tabHost.newTabSpec("Bookmarks")
        tabBookmarks.setIndicator("Bookmarks")
        tabBookmarks.setContent(R.id.bookmarks_tab)


        tabHost.addTab(tabTOC)
        tabHost.addTab(tabBookmarks)

    }

    private fun childrenOf(parent: Link): MutableList<Link> {
        val children = mutableListOf<Link>()
        for (link in parent.children) {
            children.add(link)
            children.addAll(childrenOf(link))
        }
        return children
    }



    inner class TOCAdapter(context: Context, users: MutableList<Link>) : ArrayAdapter<Link>(context, R.layout.list_item_toc, users) {
        private inner class ViewHolder {
            internal var tocTextView: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var myView = convertView

            val spineItem = getItem(position)

            val viewHolder: ViewHolder // view lookup cache stored in tag
            if (myView == null) {

                viewHolder = ViewHolder()
                val inflater = LayoutInflater.from(context)
                myView = inflater.inflate(R.layout.list_item_toc, parent, false)
                viewHolder.tocTextView = myView!!.toc_textView as TextView

                myView.tag = viewHolder

            } else {

                viewHolder = myView.tag as ViewHolder
            }

            viewHolder.tocTextView!!.text = spineItem!!.title

            return myView
        }
    }


    inner class BookMarksAdapter(val context: Context, private val bookmarks: MutableList<Locator>, private val elements: MutableList<Link>) : BaseAdapter() {

        private inner class ViewHolder {
            internal var bmkChapter: TextView? = null
            internal var bmkProgression: TextView? = null
            internal var bmkTimestamp: TextView? = null
            internal var bmkOverflow: ImageView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var bookmarkView = convertView
            val viewHolder: ViewHolder

            if(bookmarkView == null) {
                viewHolder = ViewHolder()

                val inflater = LayoutInflater.from(context)
                bookmarkView = inflater.inflate(R.layout.bookmark_item, parent, false)

                viewHolder.bmkChapter = bookmarkView!!.bmk_chapter as TextView
                viewHolder.bmkProgression = bookmarkView.bmk_progression as TextView
                viewHolder.bmkTimestamp = bookmarkView.bmk_timestamp as TextView
                viewHolder.bmkOverflow = bookmarkView.overflow as ImageView

                bookmarkView.tag = viewHolder

            } else {
                viewHolder = bookmarkView.tag as ViewHolder
            }

            val bookmark = getItem(position) as Bookmark
            
            var title = getBookSpineItem(bookmark.resourceHref)
            if(title.isNullOrEmpty()){
                title = "*Title Missing*"
            }
            val formattedProgression = "${((bookmark.progression * 100).roundToInt())}% through resource"
            val formattedDate = DateTime(bookmark.created).toString(DateTimeFormat.shortDateTime())

            viewHolder.bmkChapter!!.text = title
            viewHolder.bmkProgression!!.text = formattedProgression
            viewHolder.bmkTimestamp!!.text = formattedDate

            viewHolder.bmkOverflow?.setOnClickListener {

                val popupMenu = PopupMenu(parent?.context, viewHolder.bmkChapter)
                popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.delete) {
                        locatorUtils.deleteLocator(bookmarks[position], bookmarks)

                        //bookmarkDB.bookmarks.delete(bookmarks[position])
//                        bookmarks.removeAt(position)
                        notifyDataSetChanged()
                    }
                    false
                }
            }


            return bookmarkView
        }

        override fun getCount(): Int {
            return bookmarks.size
        }

        override fun getItem(position: Int): Any {
            return bookmarks[position]
        }

        private fun getBookSpineItem(href: String): String? {
            for (link in elements) {
                if (link.href == href) {
                    return link.title
                }
            }
            return null
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

    }

}

