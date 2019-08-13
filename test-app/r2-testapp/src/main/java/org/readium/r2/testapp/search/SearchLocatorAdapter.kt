package org.readium.r2.testapp.search

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.readium.r2.testapp.R




/**
 * This class is an adapter for Search results' list view
 */
class SearchLocatorAdapter(context: Context, resource: Int, results: List<SearchLocator> ) : ArrayAdapter<SearchLocator>(context, resource, results) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var tmpLocator = getItem(position)
        val index = position
        var txtBefore = tmpLocator.text?.before
        txtBefore = txtBefore?.replace("!§", "\"")
        txtBefore = txtBefore?.replace("§!", "'")

        var txtAfter = tmpLocator.text?.after
        txtAfter = txtAfter?.replace("!§", "\"")
        txtAfter = txtAfter?.replace("§!", "'")

        var highlight = tmpLocator.text?.highlight

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View

        view = inflater.inflate(R.layout.search_view_adapter, null)
        view?.let {
            view.findViewById<TextView>(R.id.textView1)?.text = index.toString()
            view.findViewById<TextView>(R.id.textView2)?.setText(Html.fromHtml("$txtBefore <span style=\"background-color:#FFFF00;\"><b>$highlight</b></span> $txtAfter"))
        }

        return view
    }

}