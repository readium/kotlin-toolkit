package org.readium.r2.streamer.Parser.EpubParserSubClasses

import org.readium.r2.shared.*
import org.readium.r2.streamer.XmlParser.Node

const val noTitleError = "Error : Publication has no title"

class MetadataParser {

    fun parseRenditionProperties(metadataElement: Node, metadata: Metadata){
        val metas = metadataElement.get("meta")!!
        if (metas.isEmpty()){
            metadata.rendition.layout = RenditionLayout.reflowable
            return
        }
        try {
            metas.first { it.properties["property"] == "rendition:layout" }.text?.let {
                metadata.rendition.layout = RenditionLayout.valueOf(it)
            }
        } catch(e: Exception) {
            metadata.rendition.layout = RenditionLayout.reflowable
        }
        try {
            metas.first { it.properties["property"] == "rendition:flow" }.text?.let {
                metadata.rendition.flow = RenditionFlow.valueOf(it)
            }
        } catch(e: Exception) {
            metadata.rendition.layout = RenditionLayout.reflowable
        }
        try {
            metas.first { it.properties["property"] == "rendition:orientation" }.text?.let {
                metadata.rendition.orientation = RenditionOrientation.valueOf(it)
            }
        } catch(e: Exception) {
            metadata.rendition.layout = RenditionLayout.reflowable
        }
        try {
            metas.first { it.properties["property"] == "rendition:spread" }.text?.let {
                metadata.rendition.spread = RenditionSpread.valueOf(it)
            }
        } catch(e: Exception) {
            metadata.rendition.layout = RenditionLayout.reflowable
        }
        try {
            metas.first { it.properties["property"] == "rendition:viewport" }.text?.let {
                metadata.rendition.viewport= it
            }
        } catch(e: Exception) {
            metadata.rendition.layout = RenditionLayout.reflowable
        }
    }

    // Parse and return the main title informations of the publication the from
    // the OPF XML document `<metadata>` element.
    // In the simplest cases it just return the value of the <dc:title> XML
    // element, but sometimes there are alternative titles (titles in others
    // languages).
    // See `MultilangString` for complementary informations.
    //
    // - Parameter metadata: The `<metadata>` element.
    // - Returns: The content of the `<dc:title>` element, `nil` if the element
    //            wasn't found.
    fun mainTitle(metadata: Node): MultilangString? {
        val titles = metadata.children.filter { node -> node.name == "dc:title" }
        if (titles.isEmpty())
            throw Exception(noTitleError)
        val multilangTitle = MultilangString()

        multilangTitle.singleString = try {
            metadata.get("dc:title")?.first()?.text ?: throw Exception("No title")
        } catch (e: Exception) {
            throw Exception(noTitleError)
        }
        val mainTitle = getMainTitleElement(titles, metadata) ?: return multilangTitle
        multilangTitle.multiString = multiString(mainTitle, metadata)
        return multilangTitle
    }

    // Parse and return the Epub unique identifier.
    //
    // - Parameters:
    //   - metadata: The metadata XML element.
    //   - Attributes: The XML document attributes.
    // - Returns: The content of the `<dc:identifier>` element, `nil` if the
    //            element wasn't found.
    fun uniqueIdentifier(metadata: Node, documentProperties: Map<String, String>): String? {
        val identifiers = metadata.get("dc:identifier") ?: throw Exception("No identifier")
        if (identifiers.isEmpty())
            return null
        val uniqueId = documentProperties["unique-identifier"]
        if (identifiers.size > 1 && uniqueId != null) {
            val uniqueIdentifiers = identifiers.filter { it.properties["id"] == uniqueId }
            if (!uniqueIdentifiers.isEmpty())
                return uniqueIdentifiers.firstOrNull()?.text ?: throw Exception("No identifier")
        }
        return identifiers[0].text
    }

    fun modifiedDate(metadataElement: Node) = metadataElement.get("meta")!!.firstOrNull {
                it.properties["property"] == "dcterms:modified"
            }?.text

    fun subject(metadataElement: Node) : Subject? {
        val subjectElement = metadataElement.getFirst("dc:subject") ?: return null
        val name = subjectElement.text ?: return null
        with(Subject()) {
            this.name = name
            scheme = subjectElement.properties["opf:authority"]
            code = subjectElement.properties["opf:term"]
            return this
        }
    }

    fun parseContributors(metadataElement: Node, metadata: Metadata, epubVersion: Double) {
        var allContributors: MutableList<Node> = mutableListOf()
        allContributors = allContributors.plus(findContributorsXmlElements(metadataElement)).toMutableList()
        if (epubVersion == 3.0)
            allContributors = allContributors.plus(findContributorsMetaXmlElements(metadataElement)).toMutableList()
        for (contributor in allContributors) {
            parseContributor(contributor, metadataElement, metadata)
        }
    }

