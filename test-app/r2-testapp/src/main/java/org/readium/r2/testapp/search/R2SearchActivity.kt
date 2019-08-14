package org.readium.r2.testapp.search


import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.miguelcatalan.materialsearchview.MaterialSearchView
import org.readium.r2.shared.Publication
import android.widget.*
import org.readium.r2.testapp.R
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import org.jetbrains.anko.intentFor
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.testapp.R2EpubActivity
import android.provider.MediaStore
import org.jetbrains.anko.Android


class BooVariable {
    var listener: ChangeListener? = null

    var resultsList = mutableListOf<SearchLocator>()
        set(list) {
            field = list
            if (listener != null) listener!!.onChange()
        }

    interface ChangeListener {
        fun onChange()
    }
}


class R2SearchActivity : AppCompatActivity() {

    lateinit var mMaterialSearchView: MaterialSearchView
    lateinit var listView : ListView

    // This is for our custom search interface
    lateinit var preferences: SharedPreferences
    lateinit var epubName: String
    lateinit var publication: Publication
    lateinit var publicationIdentifier: String
    var bookId: Long = -1
    lateinit var publicationPath: String
    lateinit var progressBar: ProgressBar

    var results = listOf<SearchLocator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_search)



        progressBar = findViewById(R.id.progressbar) as ProgressBar
        progressBar.visibility = View.INVISIBLE
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        publication = intent.getSerializableExtra("publication") as Publication
        epubName = intent.getStringExtra("epubName")
        publicationPath = intent.getStringExtra("publicationPath")
        bookId = intent.getLongExtra("bookId", -1)
        publicationIdentifier = publication.metadata.identifier



        //Setting up toolbar
        var toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"))

        mMaterialSearchView = findViewById(R.id.searchView)

        listView = findViewById(R.id.listView)

        // HANDLE RESULTS from SearchInterface
        val bv = BooVariable()
        bv.listener = object : BooVariable.ChangeListener {
            override fun onChange() {
                //Toast.makeText(this@R2SearchActivity, "Search Complete", Toast.LENGTH_LONG).show()
                var resultsAdapter = SearchLocatorAdapter(applicationContext, R.layout.search_view_adapter, bv.resultsList)
                listView.adapter = resultsAdapter
                Log.d("HTML", "CHanged")
            }
        }


         //Setting up search listener
        mMaterialSearchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                //progressBar.visibility = View.VISIBLE
                //Use SearchInterface & get Resul
                query?.let {
                    var markJSSearchInteface = MyMarkJSSearchInteface(publication, publicationIdentifier, preferences, epubName, bv)
                    markJSSearchInteface.search(query, applicationContext)
                    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        listView.setOnItemClickListener { adapterView, view, position, id ->
            val res =  adapterView.getItemAtPosition(position) as SearchLocator
            Log.d("OKOK", res.href)
            val intent = Intent()
            intent.putExtra("publicationPath", publicationPath)
            intent.putExtra("epubName", epubName)
            intent.putExtra("publication", publication)
            intent.putExtra("bookId", bookId)
            intent.putExtra("locator", res)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        var menuItem = menu?.findItem(R.id.searchMenu)
        mMaterialSearchView?.setMenuItem(menuItem)
        menuItem?.expandActionView()
        menu?.performIdentifierAction(R.id.searchMenu, 0)

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

