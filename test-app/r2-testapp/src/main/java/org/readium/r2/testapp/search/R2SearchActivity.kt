package org.readium.r2.testapp.search


import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.miguelcatalan.materialsearchview.MaterialSearchView
import org.readium.r2.shared.Publication
import android.widget.*
import org.readium.r2.testapp.R
import android.app.Activity
import android.content.Intent
import com.google.gson.Gson


/**
 * This Variable is used as a Trigger
 * It notifies current R2SearchActivity once JS is fully executed in our SearchInterface
 */
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

/**
 *
 */
class R2SearchActivity : AppCompatActivity() {

    lateinit var mMaterialSearchView: MaterialSearchView
    lateinit var listView : ListView

    // This variables are for our custom search interface
    lateinit var preferences: SharedPreferences
    lateinit var epubName: String
    lateinit var publication: Publication
    lateinit var publicationIdentifier: String
    var bookId: Long = -1
    lateinit var publicationPath: String
    lateinit var progressBar: ProgressBar


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
        var searchedKeyword = ""

        mMaterialSearchView = findViewById(R.id.searchView)
        listView = findViewById(R.id.listView)

        //Setting up toolbar
        var toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"))


        var jsExecutionCounter = 0

        //This variable is used to notify SearchActivity that the search interface fully executed its JS code and modified listview's adapter
        //This function basicaly refresh the listview once the JS is fully executed + Save results
        // HANDLE RESULTS from SearchInterface
        val bv = BooVariable()
        bv.listener = object : BooVariable.ChangeListener {
            override fun onChange() {
                jsExecutionCounter++
                var resultsAdapter = SearchLocatorAdapter(applicationContext, R.layout.search_view_adapter, bv.resultsList)
                listView.adapter = resultsAdapter


                /**
                 * TODO use jSExecutionCounter in order to only trigger R2SearchActivity once all JS is done, not at each occurence
                 */
                //Saving results + keyword only when JS is fully executed on all resources
                val editor = getPreferences(Context.MODE_PRIVATE).edit()
                val stringResults = Gson().toJson(bv.resultsList)
                editor.putString("searchResults", stringResults)
                editor.putString("keyword", searchedKeyword)
                editor.commit()

            }
        }


         //Setting up search listener
        mMaterialSearchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    //Saving searched term
                    searchedKeyword = query
                    //Initializing our custom search interfaces
                    var markJSSearchInteface = MyMarkJSSearchInteface(publication, publicationIdentifier, preferences, epubName, bv)
                    markJSSearchInteface.search(query, applicationContext)
                    //This hides keyboard
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
        //Opens new R2EpubActivity with desired resource
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
        //Setting up search bar
        menuInflater.inflate(R.menu.menu_search, menu)
        var menuItem = menu?.findItem(R.id.searchMenu)
        mMaterialSearchView?.setMenuItem(menuItem)
        menuItem?.expandActionView()
        menu?.performIdentifierAction(R.id.searchMenu, 0)

        //Loading previous results + keyword
        var tmp = getPreferences(Context.MODE_PRIVATE).getString("searchResults", null)
        if(tmp != null) {
            val gson = Gson()
            val results = gson.fromJson(tmp, Array<SearchLocator>::class.java).asList()
            var resultsAdapter = SearchLocatorAdapter(applicationContext, R.layout.search_view_adapter, results)
            listView.adapter = resultsAdapter
            var keyword = getPreferences(Context.MODE_PRIVATE).getString("keyword", null)
            mMaterialSearchView.setQuery(keyword, false)
        }
        return super.onCreateOptionsMenu(menu)
    }

}
