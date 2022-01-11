/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.shared.opds.Facet
import org.readium.r2.testapp.MainActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.bookshelf.BookshelfFragment
import org.readium.r2.testapp.catalogs.CatalogFeedListAdapter.Companion.CATALOGFEED
import org.readium.r2.testapp.databinding.FragmentCatalogBinding
import org.readium.r2.testapp.domain.model.Catalog
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.utils.viewLifecycle


class CatalogFragment : Fragment() {

    private val catalogViewModel: CatalogViewModel by viewModels()
    private lateinit var publicationAdapter: PublicationAdapter
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var navigationAdapter: NavigationAdapter
    private lateinit var catalog: Catalog
    private var showFacetMenu = false
    private lateinit var facets: MutableList<Facet>
    private var binding: FragmentCatalogBinding by viewLifecycle()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        catalogViewModel.eventChannel.receive(this) { handleEvent(it) }
        catalog = arguments?.get(CATALOGFEED) as Catalog
        binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        publicationAdapter = PublicationAdapter()
        navigationAdapter = NavigationAdapter(catalog.type)
        groupAdapter = GroupAdapter(catalog.type)
        setHasOptionsMenu(true)

        binding.catalogNavigationList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = navigationAdapter
            addItemDecoration(
                CatalogFeedListFragment.VerticalSpaceItemDecoration(
                    10
                )
            )
        }

        binding.catalogPublicationsList.apply {
            layoutManager = GridAutoFitLayoutManager(requireContext(), 120)
            adapter = publicationAdapter
            addItemDecoration(
                BookshelfFragment.VerticalSpaceItemDecoration(
                    10
                )
            )
        }

        binding.catalogGroupList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }

        (activity as MainActivity).supportActionBar?.title = catalog.title

        // TODO this feels hacky, I don't want to parse the file if it has not changed
        if (catalogViewModel.parseData.value == null) {
            binding.catalogProgressBar.visibility = View.VISIBLE
            catalogViewModel.parseCatalog(catalog)
        }
        catalogViewModel.parseData.observe(viewLifecycleOwner, { result ->

            facets = result.feed?.facets ?: mutableListOf()

            if (facets.size > 0) {
                showFacetMenu = true
            }
            requireActivity().invalidateOptionsMenu()

            navigationAdapter.submitList(result.feed!!.navigation)
            publicationAdapter.submitList(result.feed!!.publications)
            groupAdapter.submitList(result.feed!!.groups)

            binding.catalogProgressBar.visibility = View.GONE
        })
    }

    private fun handleEvent(event: CatalogViewModel.Event.FeedEvent) {
        val message =
            when (event) {
                is CatalogViewModel.Event.FeedEvent.CatalogParseFailed -> getString(R.string.failed_parsing_catalog)
            }
        binding.catalogProgressBar.visibility = View.GONE
        Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        if (showFacetMenu) {
            facets.let {
                for (i in facets.indices) {
                    val submenu = menu.addSubMenu(facets[i].title)
                    for (link in facets[i].links) {
                        val item = submenu.add(link.title)
                        item.setOnMenuItemClickListener {
                            val catalog1 = Catalog(
                                title = link.title!!,
                                href = link.href,
                                type = catalog.type
                            )
                            val bundle = bundleOf(CATALOGFEED to catalog1)
                            Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_catalog_self, bundle)
                            true
                        }
                    }
                }
            }
        }
    }
}