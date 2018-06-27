package org.readium.r2.opds

import java.net.URI

fun getAbsolute(href:String, base:String): String {
    val baseURI = URI.create(base)
    val relative = baseURI.resolve(href)
    return relative.toString()
}
