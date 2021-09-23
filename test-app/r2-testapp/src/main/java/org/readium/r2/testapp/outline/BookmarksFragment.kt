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
import org.readium.r2.testapp.databinding.FragmentListviewBinding
import org.readium.r2.testapp.databinding.ItemRecycleBookmarkBinding
import org.readium.r2.testapp.domain.model.Bookmark
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.extensions.outlineTitle
import kotlin.math.roundToInt

class BookmarksFragment : Fragment() {

    lateinit var publication: Publication
    lateinit var viewModel: ReaderViewModel
    private lateinit var bookmarkAdapter: BookmarkAdapter

    private var _binding: FragmentListviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            viewModel = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookmarkAdapter = BookmarkAdapter(publication, onBookmarkDeleteRequested = { bookmark -> viewModel.deleteBookmark(bookmark.id!!) }, onBookmarkSelectedRequested = { bookmark -> onBookmarkSelected(bookmark) })
        binding.listView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookmarkAdapter
        }

        val comparator: Comparator<Bookmark> = compareBy({ it.resourceIndex }, { it.locator.locations.progression })
        viewModel.getBookmarks().observe(viewLifecycleOwner, {
            val bookmarks = it.sortedWith(comparator).toMutableList()
            bookmarkAdapter.submitList(bookmarks)
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun onBookmarkSelected(bookmark: Bookmark) {
        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(bookmark.locator)
        )
    }
}

class BookmarkAdapter(private val publication: Publication, private val onBookmarkDeleteRequested: (Bookmark) -> Unit, private val onBookmarkSelectedRequested: (Bookmark) -> Unit) :
        ListAdapter<Bookmark, BookmarkAdapter.ViewHolder>(BookmarksDiff()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemRecycleBookmarkBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(val binding: ItemRecycleBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark) {
            val title = getBookSpineItem(bookmark.resourceHref)
                    ?: "*Title Missing*"

            binding.bookmarkChapter.text = title
            bookmark.locator.locations.progression?.let { progression ->
                val formattedProgression = "${(progression * 100).roundToInt()}% through resource"
                binding.bookmarkProgression.text = formattedProgression
            }

            val formattedDate = DateTime(bookmark.creation).toString(DateTimeFormat.shortDateTime())
            binding.bookmarkTimestamp.text = formattedDate

            binding.overflow.setOnClickListener {

                val popupMenu = PopupMenu(binding.overflow.context, binding.overflow)
                popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.delete) {
                        onBookmarkDeleteRequested(bookmark)
                    }
                    false
                }
            }

            binding.root.setOnClickListener {
                onBookmarkSelectedRequested(bookmark)
            }
        }
    }

    private fun getBookSpineItem(href: String): String? {
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

private class BookmarksDiff : DiffUtil.ItemCallback<Bookmark>() {

    override fun areItemsTheSame(
            oldItem: Bookmark,
            newItem: Bookmark
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
            oldItem: Bookmark,
            newItem: Bookmark
    ): Boolean {
        return oldItem.bookId == newItem.bookId
                && oldItem.location == newItem.location
    }
}
