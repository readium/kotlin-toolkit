package org.readium.r2.streamer.Fetcher

import android.util.Log
import com.mcxiaoke.koi.HASH
import com.mcxiaoke.koi.ext.toHexBytes
import org.readium.r2.shared.Publication
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.experimental.xor


class FontDecoder {

    val Adobe = 1024
    val Idpf = 1040

    var decodableAlgorithms = mapOf<String, String>(
        "fontIdpf" to "http://www.idpf.org/2008/embedding",
        "fontAdobe" to "http://ns.adobe.com/pdf/enc#RC")
    var decoders = mapOf<String, Int>(
            "http://www.idpf.org/2008/embedding" to Idpf,
    "http://ns.adobe.com/pdf/enc#RC" to Adobe
    )


    fun decoding(input: InputStream, publication: Publication, path: String) : InputStream {
        val publicationIdentifier = publication.metadata.identifier
        val link = publication.linkWithHref(path) ?: return input
        val encryption = link.properties?.encryption ?: return input
        val algorithm = encryption.algorithm ?: return input
        val type = decoders[link.properties?.encryption?.algorithm] ?: return input
        if (!decodableAlgorithms.values.contains(algorithm)){
            Log.e("Error", "$path is encrypted, but can't handle it")
            return input
        }
        return decodingFont(input, publicationIdentifier, type)
    }

    fun decodingFont(input: InputStream, pubId: String, length: Int) : ByteArrayInputStream{
        val publicationKey: ByteArray
        publicationKey =  when (length){
            Adobe -> getHashKeyAdobe(pubId)
            else -> HASH.sha1(pubId).toHexBytes()
        }
        return ByteArrayInputStream(deobfuscate(input, publicationKey, length))
    }

    fun deobfuscate(input: InputStream, publicationKey: ByteArray, obfuscationLength: Int) : ByteArray {
        val buffer = input.readBytes()
        val count = if (buffer.size > obfuscationLength) obfuscationLength else buffer.size
        for(i in 0..(count - 1))
            buffer[i] = buffer[i].xor(publicationKey[i % publicationKey.size])
        return buffer
    }

    fun getHashKeyAdobe(pubId: String) =
            pubId.replace("urn:uuid:", "")
                    .replace("-", "")
                    .toHexBytes()

}