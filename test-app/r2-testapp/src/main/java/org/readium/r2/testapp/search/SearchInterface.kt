package org.readium.r2.testapp.search

import android.os.Handler
import org.json.JSONArray
import org.readium.r2.navigator.IR2Activity
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.BuildConfig.DEBUG
import timber.log.Timber


/**
 *
 */
interface SearchInterface {
    fun search(keyword: String, callback: (Pair<Boolean, MutableList<Locator>>) -> Unit)
}

/**
 * This is our custom Search Module, this class uses MarkJS library and implements SearchInterface
 */
class MarkJSSearchEngine(private var listener: IR2Activity) : SearchInterface {


    override fun search(keyword: String, callback: (Pair<Boolean, MutableList<Locator>>) -> Unit) {
        val searchResult = mutableListOf<Locator>()

        for (resourceIndex in 0 until listener.publication.readingOrder.size) {
            val fragment = ((listener.resourcePager?.adapter as R2PagerAdapter).mFragments.get((listener.resourcePager?.adapter as R2PagerAdapter).getItemId(resourceIndex))) as R2EpubPageFragment
            val resource = listener.publication.readingOrder[resourceIndex]
            val resourceHref = resource.href
            val resourceType = resource.type ?: ""
            val resourceTitle = resource.title ?: ""
            Handler().postDelayed({
                fragment.webView?.runJavaScript("markSearch('${keyword}', null, '$resourceHref', '$resourceType', '$resourceTitle')") { result ->
                    if (DEBUG) Timber.tag("SEARCH").d("result $result")

                    if (result != "null") {
                        val locatorsList = mutableListOf<Locator>()
                        val locators = JSONArray(result)
                        if (result.isNotEmpty()) {
                            for (index in 0 until locators.length()) {
                                //Building Locators Objects
                                val locator = locators.getJSONObject(index)
                                val href = locator.getString("href")
                                val type = locator.getString("type")
                                val title = locator.getString("title")
                                val text = Locator.Text.fromJSON(locator.getJSONObject("text"))
                                val location = Locator.Locations.fromJSON(locator.getJSONObject("locations"))
                                val tmpLocator = Locator(href, type, title, location, text)
                                locatorsList.add(tmpLocator)
                            }
                            searchResult.addAll(locatorsList)
                        }
                    }

                    if (DEBUG) Timber.tag("SEARCH").d("resourceIndex $resourceIndex publication.readingOrder.size ${listener.publication.readingOrder.size}")
                    if (resourceIndex == (listener.publication.readingOrder.size - 1)) {
                        callback(Pair(true, searchResult))
                    } else {
                        callback(Pair(false, searchResult))
                    }
                }
            }, 500)

        }

    }

}
