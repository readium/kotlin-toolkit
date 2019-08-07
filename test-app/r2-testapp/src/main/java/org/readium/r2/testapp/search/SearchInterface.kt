package org.readium.r2.testapp.search

import android.content.Context



/**
 *
 */
interface SearchInterface {
    fun search(keyword: String, context: Context) : List<SearchLocator>
}

/**
 * This class uses MarkJS library and implements SearchInterface
 */
class MyMarkJSSearchInteface : SearchInterface {

    /**
     * This function is used to get search results using JSOUP and MarkJS
     */
    override fun search(keyword: String, context: Context): List<SearchLocator> {

        //Iterate through all resources
        //Use JSOUP to get HTML Content as String

        //Use MarkJS's search engine to get results

        //Convert MarkJS's results to SearchLocator

        return listOf()
    }
}


