/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.outline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.domain.model.Highlight
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.extensions.outlineTitle

class HighlightsFragment : Fragment(R.layout.fragment_listview) {

    lateinit var publication: Publication
    lateinit var viewModel: ReaderViewModel
    private lateinit var highlightAdapter: HighlightAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            viewModel = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        highlightAdapter = HighlightAdapter(publication, onDeleteHighlightRequested = { highlight -> viewModel.deleteHighlightByHighlightId(highlight.highlightId) }, onHighlightSelectedRequested = { highlight -> onHighlightSelected(highlight) })
        view.findViewById<RecyclerView>(R.id.list_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = highlightAdapter
        }

        val comparator: Comparator<Highlight> = compareBy({ it.resourceIndex }, { it.locator.locations.progression })
        viewModel.getHighlights().observe(viewLifecycleOwner, {
            val highlights = it.sortedWith(comparator).toMutableList()
            highlightAdapter.submitList(highlights)
        })
    }

    private fun onHighlightSelected(highlight: Highlight) {
        setFragmentResult(
                OutlineContract.REQUEST_KEY,
                OutlineContract.createResult(highlight.locator)
        )
    }
}

class HighlightAdapter(private val publication: Publication,
                       private val onDeleteHighlightRequested: (Highlight) -> Unit,
                       private val onHighlightSelectedRequested: (Highlight) -> Unit) :
        ListAdapter<Highlight, HighlightAdapter.ViewHolder>(HighlightsDiff()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): ViewHolder {
        return ViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_recycle_highlight, parent, false)
        )
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val row: View) : RecyclerView.ViewHolder(row) {
        private val highlightedText: TextView = row.findViewById(R.id.highlight_text)
        private val highlightTimestamp: TextView = row.findViewById(R.id.highlight_time_stamp)
        private val highlightChapter: TextView = row.findViewById(R.id.highlight_chapter)
        private val highlightOverflow: ImageView = row.findViewById(R.id.highlight_overflow)
        val annotation: TextView = row.findViewById(R.id.annotation)

        fun bind(highlight: Highlight) {
            highlightChapter.text = getHighlightSpineItem(highlight.resourceHref)
            highlightedText.text = highlight.locator.text.highlight
            annotation.text = highlight.annotation

            val formattedDate = DateTime(highlight.creation).toString(DateTimeFormat.shortDateTime())
            highlightTimestamp.text = formattedDate

            highlightOverflow.setOnClickListener {

                val popupMenu = PopupMenu(highlightOverflow.context, highlightChapter)
                popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.delete) {
                        onDeleteHighlightRequested(highlight)
                    }
                    false
                }
            }
            row.setOnClickListener {
                onHighlightSelectedRequested(highlight)
            }
        }
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

private class HighlightsDiff : DiffUtil.ItemCallback<Highlight>() {

    override fun areItemsTheSame(
            oldItem: Highlight,
            newItem: Highlight
    ): Boolean {
        return oldItem.id == newItem.id
                && oldItem.highlightId == newItem.highlightId
    }

    override fun areContentsTheSame(
            oldItem: Highlight,
            newItem: Highlight
    ): Boolean {
        return oldItem.annotation == newItem.annotation
                && oldItem.color == newItem.color
                && oldItem.annotationMarkStyle == newItem.annotationMarkStyle
    }
}
