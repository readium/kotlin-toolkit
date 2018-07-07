package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import org.jetbrains.anko.contentView
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.PUBLICATION_TYPE
import org.readium.r2.shared.Publication


class R2CbzActivity : AppCompatActivity() {

    lateinit var preferences: SharedPreferences
    lateinit var resourcePager: R2ViewPager
    var resources = arrayListOf<String>()

    lateinit var publicationPath: String
    lateinit var publication: Publication
    lateinit var cbzName: String
    lateinit var publicationIdentifier:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager) as R2ViewPager

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        cbzName = intent.getStringExtra("cbzName")
        publicationIdentifier = publication.metadata.identifier
        title = publication.metadata.title

        for (link in publication.pageList) {
            resources.add(link.href.toString())
        }

        val index = preferences.getInt( "$publicationIdentifier-document", 0)

        val adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title, PUBLICATION_TYPE.CBZ, publicationPath)

        resourcePager.adapter = adapter

        if (index == 0) {
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.setCurrentItem(resources.size - 1)
            } else {
                // The view has LTR layout
                resourcePager.setCurrentItem(index)
            }
        } else {
            resourcePager.setCurrentItem(index)
        }

        toggleActionBar()
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        // TODO we could add a thumbnail view here
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return super.onOptionsItemSelected(item)
//    }

    override fun onPause() {
        super.onPause()
        val publicationIdentifier = publication.metadata.title
        val documentIndex = resourcePager.currentItem
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
    }

    fun nextResource(v:View? = null) {
        runOnUiThread {
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() - 1)
            } else {
                // The view has LTR layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() + 1)
            }
        }
    }

    fun previousResource(v:View? = null) {
        runOnUiThread {
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() + 1)
            } else {
                // The view has LTR layout
                resourcePager.setCurrentItem(resourcePager.getCurrentItem() - 1)
            }

        }
    }

    fun toggleActionBar(v:View? = null) {
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

