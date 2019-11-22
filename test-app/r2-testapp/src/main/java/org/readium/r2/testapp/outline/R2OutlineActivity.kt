/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.outline

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_outline.*
import kotlinx.android.synthetic.main.item_recycle_bookmark.view.*
import kotlinx.android.synthetic.main.item_recycle_outline.view.*
import kotlinx.android.synthetic.main.item_recycle_highlight.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.Link
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.*
import org.readium.r2.testapp.epub.Position
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class R2OutlineActivity : AppCompatActivity() {

    private lateinit var preferences:SharedPreferences
    lateinit var bookmarkDB: BookmarksDatabase
    lateinit var positionsDB: PositionsDatabase
    lateinit var highlightsDB: HighligtsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline)
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val tabHost = findViewById<TabHost>(R.id.tabhost)
        tabHost.setup()

        val publication = intent.getSerializableExtra("publication") as Publication

        title = publication.metadata.title


        /*
         * Retrieve the Table of Content
         */
        val tableOfContext = mutableListOf<Pair<Int,Link>>()

        val contents: MutableList<Link> = when {
            publication.tableOfContents.isNotEmpty() -> {
                publication.tableOfContents
            }
            publication.readingOrder.isNotEmpty() -> {
                publication.readingOrder
            }
            publication.images.isNotEmpty() -> {
                publication.images
            }
            else -> mutableListOf()
        }

        for (link in contents) {
            val children = childrenOf(Pair(0,link))
            // Append parent.
            tableOfContext.add(Pair(0,link))
            // Append children, and their children... recursive.
            tableOfContext.addAll(children)
        }

        val tocAdapter = NavigationAdapter(this, tableOfContext.toMutableList())

        toc_list.adapter = tocAdapter

        toc_list.setOnItemClickListener { _, _, position, _ ->
            //Link to the resource in the publication

            val resource = tableOfContext[position].second
            val resourceHref = resource.href
            val resourceType = resource.typeLink?: ""

            resourceHref?.let {
                val intent = Intent()
                
                if (resourceHref.indexOf("#") > 0) {
                    val id = resourceHref.substring(resourceHref.indexOf('#'))
                    intent.putExtra("locator", Locator(resourceHref, resourceType, publication.metadata.title, Locations(fragment = id),null))
                } else {
                    intent.putExtra("locator", Locator(resourceHref, resourceType, publication.metadata.title, Locations(progression = 0.0),null))
                }

                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }


        /*
         * Retrieve the list of bookmarks
         */
        bookmarkDB = BookmarksDatabase(this)

        val bookID = intent.getLongExtra("bookId", -1)
        val bookmarks = bookmarkDB.bookmarks.list(bookID).sortedWith(compareBy({it.resourceIndex},{ it.location.progression })).toMutableList()

        val bookmarksAdapter = BookMarksAdapter(this, bookmarks, publication)

        bookmark_list.adapter = bookmarksAdapter


        bookmark_list.setOnItemClickListener { _, _, position, _ ->

            //Link to the resource in the publication
            val bookmark = bookmarks[position]
            val resourceHref = bookmark.resourceHref
            val resourceType = bookmark.resourceType

            //Progression of the selected bookmark
            val bookmarkProgression = bookmarks[position].location.progression

            val intent = Intent()
            intent.putExtra("locator", Locator(resourceHref, resourceType, publication.metadata.title, Locations(progression = bookmarkProgression),null))
            setResult(Activity.RESULT_OK, intent)
            finish()
        }


        highlightsDB = HighligtsDatabase(this)

        val highlightID = intent.getStringExtra("highlightId")
        val highlights = highlightsDB.highlights.listAll().sortedWith(compareBy({it.resourceIndex},{ it.location.progression })).toMutableList()
        val highlightsAdapter = HighlightsAdapter(this, highlights, publication)
        highlight_list.adapter = highlightsAdapter
        highlight_list.setOnItemClickListener { _, _, position, _ ->
            //Link to the resource in the publication
            val highlight = highlights[position]
            val resourceHref = highlight.resourceHref
            val resourceType = highlight.resourceType
            //Progression of the selected bookmark
            val highlightProgression = highlights[position].location.progression
            val intent = Intent()
            intent.putExtra("locator", Locator(resourceHref, resourceType, publication.metadata.title, Locations(progression = highlightProgression),null))
            setResult(Activity.RESULT_OK, intent)
            finish()

        }

        positionsDB = PositionsDatabase(this)

        actionBar?.setDisplayHomeAsUpEnabled(true)


        // Setting up tabs

        val tabTOC: TabHost.TabSpec = tabHost.newTabSpec("Content")
        tabTOC.setIndicator(tabTOC.tag)
        tabTOC.setContent(R.id.toc_tab)


        val tabBookmarks: TabHost.TabSpec = tabHost.newTabSpec("Bookmarks")
        tabBookmarks.setIndicator(tabBookmarks.tag)
        tabBookmarks.setContent(R.id.bookmarks_tab)


        val tabHighlights: TabHost.TabSpec = tabHost.newTabSpec("Highlights")
        tabHighlights.setIndicator(tabHighlights.tag)
        tabHighlights.setContent(R.id.highlights_tab)

        when {
            publication.type == Publication.TYPE.AUDIO -> {
                tabHost.addTab(tabTOC)
                tabHost.addTab(tabBookmarks)
            }
            publication.type == Publication.TYPE.DiViNa -> tabHost.addTab(tabTOC)
            publication.type == Publication.TYPE.CBZ -> tabHost.addTab(tabTOC)
            else -> {
                tabHost.addTab(tabTOC)
                tabHost.addTab(tabBookmarks)
                tabHost.addTab(tabHighlights)
//                tabHost.addTab(tabPageList)
//                tabHost.addTab(tabLandmarks)
            }
        }
    }



    private fun childrenOf(parent: Pair<Int,Link>): MutableList<Pair<Int,Link>> {
        val indentation = parent.first + 1
        val children = mutableListOf<Pair<Int,Link>>()
        for (link in parent.second.children) {
            children.add(Pair(indentation,link))
            children.addAll(childrenOf(Pair(indentation,link)))
        }
        return children
    }



    /*
     * Adapter for navigation links (Table of Contents, Page lists & Landmarks)
     */
    inner class NavigationAdapter(var activity: Activity, private var items: MutableList<Any>) : BaseAdapter() {

        private inner class ViewHolder(row: View?) {
            var navigationTextView: TextView? = null
            var indentationView: ImageView? = null

            init {
                this.navigationTextView = row?.navigation_textView
                this.indentationView = row?.indentation
            }
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         * data set.
         * @return The data at the specified position.
         */
        override fun getItem(position: Int): Any {
            return items[position]
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            return items.size
        }


        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * [android.view.LayoutInflater.inflate]
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position The position of the item within the adapter's data set of the item whose view
         * we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         * is non-null and of an appropriate type before using. If it is not possible to convert
         * this view to display the correct data, this method can create a new view.
         * Heterogeneous lists can specify their number of view types, so that this View is
         * always of the right type (see [.getViewTypeCount] and
         * [.getItemViewType]).
         * @param parent The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View?
            val viewHolder: ViewHolder
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(R.layout.item_recycle_outline, null)
                viewHolder = ViewHolder(view)
                view?.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }
            val item = getItem(position)
            if (item is Pair<*, *>) {
                item as Pair<Int, Link>
                viewHolder.navigationTextView?.text = item.second.title
                val parameter = viewHolder.indentationView?.layoutParams
                parameter?.width = item.first * 50
            } else {
                item as Link
                viewHolder.navigationTextView?.text = item.title
            }
            return view as View
        }
    }

    inner class SyntheticPageListAdapter(var activity: Activity, private var items: MutableList<Position>) : BaseAdapter() {
        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         * data set.
         * @return The data at the specified position.
         */
        override fun getItem(position: Int): Any {
            return items[position]
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        override fun getCount(): Int {
            return items.size
        }

        private inner class ViewHolder(row: View?) {
            var navigationTextView: TextView? = null

            init {
                this.navigationTextView = row?.navigation_textView
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view: View?
            val viewHolder: ViewHolder
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(R.layout.item_recycle_outline, null)
                viewHolder = ViewHolder(view)
                view?.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            val item = getItem(position) as Position

            viewHolder.navigationTextView!!.text = "Page ${item.pageNumber}"

            return view as View
        }
    }


    inner class BookMarksAdapter(val activity: Activity, private val items: MutableList<Bookmark>, private val publication: Publication) : BaseAdapter() {

        private inner class ViewHolder(row: View?) {
            internal var bookmarkChapter: TextView? = null
            internal var bookmarkProgression: TextView? = null
            internal var bookmarkTimestamp: TextView? = null
            internal var bookmarkOverflow: ImageView? = null

            init {
                this.bookmarkChapter = row?.bookmark_chapter as TextView
                this.bookmarkProgression = row.bookmark_progression as TextView
                this.bookmarkTimestamp = row.bookmark_timestamp as TextView
                this.bookmarkOverflow = row.overflow as ImageView

            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {


            val view: View?
            val viewHolder: ViewHolder
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(R.layout.item_recycle_bookmark, null)
                viewHolder = ViewHolder(view)
                view?.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            val bookmark = getItem(position) as Bookmark
            
            var title = getBookSpineItem(bookmark.resourceHref)
            if(title.isNullOrEmpty()){
                title = "*Title Missing*"
            }
            val formattedProgression = "${((bookmark.location.progression!! * 100).roundToInt())}% through resource"
            val formattedDate = DateTime(bookmark.creationDate).toString(DateTimeFormat.shortDateTime())

            viewHolder.bookmarkChapter!!.text = title
            if (bookmark.location.progression!! > 1) {

                viewHolder.bookmarkProgression!!.text   = String.format("%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(bookmark.location.progression!!.toLong()),
                        TimeUnit.MILLISECONDS.toSeconds(bookmark.location.progression!!.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(bookmark.location.progression!!.toLong())))

            } else {
                viewHolder.bookmarkProgression!!.text = formattedProgression
            }
            viewHolder.bookmarkTimestamp!!.text = formattedDate

            viewHolder.bookmarkOverflow?.setOnClickListener {

                val popupMenu = PopupMenu(parent?.context, viewHolder.bookmarkChapter)
                popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.delete) {
                        bookmarkDB.bookmarks.delete(items[position])
                        items.removeAt(position)
                        notifyDataSetChanged()
                    }
                    false
                }
            }


            return view as View
        }

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        private fun getBookSpineItem(href: String): String? {
            for (link in publication.tableOfContents) {
                if (link.href == href) {
                    return link.title
                }
            }
            for (link in publication.readingOrder) {
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

    inner class HighlightsAdapter(val activity: Activity, private val items: MutableList<Highlight>, private val publication: Publication) : BaseAdapter() {

        private inner class ViewHolder(row: View?) {
            internal var highlightTitle: TextView? = null
            internal var annotation: TextView? = null
            internal var highlightOverflow: ImageView? = null
            internal var highlightTimestamp: TextView? = null
            internal var highlightChapter: TextView? = null

            init {
                this.highlightTitle = row?.highlight_title as TextView
                this.annotation = row.annotation as TextView
                this.highlightOverflow = row.highlight_overflow as ImageView
                this.highlightTimestamp = row.highlight_time_stamp as TextView
                this.highlightChapter = row.highlight_chapter as TextView
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {


            val view: View?
            val viewHolder: ViewHolder
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(R.layout.item_recycle_highlight, null)
                viewHolder = ViewHolder(view)
                view?.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            val highlight = getItem(position) as Highlight

            var chpater = getHighlightSpineItem(highlight.resourceHref)
            var title = highlight.locatorText.highlight
            if(title.isNullOrEmpty()){
                title = "*Title Missing*"
            }

            viewHolder.highlightChapter!!.text = chpater
            viewHolder.highlightTitle!!.text = title
            viewHolder.annotation!!.text = highlight.annotation
            val formattedDate = DateTime(highlight.creationDate).toString(DateTimeFormat.shortDateTime())
            viewHolder.highlightTimestamp!!.text = formattedDate

            return view as View
        }

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        private fun getHighlightSpineItem(href: String): String? {
            for (link in publication.tableOfContents) {
                if (link.href == href) {
                    return link.title
                }
            }
            for (link in publication.readingOrder) {
                if (link.href == href) {
                    return link.title
                }
            }
            return null
        }

    }

}

