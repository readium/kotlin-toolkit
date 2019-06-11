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

fun getAbsolute(href: String, base: String): String {
    return try {
        val baseURI = URI.create(base)
        val relative = baseURI.resolve(href)
        relative.toString()
    }catch (e:IllegalArgumentException){
        val hrefUri = Uri.parse(href)
        if (hrefUri.isAbsolute){
            href
        }else{
            base+href
        }
    }
}


internal fun normalize(base: String, in_href: String?) : String {
    if (in_href == null || in_href.isEmpty()) {
        return ""
    }
    val hrefComponents = in_href.split( "/").filter { it.isNotEmpty() }
    val baseComponents = base.split( "/").filter { it.isNotEmpty() }
    baseComponents.dropLast(1)

    val replacementsNumber = hrefComponents.filter { it == ".." }.count()
    var normalizedComponents = hrefComponents.filter { it != ".." }
    for (e in 0 until replacementsNumber) {
        baseComponents.dropLast(1)
    }
    normalizedComponents = baseComponents + normalizedComponents
    var normalizedString = ""
    for (component in normalizedComponents) {
        normalizedString += "/$component"
    }
    return normalizedString
}



