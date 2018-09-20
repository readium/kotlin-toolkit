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
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.model.documents.LicenseDocument
import org.readium.r2.navigator.R
import org.readium.r2.shared.drm.DRMModel
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.*


class DRMManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val drmModel: DRMModel = intent.getSerializableExtra("drmModel") as DRMModel
        val lcpLicense = LcpLicense(drmModel.licensePath,true , this)


        //TODO network check
        val networkConnection = true

        if (networkConnection) {
            lcpLicense.fetchStatusDocument().get()
            lcpLicense.updateLicenseDocument().get()
        } else {
            //TODO
        }

        coordinatorLayout {
            fitsSystemWindows = true
            lparams(width = matchParent, height = matchParent)
            padding = dip(10)

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
                        text = lcpLicense.currentStatus()
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
                        text = lcpLicense.provider().toString()
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
                        text = DateTime(lcpLicense.issued()).toString(DateTimeFormat.shortDateTime())
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
                        text = DateTime(lcpLicense.lastUpdate()).toString(DateTimeFormat.shortDateTime())
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
                        text = lcpLicense.rightsPrints().toString()
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
                        text = lcpLicense.rightsCopies().toString()
                        textSize = 18f
                        gravity = Gravity.END
                    }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                }

                val start = DateTime(lcpLicense.rightsStart()).toString(DateTimeFormat.shortDateTime())?.let {
                    return@let it
                }
                val end = DateTime(lcpLicense.rightsEnd()).toString(DateTimeFormat.shortDateTime())?.let {
                    return@let it
                }
                val potentialRightsEnd = DateTime(lcpLicense.status?.potentialRightsEndDate()).toString(DateTimeFormat.shortDateTime())?.let {
                    return@let it
                }

                if ((start != null && end != null) && start != end) {
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
                            text = start
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
                        textView {
                            padding = dip(10)
                            text = end
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
                            text = context.getString(R.string.drm_label_potential_right_end)
                            textSize = 18f
                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                        textView {
                            padding = dip(10)
                            text = potentialRightsEnd
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
                    button {
                        text = context.getString(R.string.drm_label_renew)
                        onClick {
                            // if a renew URL is set in the server configuration, open the web browser
                            if (lcpLicense.status?.link("renew")?.type == "text/html") {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(lcpLicense.status?.link("renew")?.href.toString())
                                startActivity(intent)
                            } else {
                                val daysArray = arrayOf(1, 3, 7, 15)

                                val daysInput = Spinner(this@DRMManagementActivity)
                                daysInput.dropDownWidth = wrapContent

                                val adapter = ArrayAdapter(this@DRMManagementActivity, org.readium.r2.testapp.R.layout.days_spinner, daysArray)
                                daysInput.adapter = adapter

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
                                            val newEndDate = DateTime(lcpLicense.rightsEnd()).plusDays(addDays)

                                            if (newEndDate > DateTime(lcpLicense.status?.potentialRightsEndDate())) {
                                                runOnUiThread {
                                                    toast("New date must not exceed potential rights end date").setMargin(0f, 0.2f)
                                                }
                                            } else {
                                                lcpLicense.renewLicense(newEndDate) { renewedLicense ->

                                                    val renewedLicense = renewedLicense as LicenseDocument

                                                    lcpLicense.license = renewedLicense

                                                    renewDialog.dismiss()
                                                    recreate()
                                                }
                                            }

                                        }
                                    }
                                }
                                renewDialog.show()
                            }
                        }
                    }.lparams(width = matchParent, height = wrapContent, weight = 1f)
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
                                        lcpLicense.returnLicense() { returnedLicense ->
                                            val returnedLicense = returnedLicense as LicenseDocument

                                            lcpLicense.license = returnedLicense

                                            val intent = Intent()
                                            intent.putExtra("returned", true)
                                            setResult(Activity.RESULT_OK, intent)
                                            returnDialog.dismiss()
                                            finish()
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
