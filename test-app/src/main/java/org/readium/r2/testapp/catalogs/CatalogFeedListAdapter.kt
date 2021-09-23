/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.ItemRecycleCatalogListBinding
import org.readium.r2.testapp.domain.model.Catalog

class CatalogFeedListAdapter(private val onLongClick: (Catalog) -> Unit) :
    ListAdapter<Catalog, CatalogFeedListAdapter.ViewHolder>(CatalogListDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_recycle_catalog_list, parent, false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val catalog = getItem(position)

        viewHolder.bind(catalog)
    }

    inner class ViewHolder(private val binding: ItemRecycleCatalogListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(catalog: Catalog) {
            binding.catalog = catalog
            binding.catalogListButton.setOnClickListener {
                val bundle = bundleOf(CATALOGFEED to catalog)
                Navigation.findNavController(it)
                    .navigate(R.id.action_navigation_catalog_list_to_navigation_catalog, bundle)
            }
            binding.catalogListButton.setOnLongClickListener {
                onLongClick(catalog)
                true
            }
        }
    }

    companion object {
        const val CATALOGFEED = "catalogFeed"
    }

    private class CatalogListDiff : DiffUtil.ItemCallback<Catalog>() {

        override fun areItemsTheSame(
            oldItem: Catalog,
            newItem: Catalog
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Catalog,
            newItem: Catalog
        ): Boolean {
            return oldItem.title == newItem.title
                    && oldItem.href == newItem.href
                    && oldItem.type == newItem.type
        }
    }

}