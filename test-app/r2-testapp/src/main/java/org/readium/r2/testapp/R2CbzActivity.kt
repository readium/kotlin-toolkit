package org.readium.r2.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.jetbrains.anko.contentView
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Publication


class R2CbzActivity : AppCompatActivity() {

    lateinit var preferences: SharedPreferences
    lateinit var resourcePager: R2ViewPager
    lateinit var resources: ArrayList<String>

    lateinit var publicationPath: String
    lateinit var publication: Publication
    lateinit var cbzName: String
    lateinit var publicationIdentifier:String

    lateinit var userSettings: UserSettings
    private var menuToc: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO create ImageView fragment for CBZ
        setContentView(R.layout.activity_r2_epub)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager) as R2ViewPager


        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        cbzName = intent.getStringExtra("cbzName")
        publicationIdentifier = publication.metadata.identifier
        title = publication.metadata.title

        publication.pageList.forEach {
            resources.add(it.href.toString())
        }

        val index = preferences.getInt( "$publicationIdentifier-document", 0)

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
        toggleActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.toc), 0, 0, Gravity.END)
                false
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onPause() {
        super.onPause()
        val publicationIdentifier = publication.metadata.title
        val documentIndex = resourcePager.currentItem
        val progression = 0 //resourcePager.webView.progression
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
        preferences.edit().putString("$publicationIdentifier-documentProgression", progression.toString()).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                /*var href: String = data.getStringExtra("toc_item_uri")
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
                }*/
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

