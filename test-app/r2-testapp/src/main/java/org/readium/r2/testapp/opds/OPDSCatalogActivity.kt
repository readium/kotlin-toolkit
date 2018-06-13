package org.readium.r2.testapp.opds

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import com.mcxiaoke.koi.ext.onClick
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.opds.OPDSParser
import org.readium.r2.shared.opds.Feed
import org.readium.r2.testapp.R
import java.net.MalformedURLException
import java.net.URL


class OPDSCatalogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val opdsModel = intent.getSerializableExtra("opdsModel") as? OPDSModel
        var feed: Promise<Feed, Exception>? = null


        opdsModel?.href.let {
            if (opdsModel?.type == 1) {
                feed = OPDSParser.parseURL(URL(it))
            } else {
                feed = OPDS2Parser.parseURL(URL(it))
            }
            title = opdsModel?.title
        } ?: run {
            feed = OPDSParser.parseURL(URL("http://www.feedbooks.com/catalog.atom"))
        }

        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_loading_feed))
        progress.show()

        feed?.successUi { result ->
            runOnUiThread {
                nestedScrollView {
                    padding = dip(10)

                    linearLayout {
                        orientation = LinearLayout.VERTICAL


                        for (navigation in result.navigation) {
                            button {
                                text = navigation.title
                                onClick {
                                    val model = OPDSModel(navigation.title!!,navigation.href.toString(), opdsModel?.type!!)
                                    try {
                                        model.href.let {
                                            if (opdsModel.type == 1) {
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

                        if (result.publications.isNotEmpty()) {
                            recyclerView {
                                layoutManager = GridAutoFitLayoutManager(act, 120)
                                adapter = RecyclerViewAdapter(act, result.publications)
                            }
                        }

                        for (group in result.groups) {
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
                                                        if (opdsModel.type == 1) {
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
                                                    if (opdsModel.type == 1) {
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
}
