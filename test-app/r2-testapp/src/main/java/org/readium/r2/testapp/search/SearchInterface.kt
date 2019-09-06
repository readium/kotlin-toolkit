package org.readium.r2.testapp.search

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import org.json.JSONArray
import org.readium.r2.navigator.R2ActivityListener
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Locations
import org.readium.r2.shared.LocatorText
import org.readium.r2.shared.Publication
import timber.log.Timber


/**
 *
 */
interface SearchInterface {
    fun search(keyword: String, callback: (Pair<Boolean, MutableList<SearchLocator>>) -> Unit)
}

/**
 * This is our custom Search Module, this class uses MarkJS library and implements SearchInterface
 */
class MarkJSSearchInterface(val context: Context, override var resourcePager: R2ViewPager, override var publication: Publication, override var publicationIdentifier: String, override var preferences: SharedPreferences) : SearchInterface, R2ActivityListener {

    override fun search(keyword: String, callback: (Pair<Boolean, MutableList<SearchLocator>>) -> Unit) {
        val searchResult = mutableListOf<SearchLocator>()

        for (resourceIndex in 0 until publication.readingOrder.size) {
            val fragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourceIndex))) as R2EpubPageFragment
            val resource = publication.readingOrder[resourceIndex]
            val resourceHref = resource.href ?: ""
            val resourceType = resource.typeLink ?: ""
            val resourceTitle = resource.title ?: ""
            Handler().postDelayed({
                fragment.webView.runJavaScript("markSearch('${keyword}', null, '$resourceHref', '$resourceType', '$resourceTitle')") { result ->
                    Timber.tag("SEARCH").d("result $result")

                    if (result != "null") {
                        val locatorsList = mutableListOf<SearchLocator>()
                        val locators = JSONArray(result)
                        if (result.isNotEmpty()) {
                            for (index in 0 until locators.length()) {
                                //Building Locators Objects
                                val locator = locators.getJSONObject(index)
                                val href = locator.getString("href")
                                val type = locator.getString("type")
                                val title = locator.getString("title")
                                val text = LocatorText.fromJSON(locator.getJSONObject("text"))
                                val location = Locations.fromJSON(locator.getJSONObject("locations"))
                                val tmpLocator = SearchLocator(href, type, title, location, text)
                                locatorsList.add(tmpLocator)
                            }
                            searchResult.addAll(locatorsList)
                        }
                    }

                    Timber.tag("SEARCH").d("resourceIndex $resourceIndex publication.readingOrder.size ${publication.readingOrder.size}")
                    if (resourceIndex == (publication.readingOrder.size - 1)) {
                        callback(Pair(true, searchResult))
                    } else {
                        callback(Pair(false, searchResult))
                    }
                }
            }, 500)

        }

    }

}
