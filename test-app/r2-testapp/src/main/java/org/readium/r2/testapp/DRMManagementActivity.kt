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

import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.LinearLayout
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.model.documents.LicenseDocument
import org.readium.r2.navigator.R
import org.readium.r2.shared.drm.DRMModel


class DRMManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val drmModel: DRMModel = intent.getSerializableExtra("drmModel") as DRMModel
        val lcpLicense = LcpLicense(drmModel.licensePath,true , this)

        coordinatorLayout {
            fitsSystemWindows = true
            lparams(width = matchParent, height = matchParent)
            padding = dip(10)

            linearLayout {
                orientation = LinearLayout.VERTICAL
                lparams(width = matchParent, height = matchParent)

                textView {
                    padding = dip(10)
                    topPadding = dip(30)
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
                    topPadding = dip(30)
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
                    textView {
                        padding = dip(10)
                        topPadding = dip(30)
                        text = context.getString(R.string.drm_label_actions)
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    button {
                        text = context.getString(R.string.drm_label_renew)
                        onClick {
                            lcpLicense.renewLicense() {renewedLicense ->
                                val renewedLicense = renewedLicense  as LicenseDocument
                                // TODO refresh UI
                                // TODO update license archive

                            }
                        }
                    }.lparams(width = matchParent, height = wrapContent, weight = 1f)
                    button {
                        text = context.getString(R.string.drm_label_return)
                        onClick {
                            lcpLicense.returnLicense() { returnedLicense ->
                                val returnedLicense = returnedLicense  as LicenseDocument
                                // TODO refresh UI
                                // TODO update license archive

                            }
                        }
                    }.lparams(width = matchParent, height = wrapContent, weight = 1f)
                }
            }
        }
    }
}
