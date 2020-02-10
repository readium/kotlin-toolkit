/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import android.net.Uri
import java.net.URI
import java.net.URLDecoder

fun getAbsolute(href: String, base: String): String {
    return try {
        val baseURI = URI.create(base)
        val relative = baseURI.resolve(href)
        relative.toString()
    } catch (e:IllegalArgumentException){
        val hrefUri = Uri.parse(href)
        if (hrefUri.isAbsolute){
            href
        }else{
            base+href
        }
    }
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



