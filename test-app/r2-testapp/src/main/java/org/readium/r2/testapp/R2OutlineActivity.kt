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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_outline_container.*
import kotlinx.android.synthetic.main.bmk_item.*
import kotlinx.android.synthetic.main.bmk_item.view.*
import kotlinx.android.synthetic.main.list_item_toc.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import timber.log.Timber
import kotlin.math.roundToInt


class R2OutlineActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName
    lateinit var preferences:SharedPreferences
    lateinit var bmkDB: BookmarksDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline_container)
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val tabHost = findViewById(R.id.tabhost) as TabHost
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

            val toc_item_uri = allElements.get(position).href

            Timber.d(TAG, toc_item_uri)

            val intent = Intent()
            intent.putExtra("toc_item_uri", toc_item_uri)
            intent.putExtra("item_progression", 0.0)
            setResult(Activity.RESULT_OK, intent)
            finish()

        }


        /*
         * Retrieve the list of bookmarks
         */
        bmkDB = BookmarksDatabase(this)

        val bkId = intent.getLongExtra("bookId", -1)
        val bmks = bmkDB.bookmarks.list(bkId)
        val bmkAdapter = BookMarksAdapter(this, bmks, allElements)

        bmk_list.adapter = bmkAdapter


        bmk_list.setOnItemClickListener { _, _, position, _ ->

            val bmks_item_uri = bmks.get(position).resourceHref

            Timber.d(TAG, bmks_item_uri)

            val intent = Intent()
            intent.putExtra("toc_item_uri", bmks_item_uri)
            intent.putExtra("item_progression", bmks.get(position).progression)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        bmk_list.setOnItemLongClickListener { _, v, position, _ ->

            //popup to confirm deletion
            val layoutInflater = LayoutInflater.from(this).inflate(R.layout.popup_delete, bmk_list, false)
            val popup = PopupWindow(this)
            popup.contentView = layoutInflater
            popup.width = bmk_item.width/4
            popup.height = bmk_item.height
            popup.isOutsideTouchable = true
            popup.isFocusable = true
            popup.showAsDropDown(v, 0, -bmk_item.height, Gravity.END)
            val delete: Button = layoutInflater.findViewById(R.id.delete) as Button

            delete.setOnClickListener {
                bmkDB.bookmarks.delete(bmks[position])
                bmks.removeAt(position)
                bmkAdapter.notifyDataSetChanged()
                popup.dismiss()
            }

            true
        }

        actionBar?.setDisplayHomeAsUpEnabled(true)



        val tab1: TabHost.TabSpec = tabHost.newTabSpec("Table Of Content")
        tab1.setIndicator("Table Of Content")
        tab1.setContent(R.id.toc_tab)


        val tab2: TabHost.TabSpec = tabHost.newTabSpec("Bookmarks")
        tab2.setIndicator("Bookmarks")
        tab2.setContent(R.id.bookmarks_tab)


        tabHost.addTab(tab1)
        tabHost.addTab(tab2)

    }

    fun childrenOf(parent: Link): MutableList<Link> {
        val children = mutableListOf<Link>()
        for (link in parent.children) {
            children.add(link)
            children.addAll(childrenOf(link))
        }
        return children
    }



    inner class TOCAdapter(context: Context, users: MutableList<Link>) : ArrayAdapter<Link>(context, R.layout.list_item_toc, users) {
        private inner class ViewHolder {
            internal var toc_textView: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var myView = convertView

            val spine_item = getItem(position)

            val viewHolder: ViewHolder // view lookup cache stored in tag
            if (myView == null) {

                viewHolder = ViewHolder()
                val inflater = LayoutInflater.from(context)
                myView = inflater.inflate(R.layout.list_item_toc, parent, false)
                viewHolder.toc_textView = myView!!.toc_textView as TextView

                myView.tag = viewHolder

            } else {

                viewHolder = myView.tag as ViewHolder
            }

            viewHolder.toc_textView!!.setText(spine_item!!.title)

            return myView
        }
    }


    inner class BookMarksAdapter(val context: Context, val bookmarks: MutableList<Bookmark>, val elements: MutableList<Link>) : BaseAdapter() {

        private inner class ViewHolder {
            internal var bmk_chapter: TextView? = null
            internal var bmk_progression: TextView? = null
            internal var bmk_timestamp: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var bookmarkView = convertView
            val viewHolder: ViewHolder

            if(bookmarkView == null) {
                viewHolder = ViewHolder()

                val inflater = LayoutInflater.from(context)
                bookmarkView = inflater.inflate(R.layout.bmk_item, parent, false)

                viewHolder.bmk_chapter = bookmarkView!!.bmk_chapter as TextView
                viewHolder.bmk_progression = bookmarkView.bmk_progression as TextView
                viewHolder.bmk_timestamp = bookmarkView.bmk_timestamp as TextView

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
            val formattedDate = DateTime(bookmark.timestamp).toString(DateTimeFormat.shortDateTime())

            viewHolder.bmk_chapter!!.text = title
            viewHolder.bmk_progression!!.text = formattedProgression
            viewHolder.bmk_timestamp!!.text = formattedDate

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

