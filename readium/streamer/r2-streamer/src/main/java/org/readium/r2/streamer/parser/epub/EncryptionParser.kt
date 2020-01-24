/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.Encryption
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.streamer.parser.normalize

object EncryptionParser {
    fun parse(document: ElementNode, drm: DRM?) : Map<String, Encryption> =
        document.get("EncryptedData", Namespaces.Enc)
                .mapNotNull{ parseEncryptedData(it, drm) }
                .associate{ it }

    private fun parseEncryptedData(node: ElementNode, drm: DRM?) : Pair<String, Encryption>? {
        val resourceURI = node.getFirst("CipherData", Namespaces.Enc)
                ?.getFirst("CipherReference", Namespaces.Enc)?.getAttr("URI") ?: return null
        val keyInfoUri = node.getFirst("KeyInfo", Namespaces.Sig)
                ?.getFirst("RetrievalMethod", Namespaces.Sig)
                ?.getAttr("URI")
        val scheme = if (keyInfoUri == "license.lcpl#/encryption/content_key" && drm?.brand == DRM.Brand.lcp)
            DRM.Scheme.lcp else null
        val algorithm = node.getFirst("EncryptionMethod", Namespaces.Enc)
                ?.getAttr("Algorithm")
        val compression = node.getFirst("EncryptionProperties", Namespaces.Enc)?.let { parseEncryptionProperties(it) }
        val originalLength = compression?.first
        val compressionMethod = compression?.second
        val enc = Encryption().apply {
            this.scheme = scheme
            this.algorithm = algorithm
            this.compression = compressionMethod
            this.originalLength = originalLength
        }
        return Pair(resourceURI, enc)
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

    fun add(encryption: Encryption, publication: Publication, encryptedDataElement: ElementNode) {
        var resourceURI = encryptedDataElement.getFirst("CipherData", Namespaces.Enc)
                ?.getFirst("CipherReference", Namespaces.Enc)?.getAttr("URI")
                ?: return
        resourceURI = normalize("/", resourceURI)
        val link = publication.linkWithHref(resourceURI) ?: return
        link.properties.encryption = encryption
    }

}