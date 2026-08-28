@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.extensions

import com.mcxiaoke.koi.HASH
import com.mcxiaoke.koi.ext.toHexBytes
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull

/**
 * Computes the SHA-1 hash of a string.
 */
internal fun String.sha1(): String =
    HASH.sha1(this)

/**
 * Converts an hexadecimal string (e.g. 8ad5078e) to a byte array.
 */
internal fun String.toHexByteArray(): ByteArray? {
    // Only even-length strings can be converted to an Hex byte array, otherwise it crashes
    // with StringIndexOutOfBoundsException.
    if (isEmpty() || !hasEvenLength() || !isHexadecimal()) {
        return null
    }
    return tryOrNull { toHexBytes() }
}

private fun String.hasEvenLength(): Boolean =
    length % 2 == 0

private fun String.isHexadecimal(): Boolean =
    all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
