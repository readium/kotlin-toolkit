package org.readium.r2.testapp

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_outline_container.*
import kotlinx.android.synthetic.main.bmk_item.view.*

data class Booktest(val book_title: String, val spine_title: String, val progression: Double)

class BookmarksActivity: AppCompatActivity() {

    lateinit var bmkDB: BookmarksDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outline_container)

        bmkDB = BookmarksDatabase(this)

        val bmks = bmkDB.bookmarks.list()


//        val bk = mutableListOf<Booktest>()
//
//        bk.add(Booktest("Asterix", "Chapter 4", 99.00))
//        bk.add(Booktest("Deadpool", "Page 94", 0.00))
//        bk.add(Booktest("Le joueur d'échec", "Chapter 3", 3.00))
//        bk.add(Booktest("Frankenstein", "Preface", 15.47))


        val bmkAdapter = BookMarksAdapter(this, bmks)

        bmk_list.adapter = bmkAdapter

        bmk_list.setOnItemLongClickListener { _, _, position, _ ->

            bmkDB.bookmarks.delete(bmks[position])
            bmks.removeAt(position)
            bmkAdapter.notifyDataSetChanged()

            true
        }

    }


    inner class BookMarksAdapter(val context: Context, val bmkList: MutableList<Bookmark>) : BaseAdapter() {

        private inner class ViewHolder {
            internal var bmk_title: TextView? = null
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

                viewHolder.bmk_title = bmkView!!.bmk_title as TextView
                viewHolder.bmk_chapter = bmkView!!.bmk_chapter as TextView
                viewHolder.bmk_progression = bmkView.bmk_progression as TextView
                viewHolder.bmk_timestamp = bmkView!!.bmk_timestamp as TextView

                bmkView.tag = viewHolder

            } else {
                viewHolder = bmkView.tag as ViewHolder

            }

            viewHolder.bmk_title!!.setText(bookmark.pub_ref.toString())
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