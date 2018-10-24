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
import kotlinx.android.synthetic.main.navcontent_item.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import kotlin.math.roundToInt
import android.widget.TextView
import com.mcxiaoke.koi.ext.timestamp
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator


class R2OutlineActivity : AppCompatActivity() {

    private lateinit var preferences:SharedPreferences
    lateinit var bookmarkDB: BookmarksDatabase
    lateinit var positionsDB: PositionsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline_container)
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

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

        val tocAdapter = NavigationAdapter(this, allElements)

        toc_list.adapter = tocAdapter

        toc_list.setOnItemClickListener { _, _, position, _ ->
            //Link to the resource in the publication
            val tocItemUri = allElements[position].href

            val intent = Intent()
            tocItemUri?.let {
                if (tocItemUri.indexOf("#") > 0) {
                    val id = tocItemUri.substring(tocItemUri.indexOf('#'))
                    intent.putExtra("locator", Locator(tocItemUri, timestamp(), publication.metadata.title, Locations(id = id),null))
                } else {
                    intent.putExtra("locator", Locator(tocItemUri, timestamp(), publication.metadata.title, Locations(progression = 0.0),null))
                }
            }

            setResult(Activity.RESULT_OK, intent)
            finish()
        }


        /*
         * Retrieve the list of bookmarks
         */
        bookmarkDB = BookmarksDatabase(this)

        val bookID = intent.getLongExtra("bookId", -1)
        val bookmarks = bookmarkDB.bookmarks.list(bookID).sortedWith(compareBy({it.resourceIndex},{ it.location.progression })).toMutableList()

        val bookmarksAdapter = BookMarksAdapter(this, bookmarks, allElements)

        bookmark_list.adapter = bookmarksAdapter


        bookmark_list.setOnItemClickListener { _, _, position, _ ->

            //Link to the resource in the publication
            val bookmarkUri = bookmarks[position].resourceHref
            //Progression of the selected bookmark
            val bookmarkProgression = bookmarks[position].location.progression

            val intent = Intent()
            intent.putExtra("locator", Locator(bookmarkUri, timestamp(), publication.metadata.title, Locations(progression = bookmarkProgression),null))
            setResult(Activity.RESULT_OK, intent)
            finish()
        }



        /*
         * Retrieve the page list
         */
        positionsDB = PositionsDatabase(this)
        val pageList: MutableList<Link> = publication.pageList

        if (pageList.isNotEmpty()) {
            val pageListAdapter = NavigationAdapter(this, pageList)
            page_list.adapter = pageListAdapter

            page_list.setOnItemClickListener { _, _, position, _ ->

                //Link to the resource in the publication
                val pageUri = pageList[position].href

                val intent = Intent()
                intent.putExtra("locator", Locator(pageUri!!, timestamp(), publication.metadata.title, Locations(progression = 0.0),null))
                setResult(Activity.RESULT_OK, intent)
                finish()

            }
        } else {
            val pageListArray = positionsDB.positions.getSyntheticPageList(publication.metadata.identifier)

            val syntheticPageList = Position.fromJSON(pageListArray!!)

            val syntheticPageListAdapter = SyntheticPageListAdapter(this, syntheticPageList)
            page_list.adapter = syntheticPageListAdapter

            page_list.setOnItemClickListener { _, _, position, _ ->

                //Link to the resource in the publication
                val pageUri = syntheticPageList[position].href
                val pageProgression = syntheticPageList[position].progression

                val intent = Intent()
                intent.putExtra("locator", Locator(pageUri!!, timestamp(), publication.metadata.title, Locations(progression = pageProgression),null))
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }


        /*
         * Retrieve the landmarks
         */
        val landmarks: MutableList<Link> = publication.landmarks

        val landmarksAdapter = NavigationAdapter(this, landmarks)
        landmarks_list.adapter = landmarksAdapter

        landmarks_list.setOnItemClickListener { _, _, position, _ ->

            //Link to the resource in the publication
            val landmarkUri = landmarks[position].href

            val intent = Intent()
            intent.putExtra("locator", Locator(landmarkUri!!, timestamp(), publication.metadata.title, Locations(progression = 0.0),null))
            setResult(Activity.RESULT_OK, intent)
            finish()

        }

        actionBar?.setDisplayHomeAsUpEnabled(true)


        // Setting up tabs

        val tabTOC: TabHost.TabSpec = tabHost.newTabSpec("Content")
        tabTOC.setIndicator(tabTOC.tag)
        tabTOC.setContent(R.id.toc_tab)


        val tabBookmarks: TabHost.TabSpec = tabHost.newTabSpec("Bookmarks")
        tabBookmarks.setIndicator(tabBookmarks.tag)
        tabBookmarks.setContent(R.id.bookmarks_tab)


        val tabPageList: TabHost.TabSpec = tabHost.newTabSpec("Page List")
        tabPageList.setIndicator(tabPageList.tag)
        tabPageList.setContent(R.id.pagelists_tab)


        val tabLandmarks: TabHost.TabSpec = tabHost.newTabSpec("Landmarks")
        tabLandmarks.setIndicator(tabLandmarks.tag)
        tabLandmarks.setContent(R.id.landmarks_tab)


        tabHost.addTab(tabTOC)
        tabHost.addTab(tabBookmarks)
        tabHost.addTab(tabPageList)
        tabHost.addTab(tabLandmarks)

    }



    private fun childrenOf(parent: Link): MutableList<Link> {
        val children = mutableListOf<Link>()
        for (link in parent.children) {
            children.add(link)
            children.addAll(childrenOf(link))
        }
        return children
    }




    /*
     * Adapter for navigation links (Table of Contents, Page lists & Landmarks)
     */
    inner class NavigationAdapter(context: Context, users: MutableList<Link>) : ArrayAdapter<Link>(context, R.layout.navcontent_item, users) {
        private inner class ViewHolder {
            internal var navigationTextView: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var myView = convertView

            val item = getItem(position)

            val viewHolder: ViewHolder // view lookup cache stored in tag
            if (myView == null) {

                viewHolder = ViewHolder()
                val inflater = LayoutInflater.from(context)
                myView = inflater.inflate(R.layout.navcontent_item, parent, false)
                viewHolder.navigationTextView = myView!!.navigation_textView as TextView

                myView.tag = viewHolder

            } else {

                viewHolder = myView.tag as ViewHolder
            }

            viewHolder.navigationTextView!!.text = item!!.title

            return myView
        }
    }

    inner class SyntheticPageListAdapter(context: Context, pageList: MutableList<Position>) : ArrayAdapter<Position>(context, R.layout.navcontent_item, pageList) {
        private inner class ViewHolder {
            internal var navigationTextView: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var myView = convertView

            val item = getItem(position)

            val viewHolder: ViewHolder // view lookup cache stored in tag
            if (myView == null) {

                viewHolder = ViewHolder()
                val inflater = LayoutInflater.from(context)
                myView = inflater.inflate(R.layout.navcontent_item, parent, false)
                viewHolder.navigationTextView = myView!!.navigation_textView as TextView

                myView.tag = viewHolder

            } else {

                viewHolder = myView.tag as ViewHolder
            }

            viewHolder.navigationTextView!!.text = "Page ${item.pageNumber}"

            return myView
        }
    }


    inner class BookMarksAdapter(val context: Context, private val locators: MutableList<Bookmark>, private val elements: MutableList<Link>) : BaseAdapter() {

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
            val formattedProgression = "${((bookmark.location.progression!! * 100).roundToInt())}% through resource"
            val formattedDate = DateTime(bookmark.creationDate).toString(DateTimeFormat.shortDateTime())

            viewHolder.bmkChapter!!.text = title
            viewHolder.bmkProgression!!.text = formattedProgression
            viewHolder.bmkTimestamp!!.text = formattedDate

            viewHolder.bmkOverflow?.setOnClickListener {

                val popupMenu = PopupMenu(parent?.context, viewHolder.bmkChapter)
                popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.delete) {
                        bookmarkDB.bookmarks.delete(locators[position])
                        locators.removeAt(position)
                        notifyDataSetChanged()
                    }
                    false
                }
            }


            return bookmarkView
        }

        override fun getCount(): Int {
            return locators.size
        }

        override fun getItem(position: Int): Any {
            return locators[position]
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

