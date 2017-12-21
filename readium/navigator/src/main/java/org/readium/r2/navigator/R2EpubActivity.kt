package org.readium.r2.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import org.readium.r2.navigator.UserSettings.UserSettings
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Publication


class R2EpubActivity : AppCompatActivity() {

    val TAG = this::class.java.simpleName

    lateinit var publication: Publication
    lateinit var publication_path: String
    lateinit var epub_name: String
    lateinit var server_url: String
    lateinit var resourcePager: R2ViewPager
    lateinit var settingFrameLayout: FrameLayout
    lateinit var userSettings: UserSettings
    lateinit var cssOperator: CSSOperator

    private var resources: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_epub)

        publication_path = intent.getStringExtra("publication_path")
        epub_name = intent.getStringExtra("epub_name")
        publication = intent.getSerializableExtra("publication") as Publication
        server_url = intent.getStringExtra("server_url")

        resourcePager = findViewById(R.id.resourcePager)
        resources = java.util.ArrayList()
        for (spine in publication.spine) {
            val uri = server_url + "/" + epub_name + spine.href
            resources.add(uri)
        }
        val adapter = R2PagerAdapter(supportFragmentManager, resources)
        resourcePager.adapter = adapter

        settingFrameLayout = findViewById(R.id.frameLayout)
        userSettings = UserSettings(getSharedPreferences("org.readium.r2.testapp_preferences", Context.MODE_PRIVATE))
        cssOperator = CSSOperator(userSettings)
        cssOperator.resourcePager = resourcePager

        toggleActionBar()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toc, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {

            R.id.toc -> {
                if (fragmentManager.findFragmentByTag("pref") != null) {
                    settingFrameLayout.visibility = View.GONE
                    fragmentManager.popBackStack()
                }
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication_path", publication_path)
                intent.putExtra("epub_name", epub_name)
                intent.putExtra("publication", publication)
                intent.putExtra("server_url", server_url)
                startActivityForResult(intent, 2)

                return true
            }
            R.id.settings -> {

                if (fragmentManager.findFragmentByTag("pref") != null) {
                    settingFrameLayout.visibility = View.GONE
                    fragmentManager.popBackStack()
                } else {
                    settingFrameLayout.visibility = View.VISIBLE
                    fragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, R2ReaderSettingsFragment(), "pref")
                            .addToBackStack(null)
                            .commit()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val spine_item_index: Int = data.getIntExtra("spine_item_index", 0)
                resourcePager.setCurrentItem(spine_item_index)
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
            resourcePager.setCurrentItem(resourcePager.getCurrentItem() + 1)
        }
    }

    fun previousResource() {
        runOnUiThread {
            resourcePager.setCurrentItem(resourcePager.getCurrentItem() - 1)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (settingFrameLayout.visibility == View.VISIBLE) {
            settingFrameLayout.visibility = View.GONE
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
                if (fragmentManager.findFragmentByTag("pref") != null) {
                    settingFrameLayout.visibility = View.GONE
                    fragmentManager.popBackStack()
                }
            } else {
                resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
    }
}

