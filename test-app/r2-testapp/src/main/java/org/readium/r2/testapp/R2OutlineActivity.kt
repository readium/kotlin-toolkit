package org.readium.r2.testapp

import android.support.v7.app.AppCompatActivity
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import timber.log.Timber
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TabHost
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_outline_container.*
import kotlinx.android.synthetic.main.bmk_item.view.*
import kotlinx.android.synthetic.main.list_item_toc.view.*


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

        val epubName = intent.getStringExtra("epubName")
        val publication = intent.getSerializableExtra("publication") as Publication
        val publicationIdentifier = publication.metadata.identifier


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

            val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()
            val toc_item_uri = allElements.get(position).href

            Timber.d(TAG, toc_item_uri)

            val intent = Intent()
            intent.putExtra("toc_item_uri", toc_item_uri)
            setResult(Activity.RESULT_OK, intent)
            finish()

        }


        /*
         * Retrieve the list of bookmarks
         */
        bmkDB = BookmarksDatabase(this)

        val bmks = bmkDB.bookmarks.list(publicationIdentifier)
        val bmkAdapter = BookMarksAdapter(this, bmks)

        bmk_list.adapter = bmkAdapter

        bmk_list.setOnItemLongClickListener { _, _, position, _ ->

            bmkDB.bookmarks.delete(bmks[position])
            bmks.removeAt(position)
            bmkAdapter.notifyDataSetChanged()

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


    inner class BookMarksAdapter(val context: Context, val bmkList: MutableList<Bookmark>) : BaseAdapter() {

        private inner class ViewHolder {
            internal var bmk_chapter: TextView? = null
            internal var bmk_progression: TextView? = null
            internal var bmk_timestamp: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var bmkView = convertView

            val viewHolder: ViewHolder

            val bookmark = getItem(position) as Bookmark

            if(bmkView == null) {
                viewHolder = ViewHolder()

                val inflater = LayoutInflater.from(context)
                bmkView = inflater.inflate(R.layout.bmk_item, parent, false)

                viewHolder.bmk_chapter = bmkView!!.bmk_chapter as TextView
                viewHolder.bmk_progression = bmkView.bmk_progression as TextView
                viewHolder.bmk_timestamp = bmkView.bmk_timestamp as TextView

                bmkView.tag = viewHolder

            } else {
                viewHolder = bmkView.tag as ViewHolder

            }

            viewHolder.bmk_chapter!!.setText(bookmark.spine_index.toString())
            viewHolder.bmk_progression!!.setText(bookmark.progression.toString())
            viewHolder.bmk_timestamp!!.setText(bookmark.timestamp)

            return bmkView
        }

        override fun getCount(): Int {
            return bmkList.size
        }

        override fun getItem(position: Int): Any {
            return bmkList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

    }

}

