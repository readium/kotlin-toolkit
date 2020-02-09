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
    const val lcpl: String = "META-INF/license.lcpl"
    const val container = "META-INF/container.xml"
    const val encryption = "META-INF/encryption.xml"
    const val koboDisplayOptions = "META-INF/com.kobobooks.display-options.xml"
    const val ibooksDIsplayOptions = "META-INF/com.kobobooks.display-options.xml"
}

internal object Mimetypes {
    const val Epub = "application/epub+zip"
    const val Oebps = "application/oebps-package+xml"
    const val Ncx = "application/x-dtbncx+xml"
    const val Smil = "application/smil+xml"
}

internal object Namespaces {
    const val Opc = "urn:oasis:names:tc:opendocument:xmlns:container"
    const val Enc = "http://www.w3.org/2001/04/xmlenc#"
    const val Sig = "http://www.w3.org/2000/09/xmldsig#"
    const val Comp = "http://www.idpf.org/2016/encryption#compression"
    const val Opf = "http://www.idpf.org/2007/opf"
    const val Dc = "http://purl.org/dc/elements/1.1/"
    const val Ops = "http://www.idpf.org/2007/ops"
    const val Xhtml = "http://www.w3.org/1999/xhtml"
    const val Smil = "http://www.w3.org/ns/SMIL"
    const val Ncx = "http://www.daisy.org/z3986/2005/ncx/"
}

internal object Vocabularies {
    const val Meta = "http://idpf.org/epub/vocab/package/meta/#"
    const val Link = "http://idpf.org/epub/vocab/package/link/#"
    const val Item = "http://idpf.org/epub/vocab/package/item/#"
    const val Itemref = "http://idpf.org/epub/vocab/package/itemref/#"

    const val Media = "http://www.idpf.org/epub/vocab/overlays/#"
    const val Rendition = "http://www.idpf.org/vocab/rendition/#"
    const val Type = "http://idpf.org/epub/vocab/structure/#" // this is a guessed value

    const val Dcterms = "http://purl.org/dc/terms/"
    const val Ally = "http://www.idpf.org/epub/vocab/package/a11y/#"
    const val Marc = "http://id.loc.gov/vocabulary/"
    const val Onix = "http://www.editeur.org/ONIX/book/codelists/current.html#"
    const val Schema = "http://schema.org/"
    const val Xsd = "http://www.w3.org/2001/XMLSchema#"

    const val Msv = "http://www.idpf.org/epub/vocab/structure/magazine/#"
    const val Prism = "http://www.prismstandard.org/specifications/3.0/PRISM_CV_Spec_3.0.htm#"
}


