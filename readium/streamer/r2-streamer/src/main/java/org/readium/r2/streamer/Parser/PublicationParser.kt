package org.readium.r2.streamer.Parser

import org.readium.r2.shared.Publication
import org.readium.r2.streamer.Containers.Container

data class PubBox(val publication: Publication, val container: Container)

interface PublicationParser {

    fun parse(fileAtPath: String) : PubBox?

}

fun normalize(base: String, href: String?) : String {
    if (href == null || href.isEmpty())
        return ""
    val hrefComponents = href.split('/').filter({ !it.isEmpty() })
    var baseComponents = base.split('/').filter({ !it.isEmpty() })

    // Remove the /folder/folder/"PATH.extension" part to keep only the path.
    baseComponents = baseComponents.dropLast(1)
    // Find the number of ".." in the path to replace them.
    val replacementsNumber = hrefComponents.filter ({ it == ".." }).count()
    // Get the valid part of href, reversed for next operation.
    var normalizedComponents = hrefComponents.filter({ it != ".." })
    // Add the part from base to replace the "..".
    for (i in 0 until  replacementsNumber) {
        baseComponents = baseComponents.dropLast(1)
    }
    normalizedComponents = baseComponents + normalizedComponents
    // Recreate a string.
    var normalizedString = ""
    for (component in normalizedComponents) {
        normalizedString += "/$component"
    }
    return normalizedString
}
