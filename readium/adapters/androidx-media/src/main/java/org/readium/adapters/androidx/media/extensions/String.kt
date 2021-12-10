package org.readium.adapters.androidx.media.extensions

/**
 * Splits a [String] in two components, at the given delimiter.
 */
internal fun String.splitAt(delimiter: String): Pair<String, String?> {
    val components = split(delimiter, limit = 2)
    return Pair(components[0], components.getOrNull(1))
}
