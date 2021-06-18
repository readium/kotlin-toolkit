/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.drm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.lcp.MaterialRenewListener
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.shared.UserException
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentDrmManagementBinding
import org.readium.r2.testapp.reader.ReaderViewModel
import timber.log.Timber
import java.util.*

class DrmManagementFragment : Fragment() {

    private lateinit var model: DrmManagementViewModel

    private var _binding: FragmentDrmManagementBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val publication = ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).publication
        val license = checkNotNull(publication.lcpLicense)
        val renewListener = MaterialRenewListener(
            license = license,
            caller = this,
            fragmentManager = this.childFragmentManager
        )

        val modelFactory = LcpManagementViewModel.Factory(license, renewListener)
        model = ViewModelProvider(this, modelFactory).get(LcpManagementViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrmManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Information
        binding.drmValueLicenseType.text = model.type
        binding.drmValueState.text = model.state
        binding.drmValueProvider.text = model.provider
        binding.drmValueIssued.text = model.issued.toFormattedString()
        binding.drmValueUpdated.text = model.updated.toFormattedString()

        // Rights
        binding.drmValuePrintsLeft.text = model.printsLeft
        binding.drmValueCopiesLeft.text = model.copiesLeft

        val datesVisibility =
                if (model.start != null && model.end != null && model.start != model.end)
                    View.VISIBLE
                else
                    View.GONE

        binding.drmStart.visibility = datesVisibility
        binding.drmValueStart.text = model.start.toFormattedString()
        binding.drmEnd.visibility = datesVisibility
        binding.drmValueEnd.text = model.end?.toFormattedString()

        // Actions
        binding.drmLabelActions.visibility =
                if (model.canRenewLoan || model.canReturnPublication) View.VISIBLE else View.GONE

        binding.drmButtonRenew.run {
            visibility = if (model.canRenewLoan) View.VISIBLE else View.GONE
            setOnClickListener { onRenewLoanClicked() }
        }

        binding.drmButtonReturn.run {
            visibility = if (model.canReturnPublication) View.VISIBLE else View.GONE
            setOnClickListener { onReturnPublicationClicked() }
        }
    }

    private fun onRenewLoanClicked() {
        lifecycleScope.launch {
            model.renewLoan(this@DrmManagementFragment)
                    .onSuccess { newDate ->
                        binding.drmValueEnd.text = newDate.toFormattedString()
                    }.onFailure { exception ->
                        exception.toastUserMessage(requireView())
                    }
        }
    }

    private fun onReturnPublicationClicked() {
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.return_publication))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(getString(R.string.return_button)) { _, _ ->
                    lifecycleScope.launch {
                        model.returnPublication()
                                .onSuccess {
                                    val result = DrmManagementContract.createResult(hasReturned = true)
                                    setFragmentResult(DrmManagementContract.REQUEST_KEY, result)
                                }.onFailure { exception ->
                                    exception.toastUserMessage(requireView())
                                }
                    }
                }
                .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

private fun Date?.toFormattedString() =
        DateTime(this).toString(DateTimeFormat.shortDateTime()).orEmpty()

// FIXME: the toast is drawn behind the navigation bar
private fun Exception.toastUserMessage(view: View) {
    if (this is UserException)
        Snackbar.make(view, getUserMessage(view.context), Snackbar.LENGTH_LONG).show()

    Timber.d(this)
}
