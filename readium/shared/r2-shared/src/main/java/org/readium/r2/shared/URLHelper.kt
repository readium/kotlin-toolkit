/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.shared

import java.net.URI

fun getAbsolute(href:String, base:String): String {
    val baseURI = URI.create(base)
    val relative = baseURI.resolve(href)
    return relative.toString()
}
