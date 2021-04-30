/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.commonsware.cwac.merge.MergeAdapter
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.opds.numberOfItems
import org.readium.r2.testapp.MainActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.bookshelf.BookshelfFragment
import org.readium.r2.testapp.catalogs.CatalogFeedListAdapter.Companion.CATALOGFEED
import org.readium.r2.testapp.domain.model.Catalog
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager


class CatalogFragment : Fragment() {

    private val catalogViewModel: CatalogViewModel by viewModels()
    private lateinit var catalogListAdapter: CatalogListAdapter
    private lateinit var catalog: Catalog
    private lateinit var progressBar: ProgressBar
    private var showFacetMenu = false
    private var facetPopup: PopupWindow? = null
    private lateinit var facets: MutableList<Facet>

    // FIXME the entire way this fragment is built feels like a hack. Need a cleaner UI
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        catalogViewModel.eventChannel.receive(this) { handleEvent(it) }
        catalog = arguments?.get(CATALOGFEED) as Catalog
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        catalogListAdapter = CatalogListAdapter()
        progressBar = view.findViewById(R.id.catalog_ProgressBar)
        setHasOptionsMenu(true)
        val catalogLayout = view.findViewById<LinearLayout>(R.id.catalog_LinearLayout)

        view.findViewById<RecyclerView>(R.id.catalog_DetailList).apply {
            layoutManager = GridAutoFitLayoutManager(requireContext(), 120)
            adapter = catalogListAdapter
            addItemDecoration(
                BookshelfFragment.VerticalSpaceItemDecoration(
                    10
                )
            )
        }

        (activity as MainActivity).supportActionBar?.title = catalog.title

        // TODO this feels hacky, I don't want to parse the file if it has not changed
        if (catalogViewModel.parseData.value == null) {
            progressBar.visibility = View.VISIBLE
            catalogViewModel.parseCatalog(catalog)
        }
        catalogViewModel.parseData.observe(viewLifecycleOwner, { result ->

            facets = result.feed?.facets ?: mutableListOf()

            if (facets.size > 0) {
                showFacetMenu = true
            }
            requireActivity().invalidateOptionsMenu()

            result.feed!!.navigation.forEachIndexed { index, navigation ->
                val button = Button(requireContext())
                button.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = navigation.title
                    setOnClickListener {
                        val catalog1 = Catalog(
                            href = navigation.href,
                            title = navigation.title!!,
                            type = catalog.type
                        )
                        val bundle = bundleOf(CATALOGFEED to catalog1)
                        Navigation.findNavController(it)
                            .navigate(R.id.action_navigation_catalog_self, bundle)
                    }
                }
                catalogLayout.addView(button, index)
            }

            if (result.feed!!.publications.isNotEmpty()) {
                catalogListAdapter.submitList(result.feed!!.publications)
            }

            for (group in result.feed!!.groups) {
                if (group.publications.isNotEmpty()) {
                    val linearLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(10)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                        weightSum = 2f
                        addView(TextView(requireContext()).apply {
                            text = group.title
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        })
                        if (group.links.size > 0) {
                            addView(TextView(requireContext()).apply {
                                text = getString(R.string.catalog_list_more)
                                gravity = Gravity.END
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                                setOnClickListener {
                                    val catalog1 = Catalog(
                                        href = group.links.first().href,
                                        title = group.title,
                                        type = catalog.type
                                    )
                                    val bundle = bundleOf(CATALOGFEED to catalog1)
                                    Navigation.findNavController(it)
                                        .navigate(R.id.action_navigation_catalog_self, bundle)
                                }
                            })
                        }
                    }
                    val publicationRecyclerView = RecyclerView(requireContext()).apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        (layoutManager as LinearLayoutManager).orientation =
                            LinearLayoutManager.HORIZONTAL
                        adapter = CatalogListAdapter().apply {
                            submitList(group.publications)
                        }
                    }
                    catalogLayout.addView(linearLayout)
                    catalogLayout.addView(publicationRecyclerView)
                }
                if (group.navigation.isNotEmpty()) {
                    for (navigation in group.navigation) {
                        val button = Button(requireContext())
                        button.apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            text = navigation.title
                            setOnClickListener {
                                val catalog1 = Catalog(
                                    href = navigation.href,
                                    title = navigation.title!!,
                                    type = catalog.type
                                )
                                val bundle = bundleOf(CATALOGFEED to catalog1)
                                Navigation.findNavController(it)
                                    .navigate(R.id.action_navigation_catalog_self, bundle)
                            }
                        }
                        catalogLayout.addView(button)
                    }
                }
            }
            progressBar.visibility = View.GONE
        })
    }

    private fun handleEvent(event: CatalogViewModel.Event.FeedEvent) {
        val message =
            when (event) {
                is CatalogViewModel.Event.FeedEvent.CatalogParseFailed -> getString(R.string.failed_parsing_catalog)
            }
        progressBar.visibility = View.GONE
        Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.menu_filter, menu)
//        if (showFacetMenu) {
//            facets.let {
//                for (i in facets.indices) {
//                    val submenu = menu.addSubMenu(facets[i].title)
//                    for (link in facets[i].links) {
//                        val item = submenu.add(link.title)
//                        item.setOnMenuItemClickListener {
//                            val model = Catalog(
//                                title = link.title!!,
//                                href = link.href,
//                                type = catalog.type
//                            )
//                            true
//                        }
//                    }
//                }
//            }
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.filter -> {
//                facetPopup = facetPopUp()
//                facetPopup?.showAtLocation(requireView(), Gravity.END, 0, 0)
//                false
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
        return super.onOptionsItemSelected(item)
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

    private fun facetPopUp(): PopupWindow {

        val layoutInflater = LayoutInflater.from(requireContext())
        val layout = layoutInflater.inflate(R.layout.filter_window, null)
        val userSettingsPopup = PopupWindow(requireView())
        userSettingsPopup.contentView = layout
        userSettingsPopup.width = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.height = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.isOutsideTouchable = true
        userSettingsPopup.isFocusable = true

        val adapter = MergeAdapter()
        for (i in facets.indices) {
            adapter.addView(headerLabel(facets[i].title))
            for (link in facets[i].links) {
                adapter.addView(linkCell(link))
            }
        }

        val facetList = layout.findViewById<ListView>(R.id.facetList)
        facetList.adapter = adapter

        return userSettingsPopup
    }

    private fun headerLabel(value: String): View {
        val layout = layoutInflater.inflate(R.layout.section_header, null) as LinearLayout
        layout.findViewById<TextView>(R.id.header).text = value
        return layout
    }

    private fun linkCell(link: Link?): View {
        val layout = layoutInflater.inflate(R.layout.filter_row, null) as LinearLayout
        layout.findViewById<TextView>(R.id.text).text = link!!.title
        layout.findViewById<TextView>(R.id.count).text = link.properties.numberOfItems?.toString()
        layout.setOnClickListener {
            val model = Catalog(title = link.title!!, href = link.href, type = catalog.type)
            facetPopup?.dismiss()
        }
        return layout
    }
}