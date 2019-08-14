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

        mMaterialSearchView = findViewById(R.id.searchView)
        listView = findViewById(R.id.listView)

        //Setting up toolbar
        var toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"))


        //This variable is used to notify SearchActivity that the search interface fully executed its JS code and modified listview's adapter
        //This function basicaly refresh the listview once the JS is fully executed
        // HANDLE RESULTS from SearchInterface
        val bv = BooVariable()
        bv.listener = object : BooVariable.ChangeListener {
            override fun onChange() {
                var resultsAdapter = SearchLocatorAdapter(applicationContext, R.layout.search_view_adapter, bv.resultsList)
                listView.adapter = resultsAdapter
            }
        }


         //Setting up search listener
        mMaterialSearchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                //progressBar.visibility = View.VISIBLE
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


        //Setting up listener on item click
        listView.setOnItemClickListener { adapterView, view, position, id ->
            val res =  adapterView.getItemAtPosition(position) as SearchLocator
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
