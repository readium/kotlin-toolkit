package org.readium.r2.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.fragment_page.view.*
import org.readium.r2.navigator.UserSettings.*
import org.readium.r2.navigator.pager.R2PageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.pager.R2WebView
import org.readium.r2.shared.Publication


class R2EpubActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

    lateinit var preferences: SharedPreferences
    lateinit var resourcePager: R2ViewPager
    lateinit var resources: ArrayList<String>

    lateinit var publicationPath: String
    lateinit var publication: Publication
    lateinit var epubName: String
    lateinit var publicationIdentifier:String

    lateinit var userSettings: UserSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_epub)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)
        resources = ArrayList()

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        epubName = intent.getStringExtra("epubName")
        publicationIdentifier = publication.metadata.identifier

        title = publication.metadata.title

        val port = preferences.getString("$publicationIdentifier-documentPort", 0.toString()).toInt()

        for (spine in publication.spine) {
            val uri = "$BASE_URL:$port" + "/" + epubName + spine.href
            resources.add(uri)
        }

        val index = preferences.getInt( "$publicationIdentifier-document", 0)
        val progression = preferences.getString("$publicationIdentifier-documentProgression", 0.0.toString()).toDouble()

        val adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title)

        resourcePager.adapter = adapter

        userSettings = UserSettings(preferences, this)
        userSettings.resourcePager = resourcePager

        resourcePager.setCurrentItem(index)

        val appearance_pref = preferences.getString("appearance", Appearance.Default.toString()) ?: Appearance.Default.toString()
        when (appearance_pref) {
            Appearance.Default.toString() -> {
                resourcePager.setBackgroundColor(Color.parseColor("#ffffff"))
                (resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#000000"))
            }
            Appearance.Sepia.toString() -> {
                resourcePager.setBackgroundColor(Color.parseColor("#faf4e8"))
                (resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#000000"))
            }
            Appearance.Night.toString() -> {
                resourcePager.setBackgroundColor(Color.parseColor("#000000"))
                (resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#ffffff"))
            }
        }

        toggleActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toc, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publicationPath", publicationPath)
                intent.putExtra("publication", publication)
                intent.putExtra("epubName", epubName)
                startActivityForResult(intent, 2)
                return false
            }
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.settings), 0, 0)
                return false;
            }

            else -> return super.onOptionsItemSelected(item)
        }

    }

    override fun onPause() {
        super.onPause()
        val publicationIdentifier = publication.metadata.identifier
        val documentIndex = resourcePager.getCurrentItem()
        val progression = resourcePager.webView.progression
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
        preferences.edit().putString("$publicationIdentifier-documentProgression", progression.toString()).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val spine_item_index: Int = data.getIntExtra("spine_item_index", 0)
                resourcePager.setCurrentItem(spine_item_index)
                preferences.edit().putString("$publicationIdentifier-documentProgression", 0.0.toString()).apply()
                if (supportActionBar!!.isShowing) {
                    resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            or View.SYSTEM_UI_FLAG_IMMERSIVE)

                }
            }
        }
    }


    fun nextResource() {
        runOnUiThread {
            preferences.edit().putString("$publicationIdentifier-documentProgression", 0.0.toString()).apply()
            resourcePager.setCurrentItem(resourcePager.getCurrentItem() + 1)
        }
    }

    fun previousResource() {
        runOnUiThread {
            preferences.edit().putString("$publicationIdentifier-documentProgression", 1.0.toString()).apply()
            resourcePager.setCurrentItem(resourcePager.getCurrentItem() - 1)
        }
    }


    fun toggleActionBar() {
        runOnUiThread {
            if (supportActionBar!!.isShowing) {
                resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            } else {
                resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
    }
}

