package org.readium.r2.testapp.opds

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.ListView
import android.widget.PopupWindow
import com.commonsware.cwac.merge.MergeAdapter
import com.mcxiaoke.koi.async.safeExecute
import com.mcxiaoke.koi.ext.onClick
import kotlinx.android.synthetic.main.filter_row.view.*
import kotlinx.android.synthetic.main.section_header.view.*
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.opds.OPDSParser
import org.readium.r2.shared.Link
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.testapp.R
import java.net.MalformedURLException
import java.net.URL


class OPDSCatalogActivity : AppCompatActivity() {

    lateinit var facets:MutableList<Facet>
    var feed: Promise<ParseData, Exception>? = null
    var opdsModel:OPDSModel? = null
    var showFacetMenu = false;
    var facetPopup:PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        opdsModel = intent.getSerializableExtra("opdsModel") as? OPDSModel


        opdsModel?.href.let {
                feed = if (opdsModel?.type == 1) {
                OPDSParser.parseURL(URL(it))
            } else {
                OPDS2Parser.parseURL(URL(it))
            }
            title = opdsModel?.title
        }

        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_loading_feed))
        progress.show()

        feed?.successUi { result ->

            facets = result.feed?.facets ?: mutableListOf<Facet>()

            if (facets.size>0) {
                showFacetMenu = true;
            }
            invalidateOptionsMenu();

            runOnUiThread {
                nestedScrollView {
                    padding = dip(10)

                    linearLayout {
                        orientation = LinearLayout.VERTICAL


                        for (navigation in result.feed!!.navigation) {
                            button {
                                text = navigation.title
                                onClick {
                                    val model = OPDSModel(navigation.title!!,navigation.href.toString(), opdsModel?.type!!)
/*
                                    try {
                                        feed = if (opdsModel!!.type == 1) {
                                            OPDSParser.parseURL(URL(model.href))
                                        } else {
                                            OPDS2Parser.parseURL(URL(model.href))
                                        }
                                        startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                    } catch (e: MalformedURLException) {
                                        snackbar(this, "Failed parsing OPDS")
                                    }
*/
                                    val parseData = if (opdsModel!!.type == 1) {
                                        OPDSParser.parseURL(URL(model.href))
                                    } else {
                                        OPDS2Parser.parseURL(URL(model.href))
                                    }
                                    parseData.then {
                                        it.feed?.let {
                                            startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                        } ?: run {
                                            startActivity(intentFor<OPDSDetailActivity>("publication" to it.publication))
                                        }
                                    }
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
                                            text = "More..."
                                            gravity = Gravity.END
                                            onClick {
                                                val model = OPDSModel(group.title,group.links.first().href.toString(), opdsModel?.type!!)
                                                try {
                                                    model.href.let {
                                                        if (opdsModel!!.type == 1) {
                                                            feed = OPDSParser.parseURL(URL(it))
                                                        } else {
                                                            feed = OPDS2Parser.parseURL(URL(it))
                                                        }
                                                        startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                                    }
                                                } catch (e: MalformedURLException) {
                                                    snackbar(this, "Failed parsing OPDS")
                                                }
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

                                            try {
                                                model.href.let {
                                                    if (opdsModel!!.type == 1) {
                                                        feed = OPDSParser.parseURL(URL(it))
                                                    } else {
                                                        feed = OPDS2Parser.parseURL(URL(it))
                                                    }
                                                    startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                                                }
                                            } catch (e: MalformedURLException) {
                                                snackbar(this, "Failed parsing OPDS")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            progress.hide()
        }

        feed?.fail {
            Log.i("", it.message)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filter, menu)

        return showFacetMenu
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.filter -> {
                facetPopup = facetPopUp()
                facetPopup?.showAsDropDown(this.findViewById(R.id.filter), 0, 0, Gravity.END)
                return false;
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun facetPopUp(): PopupWindow {

        val layoutInflater = LayoutInflater.from(this)
        val layout = layoutInflater.inflate(R.layout.filter_window, null)
        val userSettingsPopup = PopupWindow(this)
        userSettingsPopup.setContentView(layout)
        userSettingsPopup.setWidth(ListPopupWindow.WRAP_CONTENT)
        userSettingsPopup.setHeight(ListPopupWindow.WRAP_CONTENT)
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
        facetList.setAdapter(adapter)

        return userSettingsPopup
    }

    private fun headerLabel(value: String): View {
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.section_header, null) as LinearLayout
        layout.header.setText(value)
        return layout
    }

    private fun linkCell(link: Link?): View {
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.filter_row, null) as LinearLayout
        layout.text.setText(link!!.title)
        layout.count.setText(link.properties.numberOfItems.toString())
        layout.setOnClickListener({
            val model = OPDSModel(link.title!!,link.href.toString(), opdsModel?.type!!)
            try {
                model.href.let {
                    if (opdsModel!!.type == 1) {
                        feed = OPDSParser.parseURL(URL(it))
                    } else {
                        feed = OPDS2Parser.parseURL(URL(it))
                    }

                    facetPopup?.dismiss()
                    startActivity(intentFor<OPDSCatalogActivity>("opdsModel" to model))
                }
            } catch (e: MalformedURLException) {
                snackbar(act.coordinatorLayout(), "Failed parsing OPDS")
            }
        })
        return layout
    }

}
