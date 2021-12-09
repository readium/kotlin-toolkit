/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.util.Href

internal object EncryptionParser {
    fun parse(document: ElementNode): Map<String, Encryption> =
        document.get("EncryptedData", Namespaces.ENC)
            .mapNotNull { parseEncryptedData(it) }
            .toMap()

    private fun parseEncryptedData(node: ElementNode): Pair<String, Encryption>? {
        val resourceURI = node.getFirst("CipherData", Namespaces.ENC)
            ?.getFirst("CipherReference", Namespaces.ENC)?.getAttr("URI")
            ?: return null
        val retrievalMethod = node.getFirst("KeyInfo", Namespaces.SIG)
            ?.getFirst("RetrievalMethod", Namespaces.SIG)?.getAttr("URI")
        val scheme = if (retrievalMethod == "license.lcpl#/encryption/content_key")
            DRM.Scheme.lcp.rawValue else null
        val algorithm = node.getFirst("EncryptionMethod", Namespaces.ENC)
            ?.getAttr("Algorithm")
            ?: return null
        val compression = node.getFirst("EncryptionProperties", Namespaces.ENC)
            ?.let { parseEncryptionProperties(it) }
        val originalLength = compression?.first
        val compressionMethod = compression?.second
        val enc = Encryption(
            scheme = scheme,
            /* profile = drm?.license?.encryptionProfile,
            FIXME: This has probably never worked. Profile needs to be filled somewhere, though. */
            algorithm = algorithm,
            compression = compressionMethod,
            originalLength = originalLength
        )
        return Pair(Href(resourceURI).string, enc)
    }

    private fun parseEncryptionProperties(encryptionProperties: ElementNode): Pair<Long, String>? {
        for (encryptionProperty in encryptionProperties.get("EncryptionProperty", Namespaces.ENC)) {
            val compressionElement = encryptionProperty.getFirst("Compression", Namespaces.COMP)
            if (compressionElement != null) {
                parseCompressionElement(compressionElement)?.let { return it }
            }
        }
        return null
    }

    private fun parseCompressionElement(compressionElement: ElementNode): Pair<Long, String>? {
        val originalLength = compressionElement.getAttr("OriginalLength")?.toLongOrNull()
            ?: return null
        val method = compressionElement.getAttr("Method")
            ?: return null
        val compression = if (method == "8") "deflate" else "none"
        return Pair(originalLength, compression)
    }
}
