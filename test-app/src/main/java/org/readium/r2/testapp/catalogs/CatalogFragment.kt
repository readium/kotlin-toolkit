/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.shared.opds.Facet
import org.readium.r2.testapp.MainActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.bookshelf.BookshelfFragment
import org.readium.r2.testapp.catalogs.CatalogFeedListAdapter.Companion.CATALOGFEED
import org.readium.r2.testapp.data.model.Catalog
import org.readium.r2.testapp.databinding.FragmentCatalogBinding
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.utils.viewLifecycle

class CatalogFragment : Fragment() {

    private val catalogViewModel: CatalogViewModel by activityViewModels()
    private lateinit var publicationAdapter: PublicationAdapter
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var navigationAdapter: NavigationAdapter
    private lateinit var catalog: Catalog
    private var showFacetMenu = false
    private lateinit var facets: List<Facet>
    private var binding: FragmentCatalogBinding by viewLifecycle()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        catalogViewModel.channel.receive(this) { handleEvent(it) }

        catalog = arguments?.let { BundleCompat.getParcelable(it, CATALOGFEED, Catalog::class.java) }!!
        binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        publicationAdapter = PublicationAdapter(catalogViewModel::publication::set)
        navigationAdapter = NavigationAdapter(catalog.type)
        groupAdapter = GroupAdapter(catalog.type, catalogViewModel::publication::set)

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

        catalogViewModel.parseCatalog(catalog)
        binding.catalogProgressBar.visibility = View.VISIBLE

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
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
                                            href = link.href.toString(),
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

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun handleEvent(event: CatalogViewModel.Event) {
        when (event) {
            is CatalogViewModel.Event.CatalogParseFailed -> {
                Snackbar.make(
                    requireView(),
                    getString(R.string.failed_parsing_catalog),
                    Snackbar.LENGTH_LONG
                ).show()
            }

            is CatalogViewModel.Event.CatalogParseSuccess -> {
                facets = event.result.feed?.facets ?: emptyList()

                if (facets.size > 0) {
                    showFacetMenu = true
                }
                requireActivity().invalidateOptionsMenu()

                navigationAdapter.submitList(event.result.feed!!.navigation)
                publicationAdapter.submitList(event.result.feed!!.publications)
                groupAdapter.submitList(event.result.feed!!.groups)
            }
        }
        binding.catalogProgressBar.visibility = View.GONE
    }
}
