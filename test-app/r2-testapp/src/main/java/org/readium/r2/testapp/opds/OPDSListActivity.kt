package org.readium.r2.testapp.opds

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.mcxiaoke.koi.ext.onClick
import com.mcxiaoke.koi.ext.onLongClick
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.design.textInputLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.readium.r2.testapp.R

class OPDSListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = OPDSDatabase(this)

        database.opds.insert(OPDSModel("Feedbooks", "http://www.feedbooks.com/catalog.atom", 1))
        database.opds.insert(OPDSModel("Open Textbooks", "http://open.minitex.org/", 1))
//        database.opds.insert(OPDSModel("OPDS Test", "http://feedbooks.github.io/opds-test-catalog/opds-2/home.json", 2))
//        database.opds.insert(OPDSModel("NYPL", "https://circulation.librarysimplified.org", 1))

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
                    alert(Appcompat, "Add OPDS Feed") {
                        var editTextTitle: EditText? = null
                        var editTextHref: EditText? = null
                        var editTextType: EditText? = null
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
                                textInputLayout {
                                    padding = dip(10)
                                    editTextType = editText {
                                        hint = "Type (1 = OPDS 1.x, 2 = OPDS 2.x) "
                                    }
                                }
                            }
                        }
                        positiveButton("Save") {

                            val opds = OPDSModel(
                                    editTextTitle!!.text.toString(),
                                    editTextHref!!.text.toString(),
                                    editTextType!!.text.toString().toInt())

                            database.opds.insert(opds)
                            list.add(opds)
                            opdsAdapter.notifyDataSetChanged()

                        }
                        negativeButton("Cancel") { }
                    }.show()
                }
            }.lparams {
                gravity = Gravity.END or Gravity.BOTTOM
                margin = dip(16)
            }
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
        val button: Button

        init {
            button = view.findViewById<View>(R.id.button) as Button
        }
    }
}
