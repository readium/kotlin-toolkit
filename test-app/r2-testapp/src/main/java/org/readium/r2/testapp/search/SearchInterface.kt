package org.readium.r2.testapp.search

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.jsoup.Jsoup
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.shared.Publication
import org.json.JSONObject
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.readium.r2.shared.Locator


/**
 *
 */
interface SearchInterface {
    fun search(keyword: String, context: Context) : List<SearchLocator>
}

/**
 * This is our custom Search Module, this class uses MarkJS library and implements SearchInterface
 */
class MyMarkJSSearchInteface(var publication: Publication, var publicationIdentifier: String, var preferences: SharedPreferences, var epubName: String) : SearchInterface {

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
        var resourceNumber = 3

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
                        element = element.replace("\"", "\\%")
                        element = element.replace("\'", "\\$")

                        //Executing MarkJS to get results

                        webView.evaluateJavascript("performSearch(\"$keyword\", \"$element\", \"$resourceId\");") { results ->
                            //Log.d("HTML", results)
                            if(results != "[]") {
                                var json = results
                                json = json.replace("\\\"", "'")
                                val jsonLocators = JSONArray(json.substring(1, json.length - 1))
                                for (i in 0..(jsonLocators.length() - 1)) {
                                    val resultObj = jsonLocators.getJSONObject(i)


                                }
                            }

                        }

                    }
                    resourceNumber++
                }
            }
        }
        return listOf()
    }

}




/*
val rhino = org.mozilla.javascript.Context.enter()
rhino.optimizationLevel = -1
val globalScope = rhino.initStandardObjects()
val searchReader = InputStreamReader(context.assets.open("search.js"))
val markJsReader = InputStreamReader(context.assets.open("mark.min.js"))
rhino.evaluateReader(globalScope, searchReader, "search.js", 1, null)
rhino.evaluateReader(globalScope, markJsReader, "mark.min.js", 1, null)

// Add a global variable out that is a JavaScript reflection of the System.out variable:
val wrappedOut = org.mozilla.javascript.Context.javaToJS(System.out, globalScope)
ScriptableObject.putProperty(globalScope, "out", wrappedOut)

// The module esprima is available as a global object due to the same
// scope object passed for evaluation:
val result = rhino.evaluateString(globalScope, "test();", "<mem>", 1, null) as String
Log.d("JSTEST", result)
org.mozilla.javascript.Context.exit()
*/


/*
// Initializing Rhino library to execute JS
        val rhino = org.mozilla.javascript.Context.enter()
        rhino.optimizationLevel = -1
        val globalScope = rhino.initStandardObjects()

        //Adding both JS files to current scope
        val markJsReader = InputStreamReader(context.assets.open("mark.min.js"))
        val searchReader = InputStreamReader(context.assets.open("search.js"))
        rhino.evaluateReader(globalScope, markJsReader, "mark.min.js", 1, null)
        rhino.evaluateReader(globalScope, searchReader, "search.js", 1, null)

        val wrappedOut = org.mozilla.javascript.Context.javaToJS(System.out, globalScope)
        ScriptableObject.putProperty(globalScope, "out", wrappedOut)

        //val result = rhino.evaluateString(globalScope, "test();", "<mem>", 1, null) as String
        //Log.d("JSTEST", result)

        //Initializing variables for iteration
        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()
        var resourceNumber = 0

        //Iterating through resources
        while (resourceNumber < publication.readingOrder.size) {
            lateinit var document : org.jsoup.nodes.Document
            val thread = Thread(Runnable {
                document = Jsoup.connect("$BASE_URL:$port/$epubName${publication.readingOrder[resourceNumber].href}").get()
            })
            thread.start()
            thread.join()
            document?.let {
                //For each resource get results
                var element = document.getElementsByTag("body").html()
                Log.d("JSOUP", element)
                //Replacing single & double quotes
                element = element.replace("\"", "\\%")
                element = element.replace("\'", "\\$")
                var resourceId = publication.readingOrder[resourceNumber].href as String

                //val result = rhino.evaluateString(globalScope, "performMark('$keyword', '$element', '$resourceId')", "<mem>", 1, null) as String

                val result = rhino.evaluateString(globalScope, "test();", "<mem>", 1, null) as String
                Log.d("JSOUP", result)
            }
            resourceNumber++
        }
 */




