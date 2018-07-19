/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.opds

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.github.kittinunf.fuel.Fuel
import com.mcxiaoke.koi.ext.onClick
import com.mcxiaoke.koi.ext.onLongClick
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.design.textInputLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.json.JSONObject
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.promise
import org.readium.r2.testapp.R
import java.net.URL


class OPDSListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = OPDSDatabase(this)

        database.opds.insert(OPDSModel("Feedbooks", "http://www.feedbooks.com/catalog.atom", 1))
        database.opds.insert(OPDSModel("Open Textbooks", "http://open.minitex.org/", 1))

        val list = database.opds.list().toMutableList()
        val opdsAdapter = OPDSViewAdapter(act, list)

        coordinatorLayout {
            fitsSystemWindows = true
            lparams(width = matchParent, height = matchParent)
            padding = dip(10)

            nestedScrollView {
                lparams(width = matchParent, height = matchParent)
                linearLayout {
                    orientation = LinearLayout.VERTICAL
                    recyclerView {
                        layoutManager = LinearLayoutManager(act)
                        (layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.VERTICAL
                        adapter = opdsAdapter
                    }
                }
            }
            floatingActionButton {
                imageResource = R.drawable.icon_plus_white
                onClick {
                    var editTextTitle: EditText? = null
                    var editTextHref: EditText? = null
                    alert(Appcompat, "Add OPDS Feed") {

                        customView {
                            verticalLayout {
                                textInputLayout {
                                    padding = dip(10)
                                    editTextTitle = editText {
                                        hint = "Title"
                                    }
                                }
                                textInputLayout {
                                    padding = dip(10)
                                    editTextHref = editText {
                                        hint = "URL"
                                    }
                                }
                            }
                        }
                        positiveButton("Save") { }
                        negativeButton("Cancel") { }

                    }.build().apply {
                        setCancelable(false)
                        setCanceledOnTouchOutside(false)
                        setOnShowListener({
                            val b = getButton(AlertDialog.BUTTON_POSITIVE)
                            b.setOnClickListener({

                                if (TextUtils.isEmpty(editTextTitle!!.text)) {
                                    editTextTitle!!.error = "Please Enter A Title."
                                    editTextTitle!!.requestFocus()
                                } else if (TextUtils.isEmpty(editTextHref!!.text)) {
                                    editTextHref!!.error = "Please Enter A URL."
                                    editTextHref!!.requestFocus()
                                } else if (!URLUtil.isValidUrl(editTextHref!!.text.toString())) {
                                    editTextHref!!.error = "Please Enter A Valid URL."
                                    editTextHref!!.requestFocus()
                                } else {
                                    val parseData: Promise<ParseData, Exception>?
                                    parseData = parseURL(URL(editTextHref!!.text.toString()))
                                    parseData.successUi {
                                        val opds = OPDSModel(
                                                editTextTitle!!.text.toString(),
                                                editTextHref!!.text.toString(),
                                                it.type)
                                        database.opds.insert(opds)
                                        list.add(opds)
                                        opdsAdapter.notifyDataSetChanged()
                                        dismiss()
                                    }
                                    parseData.failUi {
                                        editTextHref!!.error = "Please Enter A Valid OPDS Feed URL."
                                        editTextHref!!.requestFocus()
                                    }
                                }
                            })
                        })

                    }.show()
                }
            }.lparams {
                gravity = Gravity.END or Gravity.BOTTOM
                margin = dip(16)
            }
        }
    }

    private fun parseURL(url: URL): Promise<ParseData, Exception> {
        return Fuel.get(url.toString(), null).promise() then {
            val (_, _, result) = it
            if (isJson(result)) {
                OPDS2Parser.parse(result, url)
            } else {
                OPDS1Parser.parse(result, url)
            }
        }
    }

    private fun isJson(byteArray: ByteArray): Boolean {
        return try {
            JSONObject(String(byteArray))
            true
        } catch (e: Exception) {
            false
        }
    }
}

private class OPDSViewAdapter(private val activity: Activity, private val list: MutableList<OPDSModel>) : RecyclerView.Adapter<OPDSViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.item_recycle_opds_list, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val database = OPDSDatabase(activity)

        val opdsModel = list[position]
        viewHolder.button.text = opdsModel.title
        viewHolder.button.onClick {
            //            snackbar(viewHolder.itemView, "test")
            activity.startActivity(activity.intentFor<OPDSCatalogActivity>("opdsModel" to opdsModel))
        }

        viewHolder.button.onLongClick {
            database.opds.delete(opdsModel)
            list.remove(opdsModel)
            this.notifyDataSetChanged()
            return@onLongClick true
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: Button = view.findViewById<View>(R.id.button) as Button

    }
}
