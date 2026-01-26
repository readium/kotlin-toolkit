/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class, ExperimentalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationAudioRef
import org.readium.r2.shared.guided.GuidedNavigationDocument
import org.readium.r2.shared.guided.GuidedNavigationObject
import org.readium.r2.shared.guided.GuidedNavigationRole
import org.readium.r2.shared.guided.GuidedNavigationTextRef
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.MediaOverlaysService
import org.readium.r2.shared.publication.services.GuidedNavigationIterator
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.decodeXml
import org.readium.r2.shared.util.data.readDecodeOrElse
import org.readium.r2.shared.util.fromEpubHref
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.xml.ElementNode

/**
 * A GuidedNavigationService producing guided navigation documents from media overlays SMIL files.
 */
@ExperimentalReadiumApi
public class SmilBasedMediaOverlaysService(
    private val smilFiles: List<Url>,
    private val container: Container<Resource>,
) : MediaOverlaysService {

    override fun iterator(): GuidedNavigationIterator {
        return MediaOverlaysIterator(smilFiles, container)
    }

    public companion object {

        public fun createFactory(
            smilFiles: List<Url>,
        ): (
            Publication.Service.Context,
        ) -> SmilBasedMediaOverlaysService =
            { context ->
                SmilBasedMediaOverlaysService(
                    smilFiles = smilFiles,
                    container = context.container
                )
            }
    }
}

private class MediaOverlaysIterator(
    private val smils: List<Url>,
    private val container: Container<Resource>,
) : GuidedNavigationIterator {

    private var lastRead: Int = -1
    private var nextDoc: Try<GuidedNavigationDocument, ReadError>? = null

    override suspend fun hasNext(): Boolean {
        if (lastRead >= smils.size - 1) {
            return false
        }

        lastRead++
        nextDoc = parse(smils[lastRead])
        return true
    }

    private suspend fun parse(href: Url): Try<GuidedNavigationDocument, ReadError> {
        val resource = container[href]
            ?: throw IllegalStateException("Cannot find resource in the container: $href")

        val xmlDoc = resource.use { res ->
            res.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(it) }
            )
        }

        return SmilParser.parse(xmlDoc, href)
            ?.let { Try.success(it) }
            ?: Try.failure(ReadError.Decoding("Cannot parse SMIL file $href"))
    }

    override suspend fun next(): Try<GuidedNavigationDocument, ReadError> {
        return nextDoc ?: throw IllegalStateException("next was called before hasNext.")
    }
}

internal object SmilParser {

    fun parse(document: ElementNode, fileHref: Url): GuidedNavigationDocument? {
        val docPrefixes = document.getAttrNs("prefix", Namespaces.OPS)
            ?.let { parsePrefixes(it) }.orEmpty()
        val prefixMap = CONTENT_RESERVED_PREFIXES + docPrefixes // prefix element overrides reserved prefixes
        val body = document.getFirst("body", Namespaces.SMIL) ?: return null
        return parseSeq(body, fileHref, prefixMap)
            ?.let { GuidedNavigationDocument(links = emptyList(), guided = it.children) }
    }

    private fun parseSeq(
        node: ElementNode,
        filePath: Url,
        prefixMap: Map<String, String>,
    ): GuidedNavigationObject? {
        val roles = parseRoles(node, prefixMap)
        val children: MutableList<GuidedNavigationObject> = mutableListOf()
        for (child in node.getAll()) {
            if (child.name == "par" && child.namespace == Namespaces.SMIL) {
                parsePar(child, filePath, prefixMap)?.let { children.add(it) }
            } else if (child.name == "seq" && child.namespace == Namespaces.SMIL) {
                parseSeq(child, filePath, prefixMap)?.let { children.add(it) }
            }
        }

        return GuidedNavigationObject(children = children, roles = roles, refs = emptySet(), text = null)
    }

    private fun parsePar(
        node: ElementNode,
        filePath: Url,
        prefixMap: Map<String, String>,
    ): GuidedNavigationObject? {
        val roles = parseRoles(node, prefixMap)
        val text = node.getFirst("text", Namespaces.SMIL)
            ?.getAttr("src")
            ?.let { Url.fromEpubHref(it) }
            ?: return null
        val audio = node.getFirst("audio", Namespaces.SMIL)
            ?.let { audioNode ->
                val src = audioNode.getAttr("src")
                    ?.let { Url.fromEpubHref(it) }
                    ?.toString()
                    ?: return null
                val begin = audioNode.getAttr("clipBegin")
                    ?.let { ClockValueParser.parse(it) }
                    ?: ""
                val end = audioNode.getAttr("clipEnd")
                    ?.let { ClockValueParser.parse(it) }
                    ?: ""
                Url("$src#t=$begin,$end")
            }

        val refs = setOfNotNull(
            GuidedNavigationTextRef(filePath.resolve(text)),
            audio?.let { GuidedNavigationAudioRef(filePath.resolve(it)) }
        )

        return GuidedNavigationObject(
            children = emptyList(),
            text = null,
            refs = refs,
            roles = roles
        )
    }

