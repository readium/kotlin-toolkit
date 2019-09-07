// TODO
/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mcxiaoke.koi.ext.onClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.longSnackbar
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.lcp.public.LCPError
import org.readium.r2.lcp.public.LCPService
import org.readium.r2.lcp.public.R2MakeLCPService
import org.readium.r2.shared.drm.DRM
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


class DRMManagementActivity : AppCompatActivity(), CoroutineScope {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    lateinit var lcpService: LCPService
    lateinit var drmModel:DRMViewModel
    lateinit var endTextView:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lcpService = R2MakeLCPService(this)

        val drm: DRM = DRM(DRM.Brand.lcp)

        lcpService.retrieveLicense(intent.getStringExtra("publication"), null) { license, error  ->
            license?.let{
                drm.license = license
                drmModel = DRMViewModel.make(drm, this)
                Timber.e(error)
                Timber.d(license.toString())
            } ?: run {
                error?.let {
                    Timber.e(error)
                }
            }
        }


        coordinatorLayout {
            fitsSystemWindows = true
            lparams(width = matchParent, height = matchParent)
            padding = dip(10)

            scrollView {
                lparams(width = matchParent, height = matchParent)

                linearLayout {
                    orientation = LinearLayout.VERTICAL
                    lparams(width = matchParent, height = matchParent)

                    textView {
                        padding = dip(10)
                        topPadding = dip(15)
                        text = context.getString(R.string.drm_information_header)
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_license_type)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = drmModel.type
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)

                    }
                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_state)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = drmModel.state
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                    }
                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_provider)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = drmModel.provider
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                    }
                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_issued)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = drmModel.issued?.toString(DateTimeFormat.shortDateTime())
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                    }
                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_updated)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
    //                        text = DateTime(lcpLicense.lastUpdate()).toString(DateTimeFormat.shortDateTime())
                            text = drmModel.updated?.toString(DateTimeFormat.shortDateTime())
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                    }

                    textView {
                        padding = dip(10)
                        topPadding = dip(15)
                        text = context.getString(R.string.drm_label_rights)
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_prints_left)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = drmModel.printsLeft
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                    }
                    linearLayout {
                        orientation = LinearLayout.HORIZONTAL
                        lparams(width = matchParent, height = wrapContent)
                        weightSum = 2f
                        textView {
                            padding = dip(10)
                            text = context.getString(R.string.drm_label_copies_left)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = drmModel.copiesLeft
                            textSize = 18f
                            gravity = Gravity.END
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                    }

                    if ((drmModel.start != null && drmModel.end != null) && drmModel.start != drmModel.end) {
                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL
                            lparams(width = matchParent, height = wrapContent)
                            weightSum = 2f
                            textView {
                                padding = dip(10)
                                text = context.getString(R.string.drm_label_start)
                                textSize = 18f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                            textView {
                                padding = dip(10)
                                text = drmModel.start?.toString(DateTimeFormat.shortDateTime())
                                textSize = 18f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        }
                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL
                            lparams(width = matchParent, height = wrapContent)
                            weightSum = 2f
                            textView {
                                padding = dip(10)
                                text = context.getString(R.string.drm_label_end)
                                textSize = 18f
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1f)

                            endTextView = textView {
                                padding = dip(10)
                                text = drmModel.end?.toString(DateTimeFormat.shortDateTime())
                                textSize = 18f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        }
                        textView {
                            padding = dip(10)
                            topPadding = dip(15)
                            text = context.getString(R.string.drm_label_actions)
                            textSize = 20f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        if (drmModel.canRenewLoan) {
                            button {
                                text = context.getString(R.string.drm_label_renew)
                                onClick {

                                    //TODO test this

                                    // if a renew URL is set in the server configuration, open the web browser
//                                    if (lcpLicense.status?.link("renew")?.type == "text/html") {
//                                        val intent = Intent(Intent.ACTION_VIEW)
//                                        intent.data = Uri.parse(lcpLicense.status?.link("renew")?.href.toString())
//                                        startActivity(intent)
//                                    } else {
                                        val daysArray = arrayOf(1, 3, 7, 15)

                                        val daysInput = Spinner(this@DRMManagementActivity)
                                        daysInput.dropDownWidth = wrapContent

                                        val adapter: SpinnerAdapter = ArrayAdapter(this@DRMManagementActivity, R.layout.item_spinner_days, daysArray)
                                        daysInput.adapter = adapter
//
                                        val renewDialog = alert(Appcompat, "How many days do you wish to extend your loan ?") {
                                            this.customView = daysInput

                                            positiveButton("Renew") { }
                                            negativeButton("Cancel") { }
                                        }.build()
                                        renewDialog.apply {
                                            setCancelable(false)
                                            setCanceledOnTouchOutside(false)
                                            setOnShowListener {
                                                daysInput.setSelection(2)
                                                val renewButton = getButton(AlertDialog.BUTTON_POSITIVE)
                                                renewButton.setOnClickListener {


                                                val addDays = daysInput.selectedItem.toString().toInt()
                                                val newEndDate = DateTime(drmModel.end).plusDays(addDays)

                                                    drmModel.renewLoan(newEndDate) {error ->
                                                        it?.let {
                                                            renewDialog.dismiss()
                                                            (error as LCPError).errorDescription?.let {errorDescription ->
                                                                longSnackbar(errorDescription)
                                                            }
                                                        } ?: run {
                                                            renewDialog.dismiss()
                                                            launch {
                                                                endTextView.text = newEndDate?.toString(DateTimeFormat.shortDateTime())
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        renewDialog.show()
//                                    }
                                }
                            }.lparams(width = matchParent, height = wrapContent, weight = 1f)
                        }
                        if (drmModel.canReturnPublication) {
                            button {
                                text = context.getString(R.string.drm_label_return)
                                onClick {
                                    val returnDialog = alert(Appcompat, "This will return the publication") {

                                        positiveButton("Return") { }
                                        negativeButton("Cancel") { }

                                    }.build()
                                    returnDialog.apply {
                                        setCancelable(false)
                                        setCanceledOnTouchOutside(false)
                                        setOnShowListener {
                                            val button = getButton(AlertDialog.BUTTON_POSITIVE)
                                            button.setOnClickListener {
                                                drmModel.returnPublication { error ->
                                                    error?.let {
                                                        val intent = Intent()
                                                        intent.putExtra("returned", true)
                                                        setResult(Activity.RESULT_OK, intent)
                                                        returnDialog.dismiss()
                                                        finish()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    returnDialog.show()
                                }
                            }.lparams(width = matchParent, height = wrapContent, weight = 1f)
                        }
                    }
                }
            }
        }
    }
}
