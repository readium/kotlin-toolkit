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
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
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
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.public.LCPError
import org.readium.r2.lcp.public.LCPLicense
import org.readium.r2.lcp.public.LCPService
import org.readium.r2.lcp.public.R2MakeLCPService
import org.readium.r2.shared.drm.DRM
import org.readium.r2.testapp.utils.color
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


class DRMManagementActivity : AppCompatActivity(), CoroutineScope {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private lateinit var lcpService: LCPService
    private lateinit var drmModel: DRMViewModel
    private lateinit var endTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lcpService = R2MakeLCPService(this)

        val drm = DRM(DRM.Brand.lcp)

        lcpService.retrieveLicense(intent.getStringExtra("publication")
                ?: throw Exception("publication required"), null) { license, error ->
            license?.let {
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


        val daysArray = arrayOf(1, 3, 7, 15)

        val daysInput = Spinner(this@DRMManagementActivity)
        daysInput.dropDownWidth = wrapContent

        val adapter = object : ArrayAdapter<Int>(this@DRMManagementActivity, R.layout.item_spinner_days, daysArray) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v: View? = super.getDropDownView(position, null, parent)
                // Makes the selected font appear in dark
                // If this is the selected item position
                if (position == daysInput.selectedItemPosition) {
                    v!!.setBackgroundColor(context.color(R.color.colorPrimaryDark))
                    v.findViewById<TextView>(R.id.days_spinner).setTextColor(Color.WHITE)
                } else {
                    // for other views
                    v!!.setBackgroundColor(Color.WHITE)
                    v.findViewById<TextView>(R.id.days_spinner).setTextColor(Color.BLACK)
                }
                return v
            }
        }

        daysInput.adapter = adapter
        val renewDialog = alert(Appcompat, "How many days do you wish to extend your loan ?") {
            this.customView = daysInput
            daysInput.setSelection(2)
            positiveButton("Renew") { }
            negativeButton("Cancel") { }
        }.build()

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

                                    // if a renew URL is set in the server configuration, open the web browser
                                    if ((drm.license as LCPLicense).status?.link(StatusDocument.Rel.renew)?.type == "text/html") {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = Uri.parse((drm.license as LCPLicense).status?.link(StatusDocument.Rel.renew)?.href.toString())
                                        startActivity(intent)
                                    } else {

                                        renewDialog.apply {
                                            setCancelable(false)
                                            setCanceledOnTouchOutside(false)
                                            setOnShowListener {
                                                val renewButton = getButton(AlertDialog.BUTTON_POSITIVE)
                                                renewButton.setOnClickListener {

                                                    val addDays = daysInput.selectedItem.toString().toInt()
                                                    val newEndDate = DateTime(drmModel.end).plusDays(addDays)

                                                    drmModel.renewLoan(newEndDate) { error ->
                                                        error?.let {
                                                            dismiss()
                                                            (error as LCPError).errorDescription?.let { errorDescription ->
                                                                longSnackbar(errorDescription)
                                                            }
                                                        } ?: run {
                                                            dismiss()
                                                            launch {
                                                                endTextView.text = newEndDate?.toString(DateTimeFormat.shortDateTime())
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        renewDialog.show()
                                    }
                                }
                            }.lparams(width = matchParent, height = wrapContent, weight = 1f)
                        }
                        if (drmModel.canReturnPublication) {
                            button {
                                text = context.getString(R.string.drm_label_return)
                                onClick {
                                    alert(Appcompat, "This will return the publication") {

                                        positiveButton("Return") { }
                                        negativeButton("Cancel") { }

                                    }.build().apply {
                                        setCancelable(false)
                                        setCanceledOnTouchOutside(false)
                                        setOnShowListener {
                                            val button = getButton(AlertDialog.BUTTON_POSITIVE)
                                            button.setOnClickListener {
                                                drmModel.returnPublication { error ->
                                                    error?.let {
                                                        (error as LCPError).errorDescription?.let { errorDescription ->
                                                            longSnackbar(errorDescription)
                                                        }
                                                    } ?: run {
                                                        val intent = Intent()
                                                        intent.putExtra("returned", true)
                                                        setResult(Activity.RESULT_OK, intent)
                                                        dismiss()
                                                        finish()
                                                    }
                                                }
                                            }
                                        }
                                    }.show()
                                }
                            }.lparams(width = matchParent, height = wrapContent, weight = 1f)
                        }
                    }
                }
            }
        }
    }
}
