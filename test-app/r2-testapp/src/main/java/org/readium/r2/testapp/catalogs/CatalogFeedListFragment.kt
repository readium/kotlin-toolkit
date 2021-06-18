/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentCatalogFeedListBinding
import org.readium.r2.testapp.domain.model.Catalog


class CatalogFeedListFragment : Fragment() {

    private val catalogFeedListViewModel: CatalogFeedListViewModel by viewModels()
    private lateinit var catalogsAdapter: CatalogFeedListAdapter

    private var _binding: FragmentCatalogFeedListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        catalogFeedListViewModel.eventChannel.receive(this) { handleEvent(it) }
        _binding = FragmentCatalogFeedListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences =
            requireContext().getSharedPreferences("org.readium.r2.testapp", Context.MODE_PRIVATE)

        catalogsAdapter = CatalogFeedListAdapter(onLongClick = { catalog -> onLongClick(catalog) })

        binding.catalogFeedList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = catalogsAdapter
            addItemDecoration(
                VerticalSpaceItemDecoration(
                    10
                )
            )
        }

        catalogFeedListViewModel.catalogs.observe(viewLifecycleOwner, {
            catalogsAdapter.submitList(it)
        })

        val version = 2
        val VERSION_KEY = "OPDS_CATALOG_VERSION"

        if (preferences.getInt(VERSION_KEY, 0) < version) {

            preferences.edit().putInt(VERSION_KEY, version).apply()

            val oPDS2Catalog = Catalog(
                title = "OPDS 2.0 Test Catalog",
                href = "https://test.opds.io/2.0/home.json",
                type = 2
            )
            val oTBCatalog = Catalog(
                title = "Open Textbooks Catalog",
                href = "http://open.minitex.org/textbooks/",
                type = 1
            )
            val sEBCatalog = Catalog(
                title = "Standard eBooks Catalog",
                href = "https://standardebooks.org/opds/all",
                type = 1
            )

            catalogFeedListViewModel.insertCatalog(oPDS2Catalog)
            catalogFeedListViewModel.insertCatalog(oTBCatalog)
            catalogFeedListViewModel.insertCatalog(sEBCatalog)
        }

        binding.catalogFeedAddCatalogFab.setOnClickListener {
            val alertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_catalog))
                .setView(R.layout.add_catalog_dialog)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(getString(R.string.save), null)
                .show()
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = alertDialog.findViewById<EditText>(R.id.catalogTitle)
                val url = alertDialog.findViewById<EditText>(R.id.catalogUrl)
                if (TextUtils.isEmpty(title?.text)) {
                    title?.error = getString(R.string.invalid_title)
                } else if (TextUtils.isEmpty(url?.text)) {
                    url?.error = getString(R.string.invalid_url)
                } else if (!URLUtil.isValidUrl(url?.text.toString())) {
                    url?.error = getString(R.string.invalid_url)
                } else {
                    catalogFeedListViewModel.parseCatalog(
                        url?.text.toString(),
                        title?.text.toString()
                    )
                    alertDialog.dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun handleEvent(event: CatalogFeedListViewModel.Event) {
        val message =
            when (event) {
                is CatalogFeedListViewModel.Event.FeedListEvent.CatalogParseFailed -> getString(R.string.catalog_parse_error)
            }
        Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun deleteCatalogModel(catalogModelId: Long) {
        catalogFeedListViewModel.deleteCatalog(catalogModelId)
    }

    private fun onLongClick(catalog: Catalog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_catalog_title))
            .setMessage(getString(R.string.confirm_delete_catalog_text))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                catalog.id?.let { deleteCatalogModel(it) }
                dialog.dismiss()
            }
            .show()
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}