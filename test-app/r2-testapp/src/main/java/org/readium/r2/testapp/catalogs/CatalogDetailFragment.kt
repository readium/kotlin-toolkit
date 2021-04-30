/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.readium.r2.shared.extensions.getPublicationOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.testapp.MainActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentCatalogDetailBinding


class CatalogDetailFragment : Fragment() {

    private var publication: Publication? = null
    private val catalogViewModel: CatalogViewModel by viewModels()

    private var _binding: FragmentCatalogDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.fragment_catalog_detail, container, false
        )
        catalogViewModel.detailChannel.receive(this) { handleEvent(it) }
        publication = arguments?.getPublicationOrNull()
        binding.publication = publication
        binding.viewModel = catalogViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.title = publication?.metadata?.title

        publication?.coverLink?.let { link ->
            Picasso.with(requireContext()).load(link.href).into(binding.catalogListCoverImage)
        } ?: run {
            if (publication?.images?.isNotEmpty() == true) {
                Picasso.with(requireContext()).load(publication!!.images.first().href)
                    .into(binding.catalogListCoverImage)
            }
        }
//        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
//            binding.coverImageView.setImageBitmap(mPublication?.cover())
//        }

        binding.catalogDetailDownloadButton.setOnClickListener {
            publication?.let { it1 ->
                catalogViewModel.downloadPublication(
                    it1
                )
            }
        }
    }

    private fun handleEvent(event: CatalogViewModel.Event.DetailEvent) {
        val message =
            when (event) {
                is CatalogViewModel.Event.DetailEvent.ImportPublicationSuccess -> getString(R.string.import_publication_success)
                is CatalogViewModel.Event.DetailEvent.ImportPublicationFailed -> getString(R.string.unable_add_pub_database)
            }
        Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
