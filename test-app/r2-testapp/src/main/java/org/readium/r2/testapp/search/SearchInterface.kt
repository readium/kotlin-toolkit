package org.readium.r2.testapp.search

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
    fun search(keyword: String, context: Context) : List<SearchLocator>
}

/**
 * This is our custom Search Module, this class uses MarkJS library and implements SearchInterface
 */
class MyMarkJSSearchInteface(var publication: Publication, var publicationIdentifier: String, var preferences: SharedPreferences, var epubName: String, var bv: BooVariable) : SearchInterface {

    /**
     * This function is used to get search results using JSOUP and MarkJS
     * This function build a temporary WebView and executes JS code on it
     */
    override fun search(keyword: String, context: Context): List<SearchLocator> {
        //Setting up webView in order to execute JS code
        var webView = WebView(context)
        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.javaScriptEnabled = true

        //Loading empty HTML with JS files linked
        var url = "file:///android_asset/index.html"
        webView.loadUrl(url)

        //Setting up variables for iteration
        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()
        var title = publication.metadata.title
        var resourceNumber = 0
        var locatorsList = mutableListOf<SearchLocator>()

        //When webview is loaded -> execute JS to get results
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                //Iterating through resources and executing markJS
                while (resourceNumber < publication.readingOrder.size) {
                    lateinit var resource : org.jsoup.nodes.Document

                    //Creating thread to get resource's HTML content as String using JSOUP
                    val thread = Thread(Runnable {
                        resource = Jsoup.connect("$BASE_URL:$port/$epubName${publication.readingOrder[resourceNumber].href}").get()
                    })

                    //Starting thread
                    thread.start()
                    thread.join()

                    //When thread ends, document should be non null
                    //For each resource, find results using MarkJS
                    resource?.let {
                        var resourceId = publication.readingOrder[resourceNumber].href as String
                        //Element is current HTML resource as String
                        var element = resource.getElementsByClass("body").text()
                        //Log.d("JSOUP",resource.getElementsByTag("body").html().toString())

                        //Removing double and singles quotes from html string
                        element = element.replace("\"", "!§")
                        element = element.replace("\'", "§!")


                        //Executing MarkJS to get results
                        webView.evaluateJavascript("performSearch(\"$keyword\", \"$element\", \"$resourceId\");") { results ->
                            if(results != "null") {
                                Log.d("Enter", results)
                                //Transforming json string
                                var json = results
                                json = json.replace("\\\"", "'")

                                val jsonLocators = JSONArray(json.substring(1, json.length - 1))
                                for (i in 0..(jsonLocators.length() - 1)) {
                                    //Building Locators Objects
                                    val resultObj = jsonLocators.getJSONObject(i)
                                    var href = resultObj.getString("href")
                                    var type = resultObj.getString("type")
                                    var text = LocatorText.fromJSON(resultObj.getJSONObject("text"))
                                    var location = Locations.fromJSON(resultObj.getJSONObject("location"))
                                    var tmpLocator = SearchLocator(href, type, title ,location, text)
                                    locatorsList.add(tmpLocator)
                                    bv.resultsList = locatorsList
                                }
                            }
                        }

                    }
                    resourceNumber++
                }
            }
        }
        return mutableListOf()
    }


    inner class MyJavascriptInterface(internal var context: Context) {

        @android.webkit.JavascriptInterface
        fun getStringFromJS(txtVal: String) {
            Toast.makeText(context, "Value From JS : $txtVal", Toast.LENGTH_LONG).show()
            Log.d("JS DEBUG : ", txtVal)
        }

        @android.webkit.JavascriptInterface
        fun goToNextResource(currentResource: String) {

        }
    }

}
