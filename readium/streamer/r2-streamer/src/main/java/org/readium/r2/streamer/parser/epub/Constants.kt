/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

internal object Paths {
    const val LCPL: String = "META-INF/license.lcpl"
    const val CONTAINER = "META-INF/container.xml"
    const val ENCRYPTION = "META-INF/encryption.xml"
    const val KOBO_DISPLAY_OPTIONS = "META-INF/com.kobobooks.display-options.xml"
    const val IBOOKS_DISPLAY_OPTIONS = "META-INF/com.kobobooks.display-options.xml"
}

internal object Namespaces {
    const val OPC = "urn:oasis:names:tc:opendocument:xmlns:container"
    const val ENC = "http://www.w3.org/2001/04/xmlenc#"
    const val SIG = "http://www.w3.org/2000/09/xmldsig#"
    const val COMP = "http://www.idpf.org/2016/encryption#compression"
    const val OPF = "http://www.idpf.org/2007/opf"
    const val DC = "http://purl.org/dc/elements/1.1/"
    const val OPS = "http://www.idpf.org/2007/ops"
    const val XHTML = "http://www.w3.org/1999/xhtml"
    const val SMIL = "http://www.w3.org/ns/SMIL"
    const val NCX = "http://www.daisy.org/z3986/2005/ncx/"
}

internal object Vocabularies {
    const val META = "http://idpf.org/epub/vocab/package/meta/#"
    const val LINK = "http://idpf.org/epub/vocab/package/link/#"
    const val ITEM = "http://idpf.org/epub/vocab/package/item/#"
    const val ITEMREF = "http://idpf.org/epub/vocab/package/itemref/#"

    const val MEDIA = "http://www.idpf.org/epub/vocab/overlays/#"
    const val RENDITION = "http://www.idpf.org/vocab/rendition/#"
    const val TYPE = "http://idpf.org/epub/vocab/structure/#" // this is a guessed value

    const val DCTERMS = "http://purl.org/dc/terms/"
    const val A11Y = "http://www.idpf.org/epub/vocab/package/a11y/#"
    const val MARC = "http://id.loc.gov/vocabulary/"
    const val ONIX = "http://www.editeur.org/ONIX/book/codelists/current.html#"
    const val SCHEMA = "http://schema.org/"
    const val XSD = "http://www.w3.org/2001/XMLSchema#"

    const val MSV = "http://www.idpf.org/epub/vocab/structure/magazine/#"
    const val PRISM = "http://www.prismstandard.org/specifications/3.0/PRISM_CV_Spec_3.0.htm#"
}
