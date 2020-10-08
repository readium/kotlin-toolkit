/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.cbz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.*
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.publication.*
import kotlin.coroutines.CoroutineContext

open class R2CbzActivity : AppCompatActivity(), CoroutineScope, IR2Activity, VisualNavigator, ImageNavigatorFragment.Listener {

    private val navigatorFragment: ImageNavigatorFragment
        get() = supportFragmentManager.findFragmentById(R.id.image_navigator) as ImageNavigatorFragment


    protected var navigatorDelegate: NavigatorDelegate? = null

    protected val positions: List<Locator> get() = navigatorFragment.positions
    val currentPagerPosition: Int get() = navigatorFragment.currentPagerPosition

    override val currentLocator: LiveData<Locator?>
        get() = navigatorFragment.currentLocator

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        return navigatorFragment.go(locator, animated, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        return navigatorFragment.go(link, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        return navigatorFragment.goForward(animated, completion)
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        return navigatorFragment.goBackward(animated, completion)
    }

    override val readingProgression: ReadingProgression
        get() = navigatorFragment.readingProgression


    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override lateinit var preferences: SharedPreferences
    override lateinit var resourcePager: R2ViewPager
    override lateinit var publicationPath: String
    override lateinit var publication: Publication
    override lateinit var publicationIdentifier: String
    override lateinit var publicationFileName: String

    override var bookId: Long = -1

    var resources: List<String> = emptyList()
    lateinit var adapter: R2PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        publicationPath = intent.getStringExtra("publicationPath") ?: throw Exception("publicationPath required")
        publicationFileName = intent.getStringExtra("publicationFileName") ?: throw Exception("publicationFileName required")
        publication = intent.getPublication(this)

        publicationIdentifier = publication.metadata.identifier!!
        title = publication.metadata.title

        val initialLocator = intent.getParcelableExtra("locator") as? Locator

        supportFragmentManager.fragmentFactory = ImageNavigatorFragment.Factory(publication, initialLocator = initialLocator, listener = this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_r2_image)

        resourcePager = navigatorFragment.resourcePager

        navigatorFragment.currentLocator.observe(this, Observer { locator ->
            locator ?: return@Observer
            @Suppress("DEPRECATION")
            navigatorDelegate?.locationDidChange(this, locator)
        })
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    override fun nextResource(v: View?) {
        navigatorFragment.nextResource(v)
    }

    override fun previousResource(v: View?) {
        navigatorFragment.previousResource(v)
    }

    override fun toggleActionBar() {
        if (allowToggleActionBar) {
            launch {
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

    override fun toggleActionBar(v: View?) {
        toggleActionBar()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {

                val locator = data.getParcelableExtra("locator") as? Locator

                locator?.let {
                    fun setCurrent(resources: List<String>) {
                        for (index in 0 until resources.count()) {
                            val resource = resources[index]
                            if (resource.endsWith(locator.href)) {
                                resourcePager.currentItem = index
                                break
                            }
                        }
                    }

                    go(locator)

                    setCurrent(resources)
                }

                if (supportActionBar!!.isShowing && allowToggleActionBar) {
                    resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            or View.SYSTEM_UI_FLAG_IMMERSIVE)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
