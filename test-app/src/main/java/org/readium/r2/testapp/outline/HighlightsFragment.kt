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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Highlight
import org.readium.r2.testapp.databinding.FragmentListviewBinding
import org.readium.r2.testapp.databinding.ItemRecycleHighlightBinding
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.viewLifecycle

class HighlightsFragment : Fragment() {

    lateinit var publication: Publication
    lateinit var viewModel: ReaderViewModel
    private lateinit var highlightAdapter: HighlightAdapter
    private var binding: FragmentListviewBinding by viewLifecycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity())[ReaderViewModel::class.java].let {
            publication = it.publication
            viewModel = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentListviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        highlightAdapter = HighlightAdapter(
            publication,
            onDeleteHighlightRequested = { highlight -> viewModel.deleteHighlight(highlight.id) },
            onHighlightSelectedRequested = { highlight -> onHighlightSelected(highlight) }
        )
        binding.listView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = highlightAdapter
        }

        viewModel.highlights
            .onEach { highlightAdapter.submitList(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun onHighlightSelected(highlight: Highlight) {
        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(highlight.locator)
        )
    }
}

class HighlightAdapter(
    private val publication: Publication,
    private val onDeleteHighlightRequested: (Highlight) -> Unit,
    private val onHighlightSelectedRequested: (Highlight) -> Unit,
) :
    ListAdapter<Highlight, HighlightAdapter.ViewHolder>(HighlightsDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemRecycleHighlightBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(val binding: ItemRecycleHighlightBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(highlight: Highlight) {
            binding.highlightChapter.text = highlight.title
            binding.highlightText.text = highlight.locator.text.highlight
            binding.annotation.text = highlight.annotation

            val formattedDate = DateTime(highlight.creation).toString(
                DateTimeFormat.shortDateTime()
            )
            binding.highlightTimeStamp.text = formattedDate

            binding.highlightOverflow.setOnClickListener {
                val popupMenu = PopupMenu(
                    binding.highlightOverflow.context,
                    binding.highlightOverflow
                )
                popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.delete) {
                        onDeleteHighlightRequested(highlight)
                    }
                    false
                }
            }
            binding.root.setOnClickListener {
                onHighlightSelectedRequested(highlight)
            }
        }
    }
}

private class HighlightsDiff : DiffUtil.ItemCallback<Highlight>() {

    override fun areItemsTheSame(oldItem: Highlight, newItem: Highlight): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Highlight, newItem: Highlight): Boolean =
        oldItem == newItem
}
