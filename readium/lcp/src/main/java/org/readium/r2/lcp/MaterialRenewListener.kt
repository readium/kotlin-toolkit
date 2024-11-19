/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.shared.util.Instant
import org.readium.r2.shared.util.Url

/**
 * A default implementation of the [LcpLicense.RenewListener] using Chrome Custom Tabs for
 * presenting web pages and a Material Date Picker for choosing the renew date.
 *
 * [MaterialRenewListener] must be initialized before its parent component is in a RESUMED state,
 * because it needs to register an ActivityResultLauncher. Basically, create it in your
 * Activity/Fragment's onCreate.
 *
 * @param license LCP license which will be renewed.
 * @param caller Activity or Fragment used to register the ActivityResultLauncher.
 * @param fragmentManager FragmentManager used to present the date picker.
 */
public class MaterialRenewListener(
    private val license: LcpLicense,
    private val caller: ActivityResultCaller,
    private val fragmentManager: FragmentManager,
) : LcpLicense.RenewListener {

    override suspend fun preferredEndDate(maximumDate: Instant?): Instant? = suspendCancellableCoroutine { cont ->
        val start = (license.license.rights.end ?: Instant.now()).toEpochMilliseconds()
        val end = maximumDate?.toEpochMilliseconds()

        MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(
                CalendarConstraints.Builder().apply {
                    // Restricts the choice between the license expiration date and the given
                    // maximumDate.
                    setStart(start)
                    if (end != null) {
                        setEnd(end)
                    }
                    setValidator(
                        CompositeDateValidator.allOf(
                            listOfNotNull(
                                DateValidatorPointForward.from(start),
                                end?.let { DateValidatorPointBackward.before(end) }
                            )
                        )
                    )
                }.build()
            )
            .setSelection(start)
            .build()
            .apply {
                addOnCancelListener { cont.cancel() }
                addOnNegativeButtonClickListener { cont.cancel() }

                addOnPositiveButtonClickListener { selection ->
                    cont.resume(Instant.fromEpochMilliseconds(selection))
                }
            }
            .show(fragmentManager, "MaterialRenewListener.DatePicker")
    }

    override suspend fun openWebPage(url: Url) {
        suspendCoroutine { cont ->
            webPageContinuation = cont

            webPageLauncher.launch(
                CustomTabsIntent.Builder().build().intent.apply {
                    data = Uri.parse(url.toString())
                }
            )
        }
    }

    private var webPageContinuation: Continuation<Unit>? = null

    private val webPageLauncher = caller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        webPageContinuation?.resume(Unit)
    }
}
