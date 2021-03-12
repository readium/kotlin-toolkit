/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_search.*
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var searchResult: MutableLiveData<List<Locator>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireParentFragment()).get(SearchViewModel::class.java).let {
            searchResult = it.result
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter =  SearchResultAdapter(requireActivity(), searchResult, object : SearchResultAdapter.RecyclerViewClickListener {
            override fun recyclerViewListClicked(v: View, position: Int) {
                val result = Bundle().apply {
                    putParcelable(SearchFragment::class.java.name, searchResult.value!![position])
                }
                setFragmentResult(SearchFragment::class.java.name, result)
            }
        })

        searchResult.observe(viewLifecycleOwner, Observer<List<Locator>> { adapter.notifyDataSetChanged() })
        search_listView.adapter = adapter
        search_listView.layoutManager = LinearLayoutManager(activity)
    }
}

class SearchViewModel : ViewModel() {
    val result: MutableLiveData<List<Locator>> = MutableLiveData(emptyList())
}