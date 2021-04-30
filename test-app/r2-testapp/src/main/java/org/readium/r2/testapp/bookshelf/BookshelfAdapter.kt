/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.ItemRecycleBookBinding
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.utils.singleClick


class BookshelfAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onBookLongClick: (Book) -> Unit
) : ListAdapter<Book, BookshelfAdapter.ViewHolder>(BookListDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_recycle_book, parent, false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val book = getItem(position)

        viewHolder.bind(book)
    }

    inner class ViewHolder(private val binding: ItemRecycleBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.book = book
            binding.root.singleClick {
                onBookClick(book)
            }
            binding.root.setOnLongClickListener {
                onBookLongClick(book)
                true
            }
        }
    }

    private class BookListDiff : DiffUtil.ItemCallback<Book>() {

        override fun areItemsTheSame(
            oldItem: Book,
            newItem: Book
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Book,
            newItem: Book
        ): Boolean {
            return oldItem.title == newItem.title
                    && oldItem.href == newItem.href
                    && oldItem.author == newItem.author
                    && oldItem.identifier == newItem.identifier
                    && oldItem.progression == newItem.progression
        }
    }

}