/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser

import android.net.Uri
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.container.Container
import java.io.File
import java.net.URI
import java.net.URLDecoder

data class PubBox(var publication: Publication, var container: Container)

interface PublicationParser {

    fun parse(fileAtPath: String, title: String = File(fileAtPath).name): PubBox?

}

fun normalize(base: String, href: String?): String {
    val resolved = if (href.isNullOrEmpty()) ""
    else try { // href is returned by resolve if it is absolute
        val absoluteUri = URI.create(base).resolve(href)
        val absoluteString = absoluteUri.toString() // this is a percent-decoded
        val addSlash = absoluteUri.scheme == null && !absoluteString.startsWith("/")
        (if (addSlash) "/" else "") + absoluteString
    } catch (e: IllegalArgumentException){ // one of the URIs is ill-formed
        val hrefUri = Uri.parse(href) // Android Uri is more forgiving
        // Let's try to return something
        if (hrefUri.isAbsolute) {
            href
        } else if (base.startsWith("/")) {
           base + href
        } else
            "/" + base + href
    }
    return URLDecoder.decode(resolved, "UTF-8")
}
