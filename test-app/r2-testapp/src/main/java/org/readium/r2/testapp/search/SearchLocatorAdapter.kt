package org.readium.r2.testapp.search

import android.app.Activity
import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.singleClick


/**
 * This class is an adapter for Search results' list view
 */
class SearchLocatorAdapter(private val activity: Activity, private var results: List<Locator>, private var itemListener: RecyclerViewClickListener) : RecyclerView.Adapter<SearchLocatorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = activity.applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.item_recycle_search, null)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val tmpLocator = results[position]
        val txtBefore = tmpLocator.text.before
        val txtAfter = tmpLocator.text.after
        val highlight = tmpLocator.text.highlight
        val title = tmpLocator.title

        viewHolder.chapterView.text = title
        viewHolder.textView.text = Html.fromHtml("$txtBefore<span style=\"background:yellow;\"><b>$highlight</b></span>$txtAfter")

        viewHolder.itemView.singleClick { v->
            itemListener.recyclerViewListClicked(v, position)
        }

    }

    override fun getItemCount(): Int {
        return results.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chapterView: TextView = view.findViewById<View>(R.id.chapter) as TextView
        val textView: TextView = view.findViewById<View>(R.id.text) as TextView
    }

    interface RecyclerViewClickListener {
        //this is method to handle the event when clicked on the image in Recyclerview
        fun recyclerViewListClicked(v: View, position: Int)
    }

}