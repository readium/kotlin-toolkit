/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.os.AsyncTask
import android.text.Html
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import java.net.URL


class R2SyntheticPageList(private val positionsDB: PositionsDatabase, private val publicationIdentifier: String) : AsyncTask<Triple<String, String, MutableList<String>>, String, MutableList<Position>>() {

    private val syntheticPageList = mutableListOf<Position>()
    private var pageNumber: Long = 0

    override fun doInBackground(vararg p0: Triple<String, String, MutableList<String>>): MutableList<Position> {
        for (uri in p0) {
            for (i in 0 until uri.third.size) {
                createSyntheticPages(uri.first, uri.second, uri.third[i])
            }
        }

        return pageList()
    }

    override fun onPostExecute(result: MutableList<Position>?) {
        super.onPostExecute(result)

        val jsonArrayList = Position.toJSONArray(result!!)
        positionsDB.positions.storeSyntheticPageList(publicationIdentifier, jsonArrayList)
    }

    private fun createSyntheticPages(baseURL: String, epubName: String, resourceHref: String) {
        val resourceURL: URL? = URL(baseURL + epubName + resourceHref)
        val text: String?

        val resourcePageList = mutableListOf<Position>()

        text = resourceURL?.readText()
        var plainTextFromHTML = Html.fromHtml(text).toString().replace("\n".toRegex(), "").trim { it <= ' ' }
        plainTextFromHTML = plainTextFromHTML.substring(plainTextFromHTML.indexOf('}') + 1)


        for (char in 0 until plainTextFromHTML.length-1) {
            if (char%1024 == 0) {
                resourcePageList.add(Position(pageNumber++, resourceHref, char.toDouble() / plainTextFromHTML.length.toDouble()))
            }
        }

        addPages(resourcePageList)
    }

    private fun addPages(resourcePageList: MutableList<Position>) {
        for (i in 0 until resourcePageList.size) {
            syntheticPageList.add(Position(resourcePageList[i].pageNumber, resourcePageList[i].href, resourcePageList[i].progression))
        }
    }

    private fun pageList(): MutableList<Position> {
        return syntheticPageList
    }
}


class Position(var pageNumber: Long? = null, var href: String? = null, var progression: Double? = null) : JSONable {

    companion object {
        fun fromJSON(jsonArray: JSONArray): MutableList<Position> {
            val pageList = mutableListOf<Position>()

            for(i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val position = Position()
                if (json.has("pageNumber")) {
                    position.pageNumber = json.getLong("pageNumber")
                }
                if (json.has("href")) {
                    position.href = json.getString("href")
                }
                if (json.has("progression")) {
                    position.progression = json.getDouble("progression")
                }

                pageList.add(position)
            }

            return pageList
        }

        fun toJSONArray(syntheticPageList : MutableList<Position>) : JSONArray {
            val jsonArray = JSONArray()

            for (page in syntheticPageList) {
                jsonArray.put(page.toJSON())
            }

            return jsonArray
        }
    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("pageNumber", pageNumber)
        json.putOpt("href", href)
        json.putOpt("progression", progression)

        return json
    }

    override fun toString(): String {
        var jsonString = """{"""
        if (pageNumber != null) {
            pageNumber.let { jsonString += """ "pageNumber": "$pageNumber" ,""" }
        }
        if (href != null) {
            href.let { jsonString += """ "href": "$href" ,""" }
        }
        if (progression != null) {
            progression.let { jsonString += """ "progression": "$progression" ,""" }
        }
        jsonString += """}"""
        return jsonString
    }
}