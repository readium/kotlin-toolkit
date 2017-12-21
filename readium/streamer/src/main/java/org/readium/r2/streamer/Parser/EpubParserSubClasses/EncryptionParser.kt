package org.readium.r2.streamer.Parser.EpubParserSubClasses

import org.readium.r2.shared.Encryption
import org.readium.r2.shared.Properties
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.Parser.normalize
import org.readium.r2.streamer.XmlParser.Node

class EncryptionParser{

    fun parseEncryptionProperties(encryptedDataElement: Node, encryption: Encryption) {
        val encryptionProperties = encryptedDataElement.getFirst("EncryptionProperties")?.get("EncryptionProperty") ?: return
        for (encryptionProperty in encryptionProperties){
            parseCompressionElement(encryptionProperty, encryption)
        }
    }

    fun parseCompressionElement(encryptionProperty: Node, encryption: Encryption){
        val compressionElement = encryptionProperty.getFirst("Compression") ?: return
        val originalLength = compressionElement.properties["OriginalLength"]
        encryption.originalLength = originalLength?.toInt()
        val method = compressionElement.properties["Method"] ?: return
        encryption.compression = if (method == "8") "deflate" else "none"
    }

    fun add(encryption: Encryption, publication: Publication, encryptedDataElement: Node){
        var resourceURI = encryptedDataElement.getFirst("CipherData")?.getFirst("CipherReference")?.
                let{it.properties["URI"]} ?: return
        resourceURI = normalize("/", resourceURI)
        val link = publication.linkWithHref(resourceURI) ?: return
        if (link.properties == null)
            link.properties = Properties()
        link.properties!!.encryption = encryption
    }

}