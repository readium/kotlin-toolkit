/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.shared.publication.Link
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Catalog
import org.readium.r2.testapp.databinding.ItemRecycleButtonBinding

class NavigationAdapter(val type: Int) :
    ListAdapter<Link, NavigationAdapter.ViewHolder>(LinkDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemRecycleButtonBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val link = getItem(position)

        viewHolder.bind(link)
    }

    inner class ViewHolder(private val binding: ItemRecycleButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(link: Link) {
            binding.catalogListButton.text = link.title
            binding.catalogListButton.setOnClickListener {
                val catalog1 = Catalog(
                    href = link.href.toString(),
                    title = link.title!!,
                    type = type
                )
                val bundle = bundleOf(CatalogFeedListAdapter.CATALOGFEED to catalog1)
                Navigation.findNavController(it)
                    .navigate(R.id.action_navigation_catalog_self, bundle)
            }
        }
    }

    private class LinkDiff : DiffUtil.ItemCallback<Link>() {

        override fun areItemsTheSame(
            oldItem: Link,
            newItem: Link,
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: Link,
            newItem: Link,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
