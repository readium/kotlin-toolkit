/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.readium.r2.shared.extensions.putPublication
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.ItemRecycleCatalogBinding

class CatalogListAdapter :
    ListAdapter<Publication, CatalogListAdapter.ViewHolder>(PublicationListDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_recycle_catalog, parent, false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val publication = getItem(position)

        viewHolder.bind(publication)
    }

    inner class ViewHolder(private val binding: ItemRecycleCatalogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(publication: Publication) {
            binding.catalogListTitleText.text = publication.metadata.title

            publication.linkWithRel("http://opds-spec.org/image/thumbnail")?.let { link ->
                Picasso.with(binding.catalogListCoverImage.context).load(link.href)
                    .into(binding.catalogListCoverImage)
            } ?: run {
                if (publication.images.isNotEmpty()) {
                    Picasso.with(binding.catalogListCoverImage.context)
                        .load(publication.images.first().href).into(binding.catalogListCoverImage)
                }
            }

            binding.root.setOnClickListener {
                val bundle = Bundle().apply {
                    putPublication(publication)
                }
                Navigation.findNavController(it)
                    .navigate(R.id.action_navigation_catalog_to_navigation_catalog_detail, bundle)
            }
        }
    }

    private class PublicationListDiff : DiffUtil.ItemCallback<Publication>() {

        override fun areItemsTheSame(
            oldItem: Publication,
            newItem: Publication
        ): Boolean {
            return oldItem.metadata.identifier == newItem.metadata.identifier
        }

        override fun areContentsTheSame(
            oldItem: Publication,
            newItem: Publication
        ): Boolean {
            return oldItem.jsonManifest == newItem.jsonManifest
        }
    }

}