    private fun parseContributor(element: Node, metadataElement: Node, metadata: Metadata) {
        val contributor = createContributor(element, metadataElement)

        val eid = element.properties["id"]
        if (eid != null) {
            for (meta in metadataElement.get("meta")!!
                    .filter { it.properties["refines"] == eid && it.properties["property"] == "role" }) {
                meta.text?.let { contributor.roles.add(it) }
            }
        }

        if (contributor.roles.isNotEmpty()) {
            for (role in contributor.roles) {
                when (role) {
                    "aut" -> metadata.authors.add(contributor)
                    "trl" -> metadata.translators.add(contributor)
                    "art" -> metadata.artists.add(contributor)
                    "edt" -> metadata.editors.add(contributor)
                    "ill" -> metadata.illustrators.add(contributor)
                    "clr" -> metadata.colorists.add(contributor)
                    "nrt" -> metadata.narrators.add(contributor)
                    "pbl" -> metadata.publishers.add(contributor)
                    else -> metadata.contributors.add(contributor)
                }
            }
        } else {
            if (element.name == "dc:creator" || element.properties["property"] == "dcterms:contributor") {
                metadata.authors.add(contributor)
            } else if (element.name == "dc:publisher" || element.properties["property"] == "dcterms:publisher") {
                metadata.publishers.add(contributor)
            } else {
                metadata.contributors.add(contributor)
            }
        }
    }

    private fun createContributor(element: Node, metadata: Node) : Contributor {
        val contributor = Contributor()
        contributor.multilangName.singleString = element.text
        contributor.multilangName.multiString = multiString(element, metadata)
        element.properties["opf:role"]?.let { contributor.roles.add(it) }
        element.properties["opf:file-as"]?.let { contributor.sortAs = it }
        return contributor
    }

    fun parseMediaDurations(metadataElement: Node, otherMetadata: MutableList<MetadataItem>) : MutableList<MetadataItem> {
        var metadata = otherMetadata
        val metas = metadataElement.get("meta")!!
        if (metas.isEmpty())
            return metadata
        val mediaDurationItems = metas.filter { it.properties["property"] == "media:duration" }
        if (mediaDurationItems.isEmpty())
            return metadata
        for (mediaDurationItem in mediaDurationItems) {
            val item = MetadataItem()
            item.property = mediaDurationItem.properties["refines"]
            item.value = mediaDurationItem.text
            metadata = otherMetadata.plus(item).toMutableList()
        }
        return metadata
    }

    // Return the XML element corresponding to the main title (title having
    // `<meta refines="#.." property="title-type" id="title-type">main</meta>`
    // - Parameters:
    //   - titles: The titles XML elements array.
    //   - metadata: The Publication Metadata XML object.
    // - Returns: The main title XML element.
    private fun getMainTitleElement(titles: List<Node>, metadata: Node): Node? {
        val possibleTitles = titles.filter { it.properties["id"] != null }
        if (possibleTitles.isEmpty())
            return null
        for (title in possibleTitles) {
            for (meta in metadata.get("meta")!!.filter {
                it.properties["refines"] == "#${title.properties["id"]}"
                        && it.properties["property"] == "title-type"
                        && it.text == "main"}){
                return meta
            }
        }
        return null
    }

    private fun findContributorsXmlElements(metadata: Node) : List<Node>{
        val allContributors: MutableList<Node> = mutableListOf()

        metadata.get("dc:publisher")?.let { allContributors.plusAssign(it.toMutableList()) }
        metadata.get("dc:creator")?.let { allContributors.plusAssign(it.toMutableList()) }
        metadata.get("dc:contributor")?.let { allContributors.plusAssign(it.toMutableList()) }
        return allContributors
    }

    private fun findContributorsMetaXmlElements(metadata: Node) =
            metadata.get("meta")!!.filter { it.properties["property"] == "dcterms:publisher" }.toMutableList()
                    .plus(metadata.get("meta")!!.filter { it.properties["property"] == "dcterms:creator" }).toMutableList()
                .plus(metadata.get("meta")!!.filter { it.properties["property"] == "dcterms:contributor" }).toMutableList()

    // Return an array of lang:string, defining the multiple representations of
    // a string in different languages.
    // - Parameters:
    // - element: The element to parse (can be a title or a contributor).
    // - metadata: The metadata XML element.
    private fun multiString(element: Node, metadata: Node): Map<String, String> {
        val multiString: MutableMap<String, String> = mutableMapOf()

        val elementId = element.properties["id"] ?: return multiString
        val altScriptMetas = metadata.get("meta")!!.filter {
            it.properties["refines"] == "#$elementId"
                    && it.properties["property"] == "alternate-script"
        }
        if (altScriptMetas.isEmpty())
            return multiString
        for (altScriptMeta in altScriptMetas) {
            val title = altScriptMeta.text
            val lang = altScriptMeta.properties["xml:lang"]
            if (title != null && lang != null)
                multiString[lang] = title
        }
        if (!multiString.isEmpty()) {
            val publicationDefaultLanguage = metadata.get("dc:language")?.first()?.text
                    ?: throw Exception("No language")
            val lang = element.properties["xml:lang"] ?: publicationDefaultLanguage
            val value = element.text ?: ""
            multiString[lang] = value
        }
        return multiString
    }

}
