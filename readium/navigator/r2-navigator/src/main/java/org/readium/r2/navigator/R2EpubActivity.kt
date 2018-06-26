package org.readium.r2.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_page.view.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.webView
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRMMModel
import org.readium.r2.navigator.UserSettings
import timber.log.Timber


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
    var drmModel: DRMMModel? = null
    private var menuDrm: MenuItem? = null
    private var menuToc: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_epub)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)
        resources = ArrayList()

        Handler().postDelayed({
            if ( intent.getSerializableExtra("drmModel") != null) {
                drmModel = intent.getSerializableExtra("drmModel") as DRMMModel
                drmModel?.let {
                    runOnUiThread {
                        menuDrm?.setVisible(true)
                    }
                } ?: run {
                    runOnUiThread {
                        menuDrm?.setVisible(false)
                    }
                }
            }
        }, 100)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        epubName = intent.getStringExtra("epubName")
        publicationIdentifier = publication.metadata.identifier

        title = publication.metadata.title

        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()

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

        if (index == 0) {
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.setCurrentItem(resources.size - 1)
            } else {
                // The view has LTR layout
            }
        } else {
            resourcePager.setCurrentItem(index)
        }

        val appearancePref = preferences.getInt("appearance", 0)
        val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
        val textColors = mutableListOf("#000000", "#000000", "#ffffff")
        resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        (resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor(textColors[appearancePref]))
        toggleActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toc, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuDrm?.setVisible(false)
        return true
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
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.toc), 0, 0, Gravity.END)
                return false;
            }
            R.id.drm -> {
                startActivity(intentFor<DRMManagementActivity>("drmModel" to drmModel))
                return false
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
                var href: String = data.getStringExtra("toc_item_uri")
                // href is the link to the page in the toc

                if (href.indexOf("#") > 0) {
                    href = href.substring(0, href.indexOf("#"))
                }
                // Search corresponding href in the spine
                for (i in 0..publication.spine.size - 1) {
                    if (publication.spine[i].href == href) {
                        resourcePager.setCurrentItem(i)
                    }
                }
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
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() - 1)
            } else {
                // The view has LTR layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() + 1)
            }
        }
    }

    fun previousResource() {
        runOnUiThread {
            preferences.edit().putString("$publicationIdentifier-documentProgression", 1.0.toString()).apply()
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() + 1)
            } else {
                // The view has LTR layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() - 1)
            }

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

