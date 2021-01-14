/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.drm

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mcxiaoke.koi.ext.onClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.longSnackbar
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.UserException
import org.readium.r2.testapp.R
import java.io.File
import kotlin.coroutines.CoroutineContext


class DRMManagementActivity : AppCompatActivity(), CoroutineScope {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private lateinit var drmModel: DRMViewModel
    private lateinit var endTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pubPath = intent.getStringExtra("publication")
            ?: throw Exception("publication required")

        drmModel = runBlocking { LCPViewModel.from(File(pubPath), this@DRMManagementActivity) }
            ?: run {
                finish()
                return
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
                            text = drmModel.issued?.let { DateTime(it) }?.toString(DateTimeFormat.shortDateTime())
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
                            text = drmModel.updated?.let { DateTime(it) }?.toString(DateTimeFormat.shortDateTime())
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
                                text = drmModel.start?.let { DateTime(it) }?.toString(DateTimeFormat.shortDateTime())
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
                                text = drmModel.end?.let { DateTime(it) }?.toString(DateTimeFormat.shortDateTime())
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
                                    launch {
                                        drmModel.renewLoan()
                                            .onSuccess { newDate ->
                                                endTextView.text = newDate?.let { DateTime(it).toString(DateTimeFormat.shortDateTime()) }
                                            }.onFailure { exception ->
                                                (exception as? UserException)?.getUserMessage(this@DRMManagementActivity)
                                                    ?.let { longSnackbar(it) }
                                            }
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
                                                launch {
                                                    drmModel.returnPublication()
                                                        .onSuccess {
                                                            val intent = Intent()
                                                            intent.putExtra("returned", true)
                                                            setResult(Activity.RESULT_OK, intent)
                                                            dismiss()
                                                            finish()
                                                        }.onFailure { exception ->
                                                            (exception as? UserException)?.getUserMessage(this@DRMManagementActivity)
                                                                ?.let { longSnackbar(it) }
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
