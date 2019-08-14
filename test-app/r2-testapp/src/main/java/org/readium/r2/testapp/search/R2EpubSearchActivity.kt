package org.readium.r2.testapp.search

import android.os.Bundle
import android.util.Base64
import android.view.View
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.COLUMN_COUNT_REF
import org.readium.r2.shared.RenditionLayout
import android.webkit.WebView
import java.io.IOException
import java.io.InputStream
import kotlin.collections.ArrayList







class R2EpubSearchActivity : org.readium.r2.testapp.R2EpubActivity() {


    var currentLocator : SearchLocator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Getting saved locator
        currentLocator = intent.getSerializableExtra("locator") as SearchLocator

        //This will open desired resource from locator's information
        if(currentLocator != null) {
            // Set the progression fetched
            storeProgression(currentLocator?.locations)

            // href is the link to the page in the toc
            var href = currentLocator?.href

            if (href?.indexOf("#") as Int > 0) {
                href = href?.substring(0, href?.indexOf("#") as Int)
            }

            fun setCurrent(resources: ArrayList<*>) {
                for (resource in resources) {
                    if (resource is Pair<*, *>) {
                        resource as Pair<Int, String>
                        if (resource.second.endsWith(href)) {
                            if (resourcePager.currentItem == resource.first) {
                                // reload webview if it has an anchor
                                val currentFragent = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
                                currentLocator?.locations?.fragment?.let {
                                    var anchor = it
                                    if (anchor.startsWith("#")) {
                                    } else {
                                        anchor = "#$anchor"
                                    }
                                    val goto = resource.second +  anchor
                                    currentFragent?.webView?.loadUrl(goto)
                                }?:run {
                                    currentFragent?.webView?.loadUrl(resource.second)
                                }
                            } else {
                                resourcePager.currentItem = resource.first
                            }
                            storeDocumentIndex()
                            break
                        }
                    } else {
                        resource as Triple<Int, String, String>
                        if (resource.second.endsWith(href) || resource.third.endsWith(href)) {
                            resourcePager.currentItem = resource.first
                            storeDocumentIndex()
                            break
                        }
                    }
                }
            }

            resourcePager.adapter = adapter

            if (publication.metadata.rendition.layout == RenditionLayout.Reflowable) {
                setCurrent(resourcesSingle)
            } else {

                when (preferences.getInt(COLUMN_COUNT_REF, 0)) {
                    1 -> {
                        setCurrent(resourcesSingle)
                    }
                    2 -> {
                        setCurrent(resourcesDouble)
                    }
                    else -> {
                        // TODO based on device
                        // TODO decide if 1 page or 2 page
                        setCurrent(resourcesSingle)
                    }
                }
            }

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

    fun injectMarkJS() {
        val currentFragment = (resourcePager.adapter as R2PagerAdapter).getCurrentFragment() as R2EpubPageFragment
        if(currentFragment != null)
        {
            injectScriptFile(currentFragment.webView, "mark.js")
        }
    }

    private fun injectScriptFile(view: WebView, scriptFile: String) {
        val input: InputStream
        try {
            input = assets.open(scriptFile)
            val buffer = ByteArray(input.available())
            input.read(buffer)
            input.close()

            // String-ify the script byte-array using BASE64 encoding !!!
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            view.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()")
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

}
