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
import org.readium.r2.streamer.parser.normalize
import org.readium.r2.shared.parser.xml.ElementNode

internal object EncryptionParser {
    fun parse(document: ElementNode, drm: DRM?) : Map<String, Encryption> =
        document.get("EncryptedData", Namespaces.Enc)
                .mapNotNull{ parseEncryptedData(it, drm) }
                .associate{ it }

    private fun parseEncryptedData(node: ElementNode, drm: DRM?) : Pair<String, Encryption>? {
        val resourceURI = node.getFirst("CipherData", Namespaces.Enc)
                ?.getFirst("CipherReference", Namespaces.Enc)?.getAttr("URI") ?: return null
        val retrievalMethod = node.getFirst("KeyInfo", Namespaces.Sig)
                ?.getFirst("RetrievalMethod", Namespaces.Sig)?.getAttr("URI")
        val scheme = if (retrievalMethod == "license.lcpl#/encryption/content_key" && drm?.brand == DRM.Brand.lcp)
            DRM.Scheme.lcp.rawValue else null
        val algorithm = node.getFirst("EncryptionMethod", Namespaces.Enc)
                ?.getAttr("Algorithm") ?: return null
        val compression = node.getFirst("EncryptionProperties", Namespaces.Enc)?.let { parseEncryptionProperties(it) }
        val originalLength = compression?.first
        val compressionMethod = compression?.second
        val enc = Encryption(
                scheme = scheme,
                profile = drm?.license?.encryptionProfile,
                algorithm = algorithm,
                compression = compressionMethod,
                originalLength = originalLength
        )
        return Pair(normalize("/", resourceURI), enc)
    }

    private fun parseEncryptionProperties(encryptionProperties: ElementNode) : Pair<Int, String>? {
        for (encryptionProperty in encryptionProperties.get("EncryptionProperty", Namespaces.Enc)) {
            val compressionElement = encryptionProperty.getFirst("Compression", Namespaces.Comp)
            if (compressionElement != null) {
                return parseCompressionElement(compressionElement) ?: continue
            }
        }
        return null
    }

    private fun parseCompressionElement(compressionElement: ElementNode) : Pair<Int, String>? {
        val originalLength = compressionElement.getAttr("OriginalLength")?.toInt() ?: return null
        val method = compressionElement.getAttr("Method") ?: return null
        val compression = if (method == "8") "deflate" else "none"
        return Pair(originalLength, compression)
    }
}