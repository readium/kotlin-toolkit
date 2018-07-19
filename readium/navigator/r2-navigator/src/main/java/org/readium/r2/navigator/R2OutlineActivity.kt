/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_r2_outline.*
import kotlinx.android.synthetic.main.list_item_toc.view.*
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import timber.log.Timber


class R2OutlineActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_outline)
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val publication = intent.getSerializableExtra("publication") as Publication

        title = publication.metadata.title

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

        list.adapter = listAdapter

        list.setOnItemClickListener { _, _, position, _ ->

            val tocItemUri = allElements[position].href

            Timber.d(TAG, tocItemUri)

            val intent = Intent()
            intent.putExtra("toc_item_uri", tocItemUri)
            setResult(Activity.RESULT_OK, intent)
            finish()

        }
        actionBar?.setDisplayHomeAsUpEnabled(true)

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
}
