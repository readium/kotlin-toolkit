package org.readium.r2.testapp.search


import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.miguelcatalan.materialsearchview.MaterialSearchView
import org.readium.r2.testapp.R



class R2SearchActivity : AppCompatActivity() {

    lateinit var mMaterialSearchVIew: MaterialSearchView
    lateinit var listView : ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_search)


        //Setting up toolbar
        var toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"))
        mMaterialSearchVIew = findViewById(R.id.searchView)


         //Setting up search listener
        mMaterialSearchVIew.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                //Use SearchInterface & get Result
                Toast.makeText(applicationContext, "Searching word : "+query,
                        Toast.LENGTH_SHORT).show();

                query?.let {
                    var searchInterface = MyMarkJSSearchInteface()
                    var results = searchInterface.search(query, applicationContext)
                }

                //Setting up adapter
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        var menuItem = menu?.findItem(R.id.searchMenu)
        mMaterialSearchVIew?.setMenuItem(menuItem)
        return super.onCreateOptionsMenu(menu)
    }



}

/*
//Setting up listView adapter
        var res1 = SearchLocator("", "", null, null,null, "Mark1", "")
        var res2 = SearchLocator("", "", null, null,null, "Mark2", "")
        var res3 = SearchLocator("", "", null, null,null, "Mark3", "")
        var res4 = SearchLocator("", "", null, null,null, "Mark4", "")
        var res5 = SearchLocator("", "", null, null,null, "Mark5", "")
        var res6 = SearchLocator("", "", null, null,null, "Mark6", "")
        var res7 = SearchLocator("", "", null, null,null, "Mark7", "")
        var res8 = SearchLocator("", "", null, null,null, "Mark8", "")
        var res9 = SearchLocator("", "", null, null,null, "Mark9", "")
        var res10 = SearchLocator("", "", null, null,null, "Mark10", "")
        var res11 = SearchLocator("", "", null, null,null, "Mark11", "")
        var res12 = SearchLocator("", "", null, null,null, "Mark12", "")
        var res13 = SearchLocator("", "", null, null,null, "Mark13", "")
        var res14 = SearchLocator("", "", null, null,null, "Mark14", "")
        var results = listOf<SearchLocator>(res1,res2,res3,res4,res5,res6,res7,res8,res9,res10,res11,res12,res13,res14)
        var resultsAdapter = SearchLocatorAdapter(applicationContext, R.layout.search_view_adapter, results)
        listView = findViewById(R.id.listView)
        listView.adapter = resultsAdapter
 */




/*
Setting up items
mMaterialSearchVIew.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
    override fun onSearchViewShown() {
        Toast.makeText(applicationContext, "Search Enabled",
                Toast.LENGTH_SHORT).show();
    }

    override fun onSearchViewClosed() {
        val arrayAdapter = ArrayAdapter<String>(this@R2SearchActivity, android.R.layout.simple_list_item_1)
        listView.setAdapter(arrayAdapter)
    }
})*/

