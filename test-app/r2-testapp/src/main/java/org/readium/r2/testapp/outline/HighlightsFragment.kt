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
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_listview.*
import kotlinx.android.synthetic.main.item_recycle_highlight.view.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Highlight
import org.readium.r2.testapp.reader.BookData
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.extensions.outlineTitle

class HighlightsFragment : Fragment(R.layout.fragment_listview) {

    lateinit var publication: Publication
    lateinit var persistence: BookData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            persistence = it.persistence
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val comparator: Comparator<Highlight> = compareBy( {it.resourceIndex },{ it.location.progression })
        val highlights = persistence.getHighlights(comparator = comparator).toMutableList()

        list_view.adapter = HighlightsAdapter(
            requireActivity(),
            highlights,
            publication,
            onDeleteHighlightRequested = { persistence.removeHighlight(it.highlightID) }
        )

        list_view.setOnItemClickListener { _, _, position, _ -> onHighlightSelected(highlights[position]) }
    }

    private fun onHighlightSelected(highlight: Highlight) {
        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(highlight.locator)
        )
    }
}

private class HighlightsAdapter(
    private val activity: Activity,
    private val items: MutableList<Highlight>,
    private val publication: Publication,
    private val onDeleteHighlightRequested: (Highlight) -> Unit
) : BaseAdapter() {

    private class ViewHolder(row: View) {
        val highlightedText: TextView = row.highlight_text
        val highlightTimestamp: TextView = row.highlight_time_stamp
        val highlightChapter: TextView = row.highlight_chapter
        val highlightOverflow: ImageView = row.highlight_overflow
        val annotation: TextView = row.annotation
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view =
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                inflater.inflate(R.layout.item_recycle_highlight, null).also {
                    it.tag = ViewHolder(it)
                }
            } else {
                convertView
            }

        val viewHolder = view.tag as ViewHolder

        val highlight = getItem(position) as Highlight

        viewHolder.highlightChapter.text = getHighlightSpineItem(highlight.resourceHref)
        viewHolder.highlightedText.text = highlight.locatorText.highlight
        viewHolder.annotation.text = highlight.annotation

        val formattedDate = DateTime(highlight.creationDate).toString(DateTimeFormat.shortDateTime())
        viewHolder.highlightTimestamp.text = formattedDate

        viewHolder.highlightOverflow.setOnClickListener {

            val popupMenu = PopupMenu(parent?.context, viewHolder.highlightChapter)
            popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.delete) {
                    onDeleteHighlightRequested(items[position])
                    items.removeAt(position)
                    notifyDataSetChanged()
                }
                false
            }
        }

        return view
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
                return link.outlineTitle
            }
        }
        for (link in publication.readingOrder) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        return null
    }
}

