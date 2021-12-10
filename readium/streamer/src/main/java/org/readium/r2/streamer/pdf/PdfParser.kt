package org.readium.r2.streamer.pdf

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.InMemoryCoverService
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.streamer.PublicationParser
import org.readium.r2.streamer.container.PublicationContainer
import java.io.File

/**
 * Parses a PDF file into a Readium [Publication].
 */
@PdfSupport
class PdfParser(private val pdfFactory: PdfDocumentFactory)
    : PublicationParser,
    @Suppress("DEPRECATION") org.readium.r2.streamer.parser.PublicationParser {

    override suspend fun parse(asset: PublicationAsset, fetcher: Fetcher, warnings: WarningLogger?): Publication.Builder? =
        _parse(asset, fetcher, asset.name)

    private suspend fun _parse(asset: PublicationAsset, fetcher: Fetcher, fallbackTitle: String): Publication.Builder? {
        if (asset.mediaType() != MediaType.PDF)
            return null

        val fileHref = fetcher.links().firstOrNull { it.mediaType == MediaType.PDF }?.href
            ?: throw Exception("Unable to find PDF file.")
        val document = pdfFactory.open(fetcher.get(fileHref), password = null)
        val tableOfContents = document.outline.toLinks(fileHref)

        val manifest = Manifest(
            metadata = Metadata(
                identifier = document.identifier,
                conformsTo = setOf(Publication.Profile.PDF),
                localizedTitle = LocalizedString(document.title?.ifBlank { null } ?: fallbackTitle),
                authors = listOfNotNull(document.author).map { Contributor(name = it) },
                numberOfPages = document.pageCount
            ),
            readingOrder = listOf(Link(href = fileHref, type = MediaType.PDF.toString())),
            tableOfContents = tableOfContents
        )

        val servicesBuilder = Publication.ServicesBuilder(
            positions = PdfPositionsService.Companion::create,
            cover = document.cover?.let { InMemoryCoverService.createFactory(it) }
        )

        return Publication.Builder(manifest, fetcher, servicesBuilder)
    }

    @Deprecated("This will be removed in the next major version of Readium.")
    @Suppress("DEPRECATION")
    override fun parse(fileAtPath: String, fallbackTitle: String): org.readium.r2.streamer.parser.PubBox? = runBlocking {

        val file = File(fileAtPath)
        val asset = FileAsset(file)
        val baseFetcher = FileFetcher(href = "/${file.name}", file = file)
        val builder = try {
            _parse(asset, baseFetcher, fallbackTitle)
        } catch (e: Exception) {
            return@runBlocking null
        } ?: return@runBlocking null

        val publication = builder.build()
        val container = PublicationContainer(
            publication = publication,
            path = file.canonicalPath,
            mediaType = MediaType.PDF
        )

        org.readium.r2.streamer.parser.PubBox(publication, container)
    }

}