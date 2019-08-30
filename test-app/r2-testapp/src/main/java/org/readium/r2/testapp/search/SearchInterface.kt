package org.readium.r2.testapp.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.jsoup.Jsoup
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.shared.Locations
import org.readium.r2.shared.LocatorText
import org.readium.r2.shared.Publication


/**
 *
 */
interface SearchInterface {
    fun search(keyword: String, context: Context, callback: (Pair<Boolean, MutableList<SearchLocator>>) -> Unit)
}

interface R2Search {
    fun injectMarkJS()
    fun injectScriptFile(view: WebView, scriptFile: String)
}

/**
 * This is our custom Search Module, this class uses MarkJS library and implements SearchInterface
 */
class MarkJSSearchInterface(var publication: Publication, private var publicationIdentifier: String, private var preferences: SharedPreferences, var epubName: String) : SearchInterface {

    /**
     * This function is used to get search results using JSOUP and MarkJS
     * This function build a temporary WebView and executes JS code on it
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun search(keyword: String, context: Context, callback: (Pair<Boolean, MutableList<SearchLocator>>) -> Unit) {
        //Setting up webView in order to execute JS code
        val webView = WebView(context)
        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.javaScriptEnabled = true

        //Loading empty HTML with JS files linked
        val url = "file:///android_asset/Search/index.html"
        webView.loadUrl(url)

        //Setting up variables for iteration
        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString())?.toInt()
        var resourceNumber = 0
        val locatorsList = mutableListOf<SearchLocator>()

        //When webview is loaded -> execute JS to get results
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                //Iterating through resources and executing markJS
                while (resourceNumber < publication.readingOrder.size) {
                    lateinit var resource: org.jsoup.nodes.Document

                    //Creating thread to get resource's HTML content as String using JSOUP
                    val thread = Thread(Runnable {
                        resource = Jsoup.connect("$BASE_URL:$port/$epubName${publication.readingOrder[resourceNumber].href}").get()
                    })

                    //Starting thread
                    thread.start()
                    thread.join()

                    //When thread ends, document should be non null
                    //For each resource, find results using MarkJS
                    resource.let {
                        val resource1 = publication.readingOrder[resourceNumber]
                        val resourceHref = resource1.href ?: ""
                        val resourceType = resource1.typeLink ?: ""
                        val resourceTitle = resource1.title ?: ""

                        //Element is current HTML resource as String
                        var element = resource.getElementsByClass("body").text()

                        //Removing double and singles quotes from html string
                        element = element.replace("\"", "!§")
                        element = element.replace("\'", "§!")


                        //Executing MarkJS to get results
                        webView.evaluateJavascript("markSearch(\"$keyword\", \"$element\", \"$resourceHref\", \"$resourceType\", \"$resourceTitle\")") { results ->
                            val locators = JSONArray(results)
                            if (results.isNotEmpty()) {
                                for (i in 0 until locators.length()) {
                                    //Building Locators Objects
                                    val locator = locators.getJSONObject(i)
                                    val href = locator.getString("href")
                                    val type = locator.getString("type")
                                    val title = locator.getString("title")
                                    val text = LocatorText.fromJSON(locator.getJSONObject("text"))
                                    val location = Locations.fromJSON(locator.getJSONObject("locations"))
                                    val tmpLocator = SearchLocator(href, type, title, location, text)
                                    locatorsList.add(tmpLocator)
                                }
                                callback(Pair(false, locatorsList))
                            }
                        }

                    }
                    resourceNumber++
                }
                if (resourceNumber == publication.readingOrder.size) {
                    callback(Pair(true, locatorsList))
                }
            }
        }

    }

}
