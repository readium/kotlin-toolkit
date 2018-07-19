/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("DEPRECATION")

package org.readium.r2.testapp.opds

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.ListView
import android.widget.PopupWindow
import com.commonsware.cwac.merge.MergeAdapter
import com.mcxiaoke.koi.ext.onClick
import kotlinx.android.synthetic.main.filter_row.view.*
import kotlinx.android.synthetic.main.section_header.view.*
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.shared.Link
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.testapp.R
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL


class OPDSCatalogActivity : AppCompatActivity() {

    private lateinit var facets:MutableList<Facet>
    private var parsePromise: Promise<ParseData, Exception>? = null
    private var opdsModel:OPDSModel? = null
    private var showFacetMenu = false
    private var facetPopup:PopupWindow? = null
    private lateinit var progress: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_loading_feed))

        opdsModel = intent.getSerializableExtra("opdsModel") as? OPDSModel

        opdsModel?.href.let {
            progress.show()
            try {
                parsePromise = if (opdsModel?.type == 1) {
                    OPDS1Parser.parseURL(URL(it))
                } else {
                    OPDS2Parser.parseURL(URL(it))
                }
            } catch (e: MalformedURLException) {
                progress.dismiss()
                snackbar(act.coordinatorLayout(), "Failed parsing OPDS")
            }
            title = opdsModel?.title
        }

        parsePromise?.successUi { result ->

            facets = result.feed?.facets ?: mutableListOf()

            if (facets.size>0) {
                showFacetMenu = true
            }
            invalidateOptionsMenu()

            runOnUiThread {
                nestedScrollView {
                    padding = dip(10)

                    linearLayout {
                        orientation = LinearLayout.VERTICAL

                        for (navigation in result.feed!!.navigation) {
                            button {
                                text = navigation.title
                                onClick {
                                    val model = OPDSModel(navigation.title!!, navigation.href.toString(), opdsModel?.type!!)
                                    progress.show()
                                    startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                }
                            }
                        }

                        if (result.feed!!.publications.isNotEmpty()) {
                            recyclerView {
                                layoutManager = GridAutoFitLayoutManager(act, 120)
                                adapter = RecyclerViewAdapter(act, result.feed!!.publications)
                            }
                        }

                        for (group in result.feed!!.groups) {
                            if (group.publications.isNotEmpty()) {

                                linearLayout {
                                    orientation = LinearLayout.HORIZONTAL
                                    padding = dip(10)
                                    bottomPadding = dip(5)
                                    lparams(width = matchParent, height = wrapContent)
                                    weightSum = 2f
                                    textView {
                                        text = group.title
                                    }.lparams(width = wrapContent, height = wrapContent, weight = 1f)

                                    if (group.links.size > 0) {
                                        textView {
                                            text = context.getString(R.string.opds_list_more)
                                            gravity = Gravity.END
                                            onClick {
                                                val model = OPDSModel(group.title,group.links.first().href.toString(), opdsModel?.type!!)
                                                startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                            }
                                        }.lparams(width = wrapContent, height = wrapContent, weight = 1f)
                                    }
                                }

                                recyclerView {
                                    layoutManager = LinearLayoutManager(act)
                                    (layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
                                    adapter = RecyclerViewAdapter(act, group.publications)
                                }
                            }
                            if (group.navigation.isNotEmpty()) {
                                for (navigation in group.navigation) {
                                    button {
                                        text = navigation.title
                                        onClick {
                                            val model = OPDSModel(navigation.title!!,navigation.href.toString(), opdsModel?.type!!)
                                            startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                progress.dismiss()
            }
        }

        parsePromise?.fail {
            runOnUiThread {
                progress.dismiss()
//                snackbar(act.coordinatorLayout(), it.message!!)
            }
            Timber.e(it.message)
        }

    }

    override fun onPause() {
        super.onPause()
        progress.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filter, menu)

        return showFacetMenu
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.filter -> {
                facetPopup = facetPopUp()
                facetPopup?.showAsDropDown(this.findViewById(R.id.filter), 0, 0, Gravity.END)
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun facetPopUp(): PopupWindow {

        val layoutInflater = LayoutInflater.from(this)
        val layout = layoutInflater.inflate(R.layout.filter_window, null)
        val userSettingsPopup = PopupWindow(this)
        userSettingsPopup.contentView = layout
        userSettingsPopup.width = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.height = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.isOutsideTouchable = true
        userSettingsPopup.isFocusable = true

        val adapter = MergeAdapter()
        for (i in facets.indices) {
            adapter.addView(headerLabel(facets[i].title))
            for (link in facets[i].links) {
                adapter.addView(linkCell(link))
            }
        }

        val facetList = layout.findViewById<ListView>(R.id.facetList)
        facetList.adapter = adapter

        return userSettingsPopup
    }

    private fun headerLabel(value: String): View {
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.section_header, null) as LinearLayout
        layout.header.text = value
        return layout
    }

    private fun linkCell(link: Link?): View {
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.filter_row, null) as LinearLayout
        layout.text.text = link!!.title
        layout.count.text = link.properties.numberOfItems.toString()
        layout.setOnClickListener({
            val model = OPDSModel(link.title!!,link.href.toString(), opdsModel?.type!!)
            facetPopup?.dismiss()
            startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
        })
        return layout
    }

}