    private fun parseRoles(
        node: ElementNode,
        prefixMap: Map<String, String>,
    ): Set<GuidedNavigationRole> {
        val typeAttr = node.getAttrNs("type", Namespaces.OPS) ?: ""
        val candidates = if (typeAttr.isNotEmpty()) {
            parseProperties(typeAttr).map {
                resolveProperty(
                    it,
                    prefixMap,
                    DEFAULT_VOCAB.TYPE
                )
            }.toSet()
        } else {
            emptySet()
        }

        return candidates.mapNotNull {
            when (it.removePrefix(Vocabularies.TYPE)) {
                "aside" -> GuidedNavigationRole.ASIDE
                "table-cell" -> GuidedNavigationRole.CELL
                "glossdef" -> GuidedNavigationRole.DEFINITION
                "figure" -> GuidedNavigationRole.FIGURE
                "list" -> GuidedNavigationRole.LIST
                "list-item" -> GuidedNavigationRole.LIST_ITEM
                "table-row" -> GuidedNavigationRole.ROW
                "table" -> GuidedNavigationRole.TABLE
                "glossterm" -> GuidedNavigationRole.TERM

                "abstract" -> GuidedNavigationRole.ABSTRACT
                "acknowledgments" -> GuidedNavigationRole.ACKNOWLEDGMENTS
                "afterword" -> GuidedNavigationRole.AFTERWORD
                "appendix" -> GuidedNavigationRole.APPENDIX
                "backlink" -> GuidedNavigationRole.BACKLINK
                "bibliography" -> GuidedNavigationRole.BIBLIOGRAPHY
                "biblioref" -> GuidedNavigationRole.BIBLIOREF
                "chapter" -> GuidedNavigationRole.CHAPTER
                "colophon" -> GuidedNavigationRole.COLOPHON
                "conclusion" -> GuidedNavigationRole.CONCLUSION
                "cover" -> GuidedNavigationRole.COVER
                "credit" -> GuidedNavigationRole.CREDIT
                "credits" -> GuidedNavigationRole.CREDITS
                "dedication" -> GuidedNavigationRole.DEDICATION
                "endnotes" -> GuidedNavigationRole.ENDNOTES
                "epigraph" -> GuidedNavigationRole.EPIGRAPH
                "epilogue" -> GuidedNavigationRole.EPILOGUE
                "errata" -> GuidedNavigationRole.ERRATA
                "example" -> GuidedNavigationRole.EXAMPLE
                "footnote" -> GuidedNavigationRole.FOOTNOTE
                "glossary" -> GuidedNavigationRole.GLOSSARY
                "glossref" -> GuidedNavigationRole.GLOSSREF
                "index" -> GuidedNavigationRole.INDEX
                "introduction" -> GuidedNavigationRole.INTRODUCTION
                "noteref" -> GuidedNavigationRole.NOTEREF
                "notice" -> GuidedNavigationRole.NOTICE
                "pagebreak" -> GuidedNavigationRole.PAGEBREAK
                "page-list" -> GuidedNavigationRole.PAGELIST
                "part" -> GuidedNavigationRole.PART
                "preface" -> GuidedNavigationRole.PREFACE
                "prologue" -> GuidedNavigationRole.PROLOGUE
                "pullquote" -> GuidedNavigationRole.PULLQUOTE
                "qna" -> GuidedNavigationRole.QNA
                "subtitle" -> GuidedNavigationRole.SUBTITLE
                "tip" -> GuidedNavigationRole.TIP
                "toc" -> GuidedNavigationRole.TOC

                "landmarks" -> GuidedNavigationRole.LANDMARKS
                "loa" -> GuidedNavigationRole.LOA
                "loi" -> GuidedNavigationRole.LOI
                "lot" -> GuidedNavigationRole.LOT
                "lov" -> GuidedNavigationRole.LOV
                else -> null
            }
        }.toSet()
    }
}
