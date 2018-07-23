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
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_r2_outline.*
//import kotlinx.android.synthetic.main.toc_item.view.*


class R2OutlineActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName
    lateinit var preferences:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_outline)
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val epubName = intent.getStringExtra("epubName")
        val publication = intent.getSerializableExtra("publication") as Publication
        val publicationIdentifier = publication.metadata.identifier

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

            val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()
            val toc_item_uri = allElements.get(position).href

            Timber.d(TAG, toc_item_uri)

            val intent = Intent()
            intent.putExtra("toc_item_uri", toc_item_uri)
            setResult(Activity.RESULT_OK, intent)
            finish()

        }
        actionBar?.setDisplayHomeAsUpEnabled(true)

    }

    fun childrenOf(parent: Link): MutableList<Link> {
        val children = mutableListOf<Link>()
        for (link in parent.children) {
            children.add(link)
            children.addAll(childrenOf(link))
        }
        return children
    }

    inner class TOCAdapter(context: Context, users: MutableList<Link>) : ArrayAdapter<Link>(context, R.layout.toc_item, users) {
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
//                myView = inflater.inflate(R.layout.toc_item, parent, false)
//                viewHolder.toc_textView = myView!!.toc_textView as TextView

                myView!!.tag = viewHolder

            } else {

                viewHolder = myView.tag as ViewHolder
            }

            viewHolder.toc_textView!!.setText(spine_item!!.title)

            return myView
        }
    }
}

