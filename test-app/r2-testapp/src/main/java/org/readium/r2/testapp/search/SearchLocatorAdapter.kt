package org.readium.r2.testapp.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.readium.r2.testapp.R



class SearchLocatorAdapter(context: Context, resource: Int, results: List<SearchLocator> ) : ArrayAdapter<SearchLocator>(context, resource, results) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var tmpLocator = getItem(position)
        val index = position
        //val snippet = tmpLocator.text?.before + " " + tmpLocator.text?.after
        val tmp = tmpLocator.mark

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view: View

        view = inflater.inflate(R.layout.search_view_adapter, null)
        view?.let {
            view.findViewById<TextView>(R.id.textView1)?.text = index.toString()
            view.findViewById<TextView>(R.id.textView2)?.text = tmp
        }
        return view
    }

}