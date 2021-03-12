/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.outline

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_listview.*
import kotlinx.android.synthetic.main.item_recycle_navigation.view.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.toLocator
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.BookData
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.extensions.outlineTitle

/*
* Fragment to show navigation links (Table of Contents, Page lists & Landmarks)
*/
class NavigationFragment : Fragment(R.layout.fragment_listview) {

    private lateinit var publication: Publication
    private lateinit var persistence: BookData
    private lateinit var links: List<Link>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            persistence = it.persistence
        }

        links = requireNotNull(requireArguments().getParcelableArrayList(LINKS_ARG))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val flatLinks = mutableListOf<Pair<Int, Link>>()

        for (link in links) {
            val children = childrenOf(Pair(0, link))
            // Append parent.
            flatLinks.add(Pair(0, link))
            // Append children, and their children... recursive.
            flatLinks.addAll(children)
        }

        list_view.adapter = NavigationAdapter(requireActivity(), flatLinks.toMutableList())
        list_view.setOnItemClickListener { _, _, position, _ -> onLinkSelected(flatLinks[position].second) }
    }

    private fun onLinkSelected(link: Link) {
        val locator = link.toLocator().let {
            // progression is mandatory in some contexts
            if (it.locations.fragments.isEmpty())
                it.copyWithLocations(progression = 0.0)
            else
                it
        }

        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(locator)
        )
    }

    companion object {

        private const val LINKS_ARG = "links"

        fun newInstance(links: List<Link>) =
            NavigationFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(LINKS_ARG, if (links is ArrayList<Link>) links else ArrayList(links))
                }
            }
    }
}

private class NavigationAdapter(
    private val activity: Activity,
    private var items: MutableList<Any>
) : BaseAdapter() {

    private class ViewHolder(row: View) {
        val navigationTextView: TextView = row.navigation_textView
        val indentationView: ImageView = row.indentation
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
        val view: View =
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                inflater.inflate(R.layout.item_recycle_navigation, null).also {
                    it.tag = ViewHolder(it)
                }
            } else {
                convertView
            }

        val viewHolder = view.tag as ViewHolder

        val item = getItem(position)
        if (item is Pair<*, *>) {
            item as Pair<Int, Link>
            viewHolder.navigationTextView.text = item.second.outlineTitle
            viewHolder.indentationView.layoutParams = LinearLayout.LayoutParams(item.first * 50, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            item as Link
            viewHolder.navigationTextView.text = item.outlineTitle
        }
        return view
    }
}

fun childrenOf(parent: Pair<Int, Link>): MutableList<Pair<Int, Link>> {
    val indentation = parent.first + 1
    val children = mutableListOf<Pair<Int, Link>>()
    for (link in parent.second.children) {
        children.add(Pair(indentation,link))
        children.addAll(childrenOf(Pair(indentation,link)))
    }
    return children
}
