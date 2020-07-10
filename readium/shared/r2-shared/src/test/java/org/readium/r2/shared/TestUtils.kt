/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.readium.r2.shared.extensions.toList
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import java.io.File
import java.net.URL

/**
 * Asserts that two [JSONObject] are equal.
 */
fun assertJSONEquals(expected: JSONObject, actual: JSONObject) {
    assertEquals(expected.toMap(), actual.toMap())
}

/**
 * Asserts that two [JSONArray] are equal.
 */
fun assertJSONEquals(expected: JSONArray, actual: JSONArray) {
    assertEquals(expected.toList(), actual.toList())
}

class Fixtures(val path: String? = null) {

    fun urlAt(resourcePath: String): URL {
        val path = this.path?.let { "$it/$resourcePath" } ?: resourcePath
        return Fixtures::class.java.getResource(path)!!
    }

    fun pathAt(resourcePath: String): String =
        urlAt(resourcePath).path

    fun fileAt(resourcePath: String): File =
        File(pathAt(resourcePath))

}

internal fun Resource.readBlocking(range: LongRange? = null) = runBlocking { read(range) }

internal fun Fetcher.readBlocking(href: String) = runBlocking { get(Link(href = href)).use { it.readBlocking() } }

internal fun Resource.lengthBlocking() = runBlocking { length() }

internal fun Fetcher.lengthBlocking(href: String) = runBlocking { get(Link(href = href)).use { it.lengthBlocking() } }

internal fun Resource.linkBlocking() = runBlocking { link() }

internal fun Fetcher.linkBlocking(href: String) = runBlocking { get(Link(href = href)).use { it.linkBlocking() } }
