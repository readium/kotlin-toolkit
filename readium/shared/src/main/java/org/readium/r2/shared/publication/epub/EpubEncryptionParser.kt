/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.epub

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.fromEpubHref
import org.readium.r2.shared.util.xml.ElementNode

@InternalReadiumApi
public object EpubEncryptionParser {

    private object Namespaces {
        const val ENC = "http://www.w3.org/2001/04/xmlenc#"
        const val SIG = "http://www.w3.org/2000/09/xmldsig#"
        const val COMP = "http://www.idpf.org/2016/encryption#compression"
    }

    public fun parse(document: ElementNode): Map<Url, Encryption> =
        document.get("EncryptedData", Namespaces.ENC)
            .mapNotNull { parseEncryptedData(it) }
            .toMap()

    private fun parseEncryptedData(node: ElementNode): Pair<Url, Encryption>? {
        val resourceURI = node.getFirst("CipherData", Namespaces.ENC)
            ?.getFirst("CipherReference", Namespaces.ENC)?.getAttr("URI")
            ?.let { Url.fromEpubHref(it) }
            ?: return null
        val retrievalMethod = node.getFirst("KeyInfo", Namespaces.SIG)
            ?.getFirst("RetrievalMethod", Namespaces.SIG)?.getAttr("URI")
        val scheme = if (retrievalMethod == "license.lcpl#/encryption/content_key") {
            ContentProtection.Scheme.Lcp.uri
        } else {
            null
        }
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
        return Pair(resourceURI, enc)
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
