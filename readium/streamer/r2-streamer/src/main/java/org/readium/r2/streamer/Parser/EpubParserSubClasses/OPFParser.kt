package org.readium.r2.streamer.Parser.EpubParserSubClasses

import android.util.Log
import org.readium.r2.shared.*
import org.readium.r2.streamer.XmlParser.*
import org.readium.r2.streamer.Containers.Container
import org.readium.r2.streamer.Parser.normalize

class OPFParser {

    val smilp = SMILParser()
    private var rootFilePath: String? = null

    fun parseOpf(document: XmlParser, container: Container, epubVersion: Double) : Publication? {
        val publication = Publication()

        rootFilePath = container.rootFile.rootFilePath
        publication.version = epubVersion
        publication.internalData["type"] = "epub"
        publication.internalData["rootfile"] = rootFilePath!!
        if (!parseMetadata(document, publication))
            return null
        parseRessources(document.getFirst("package")!!.getFirst("manifest")!!, publication)
        coverLinkFromMeta(document.root().getFirst("metadata") ?:
                    document.root().getFirst("opf:metadata")!!, publication)
        parseSpine(document.getFirst("package")!!.getFirst("spine")!!, publication)
        //  TODO: ParseMediaOverlay
        return publication
    }

    private fun parseMetadata(document: XmlParser, publication: Publication) : Boolean {
        val metadata = Metadata()
        val mp = MetadataParser()
        val metadataElement: Node? = document
                .root().getFirst("metadata") ?: document.root().getFirst("opf:metadata")
        metadata.multilangTitle = mp.mainTitle(metadataElement!!)
        metadata.identifier = mp.uniqueIdentifier(metadataElement,
                    document.getFirst("package")!!.properties) ?: return false
        metadata.description = metadataElement.getFirst("dc:description")?.text
        metadata.publicationDate = metadataElement.getFirst("dc:date")?.text
        metadata.modified = mp.modifiedDate(metadataElement)
        metadata.source = metadataElement.getFirst("dc:sources")?.text
        mp.subject(metadataElement)?.let { metadata.subjects.add(it) }
        metadata.languages = metadataElement.get("dc:language")?.map { it.text!! }?.toMutableList()
                ?: throw Exception("No language")
        val rightsMap = metadataElement.get("dc:rights")?.map { it.text }
        if (rightsMap != null && rightsMap.isNotEmpty())
            metadata.rights = rightsMap.joinToString { " " }
        mp.parseContributors(metadataElement, metadata, publication.version)
        document.root().getFirst("spine")?.properties?.get("page-progression-direction")?.let {
            metadata.direction = it
        }
        mp.parseRenditionProperties(metadataElement, metadata)
        metadata.otherMetadata = mp.parseMediaDurations(metadataElement, metadata.otherMetadata)
        publication.metadata = metadata
        return true
    }

    private fun parseRessources(manifest: Node, publication: Publication){
        val manifestItems = manifest.get("item")!!
        if (manifestItems.isEmpty())
            return
        for (item in manifestItems){
            val id = item.properties["id"] ?: continue
            val link = linkFromManifest(item)

            // TODO: SMIL for MediaOverlays
//            if (link.typeLink == "application/smil+xml") {
//                val duration = publication.metadata.otherMetadata.first{ it.property == id }
//                        .value?.let { link.duration = Double(smilp.smilTimeToSeconds(duration)) }
//            }
            publication.resources.add(link)
        }
    }

    private fun coverLinkFromMeta(metadata: Node, publication: Publication){
        val coverMeta = metadata.get("meta")!!.firstOrNull { it.properties["name"] == "cover" }
        val coverId = coverMeta?.properties?.get("content")
        val coverLink = publication.resources.firstOrNull {it.title == coverId}
        coverLink?.rel?.add("cover")
    }

    private fun parseSpine(spine: Node, publication: Publication){
        val spineItems = spine.get("itemref")!!
        if (spineItems.isEmpty()) {
            Log.d("Warning", "Spine has no children elements")
            return
        }
        for (item in spineItems){
            val idref = item.properties["idref"]
            val index = publication.resources.indexOfFirst { it.title == idref }
            if (index == -1)
                continue
            item.properties["properties"]?.let {
                val properties = it.split(" ")
                publication.resources[index].properties = parse(properties)
            }
            if (item.properties["linear"]?.toLowerCase() == "no")
                continue
            publication.resources[index].title = null
            publication.spine.add(publication.resources[index])
            publication.resources.removeAt(index)
        }
    }

    /// Parse properties string array and return a Properties object.
    ///
    /// - Parameter propertiesArray: The array of properties strings.
    /// - Returns: The Properties instance created from the strings array info
    private fun parse(propertiesArray: List<String>) : Properties? {
        val properties = Properties()

        for (property in propertiesArray){
            //  Contains
            when (property) {
                "scripted" -> "js"
                "mathml" -> "onix-record"
                "svg" -> "svg"
                "xmp-record" -> "xmp"
                "remote-resources" -> "remote-resources"
                else -> null
            }?.let { properties.contains.add(it)}
            //  Page
            when (property){
                "page-spread-left" -> "left"
                "page-spread-right" -> "right"
                "page-spread-center" -> "center"
                else -> null
            }?.let { properties.page = it }
            //  Spread
            when (property){
                "rendition:spread-node" -> "none"
                "rendition:spread-auto" -> "auto"
                "rendition:spread-landscape" -> "landscape"
                "rendition:spread-portrait" -> "portrait"
                "rendition:spread-both" -> "both"
                else -> null
            }?.let { properties.spread = it }
            //  Layout
            when (property){
                "rendition:layout-reflowable" -> "reflowable"
                "rendition:layout-pre-paginated" -> "fixed"
                else -> null
            }?.let { properties.layout = it }
            //  Orientation
            when (property){
                "rendition:orientation-auto" -> "auto"
                "rendition:orientation-landscape" -> "landscape"
                "rendition:orientation-portrait" -> "portrait"
                else -> null
            }?.let { properties.orientation = it }
            //  Rendition
            when (property){
                "rendition:flow-auto" -> "auto"
                "rendition:flow-paginated" -> "paginated"
                "rendition:flow-scrolled-continuous" -> "scrolled-continuous"
                "rendition:flow-scrolled-doc" -> "scrolled"
                else -> null
            }?.let { properties.overflow = it }
        }

        return properties
    }

    private fun linkFromManifest(item: Node) : Link {
        val link = Link()

        link.title = item.properties["id"]
        link.href = normalize(rootFilePath!!, item.properties["href"])
        link.typeLink = item.properties["media-type"]
        item.properties["properties"]?.let {
            val properties = it.split("\\s+")
            with(properties){
                if (contains("nav"))
                    link.rel.add("contents")
                if (contains("cover-image"))
                    link.rel.add("cover")
            }
        }
        return link
    }

}