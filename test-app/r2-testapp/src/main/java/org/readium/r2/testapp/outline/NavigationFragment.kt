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
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.toLocator
import org.readium.r2.testapp.databinding.FragmentListviewBinding
import org.readium.r2.testapp.databinding.ItemRecycleNavigationBinding
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.extensions.outlineTitle

/*
* Fragment to show navigation links (Table of Contents, Page lists & Landmarks)
*/
class NavigationFragment : Fragment() {

    private lateinit var publication: Publication
    private lateinit var links: List<Link>
    private lateinit var navAdapter: NavigationAdapter

    private var _binding: FragmentListviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
        }

        links = requireNotNull(requireArguments().getParcelableArrayList(LINKS_ARG))
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
        navAdapter = NavigationAdapter(onLinkSelected = { link -> onLinkSelected(link) })

        val flatLinks = mutableListOf<Pair<Int, Link>>()

        for (link in links) {
            val children = childrenOf(Pair(0, link))
            // Append parent.
            flatLinks.add(Pair(0, link))
            // Append children, and their children... recursive.
            flatLinks.addAll(children)
        }

        binding.listView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = navAdapter
        }
        navAdapter.submitList(flatLinks)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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

class NavigationAdapter(private val onLinkSelected: (Link) -> Unit) :
        ListAdapter<Pair<Int, Link>, NavigationAdapter.ViewHolder>(NavigationDiff()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemRecycleNavigationBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(val binding: ItemRecycleNavigationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<Int, Link>) {
            binding.navigationTextView.text = item.second.outlineTitle
            binding.indentation.layoutParams = LinearLayout.LayoutParams(item.first * 50, ViewGroup.LayoutParams.MATCH_PARENT)
            binding.root.setOnClickListener {
                onLinkSelected(item.second)
            }
        }
    }
}

private class NavigationDiff : DiffUtil.ItemCallback<Pair<Int, Link>>() {

    override fun areItemsTheSame(
            oldItem: Pair<Int, Link>,
            newItem: Pair<Int, Link>
    ): Boolean {
        return oldItem.first == newItem.first
                && oldItem.second == newItem.second
    }

    override fun areContentsTheSame(
            oldItem: Pair<Int, Link>,
            newItem: Pair<Int, Link>
    ): Boolean {
        return oldItem.first == newItem.first
                && oldItem.second == newItem.second
    }
}

fun childrenOf(parent: Pair<Int, Link>): MutableList<Pair<Int, Link>> {
    val indentation = parent.first + 1
    val children = mutableListOf<Pair<Int, Link>>()
    for (link in parent.second.children) {
        children.add(Pair(indentation, link))
        children.addAll(childrenOf(Pair(indentation, link)))
    }
    return children
}
