/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.shared.opds.Group
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Catalog
import org.readium.r2.testapp.databinding.ItemGroupViewBinding

class GroupAdapter(
    val type: Int,
    private val setModelPublication: (Publication) -> Unit,
) :
    ListAdapter<Group, GroupAdapter.ViewHolder>(GroupDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemGroupViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val group = getItem(position)

        viewHolder.bind(group)
    }

    inner class ViewHolder(private val binding: ItemGroupViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            binding.groupViewGroupPublications.itemRecycleHeaderTitle.text = group.title
            if (group.links.size > 0) {
                binding.groupViewGroupPublications.itemRecycleMoreButton.visibility = View.VISIBLE
                binding.groupViewGroupPublications.itemRecycleMoreButton.setOnClickListener {
                    val catalog1 = Catalog(
                        href = group.links.first().href.toString(),
                        title = group.title,
                        type = type
                    )
                    val bundle = bundleOf(CatalogFeedListAdapter.CATALOGFEED to catalog1)
                    Navigation.findNavController(it)
                        .navigate(R.id.action_navigation_catalog_self, bundle)
                }
            }
            binding.groupViewGroupPublications.recyclerView.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                (layoutManager as LinearLayoutManager).orientation =
                    LinearLayoutManager.HORIZONTAL
                adapter = PublicationAdapter(setModelPublication).apply {
                    submitList(group.publications)
                }
            }
            binding.groupViewGroupLinks.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = NavigationAdapter(type).apply {
                    submitList(group.navigation)
                }
                addItemDecoration(
                    CatalogFeedListFragment.VerticalSpaceItemDecoration(
                        10
                    )
                )
            }
        }
    }

    private class GroupDiff : DiffUtil.ItemCallback<Group>() {

        override fun areItemsTheSame(
            oldItem: Group,
            newItem: Group,
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: Group,
            newItem: Group,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
