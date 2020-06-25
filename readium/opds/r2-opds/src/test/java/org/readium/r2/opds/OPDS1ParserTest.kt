package org.readium.r2.opds

import org.joda.time.DateTime
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.opds.OpdsMetadata
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.Properties
import java.net.URL
import java.util.*

class OPDS1ParserTest {

    val fixtures = Fixtures("opds1")

    @Test fun `parse navigation feed`() {
        val parseData = parse("nav-feed.atom")
        assertEquals(
            ParseData(
                feed = Feed(
                    title = "OPDS Catalog Root Example",
                    type = 1,
                    href = URL("https://example.com"),
                    metadata = OpdsMetadata(
                        title = "OPDS Catalog Root Example",
                        modified = parseDate("2010-01-10T10:03:10Z")
                    ),
                    links = mutableListOf(
                        Link(
                            href = "https://example.com/opds-catalogs/root.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=navigation",
                            rels = setOf("self"),
                            properties = Properties()
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/root.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=navigation",
                            rels = setOf("start")
                        )
                    ),
                    navigation = mutableListOf(
                        Link(
                            href = "https://example.com/opds-catalogs/popular.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            title = "Popular Publications",
                            rels = setOf("http://opds-spec.org/sort/popular")
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/new.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            title = "New Publications",
                            rels = setOf("http://opds-spec.org/sort/new")
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/unpopular.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            title = "Unpopular Publications",
                            rels = setOf("subsection")
                        )
                    )
                ),
                publication = null,
                type = 1
            ),
            parseData
        )
    }

    @Test fun `parse acquisition feed`() {
        val parseData = parse("acquisition-feed.atom")
        assertEquals(
            ParseData(
                feed = Feed(
                    title = "Unpopular Publications",
                    type = 1,
                    href = URL("https://example.com"),
                    metadata = OpdsMetadata(
                        title = "Unpopular Publications",
                        modified = parseDate("2010-01-10T10:01:11Z")
                    ),
                    links = mutableListOf(
                        Link(
                            href = "https://example.com/opds-catalogs/vampire.farming.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            rels = setOf("related")
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/unpopular.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            rels = setOf("self")
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/root.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=navigation",
                            rels = setOf("start")
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/root.xml",
                            type = "application/atom+xml;profile=opds-catalog;kind=navigation",
                            rels = setOf("up")
                        )
                    ),
                    publications = mutableListOf(
                        Publication(
                            metadata = Metadata(
                                identifier = "urn:uuid:6409a00b-7bf2-405e-826c-3fdff0fd0734",
                                localizedTitle = LocalizedString("Bob, Son of Bob"),
                                modified = parseDate("2010-01-10T10:01:11Z"),
                                published = null,
                                languages = listOf("en"),
                                subjects = listOf(
                                    Subject(
                                        localizedName = LocalizedString("FICTION / Men's Adventure"),
                                        scheme = "http://www.bisg.org/standards/bisac_subject/index.html",
                                        code = "FIC020000"
                                    )
                                ),
                                authors = listOf(
                                    Contributor(
                                        localizedName = LocalizedString("Bob the Recursive"),
                                        links = listOf(Link(href = "http://opds-spec.org/authors/1285"))
                                    )
                                ),
                                description = "The story of the son of the Bob and the gallant part he played in the lives of a man and a woman."
                            ),
                            links = listOf(
                                Link(
                                    href = "https://example.com/covers/4561.thmb.gif",
                                    type = "image/gif",
                                    rels = setOf("http://opds-spec.org/image/thumbnail")
                                ),
                                Link(
                                    href = "https://example.com/opds-catalogs/entries/4571.complete.xml",
                                    type = "application/atom+xml;type=entry;profile=opds-catalog",
                                    title = "Complete Catalog Entry for Bob, Son of Bob",
                                    rels = setOf("alternate")
                                ),
                                Link(
                                    href = "https://example.com/content/free/4561.epub",
                                    type = "application/epub+zip",
                                    rels = setOf("http://opds-spec.org/acquisition")
                                ),
                                Link(
                                    href = "https://example.com/content/free/4561.mobi",
                                    type = "application/x-mobipocket-ebook",
                                    rels = setOf("http://opds-spec.org/acquisition")
                                )
                            ),
                            otherCollections = listOf(
                                PublicationCollection(
                                    role = "images",
                                    links = listOf(
                                        Link(
                                            href = "https://example.com/covers/4561.lrg.png",
                                            type = "image/png",
                                            rels = setOf("http://opds-spec.org/image")
                                        )
                                    )
                                )
                            ),
                            type = Publication.TYPE.EPUB
                        ),
                        Publication(
                            metadata = Metadata(
                                identifier = "urn:uuid:7b595b0c-e15c-4755-bf9a-b7019f5c1dab",
                                localizedTitle = LocalizedString("Modern Online Philately"),
                                modified = parseDate("2010-01-10T10:01:10Z"),
                                languages = listOf("en"),
                                authors = listOf(
                                    Contributor(
                                        localizedName = LocalizedString("Stampy McGee"),
                                        links = listOf(Link(href = "http://opds-spec.org/authors/21285"))
                                    ),
                                    Contributor(
                                        localizedName = LocalizedString("Alice McGee"),
                                        links = listOf(Link(href = "http://opds-spec.org/authors/21284"))
                                    ),
                                    Contributor(
                                        localizedName = LocalizedString("Harold McGee"),
                                        links = listOf(Link(href = "http://opds-spec.org/authors/21283"))
                                    )
                                ),
                                publishers = listOf(
                                    Contributor(localizedName = LocalizedString("StampMeOnline, Inc."))
                                ),
                                description = "The definitive reference for the web-curious philatelist."
                            ),
                            links = listOf(
                                Link(
                                    href = "https://example.com/content/buy/11241.epub",
                                    type = "application/epub+zip",
                                    rels = setOf("http://opds-spec.org/acquisition/buy"),
                                    properties = Properties(mapOf("price" to mapOf("currency" to "USD", "value" to 18.99)))
                                )
                            ),
                            otherCollections = listOf(
                                PublicationCollection(
                                    role = "images",
                                    links = listOf(
                                        Link(
                                            href = "https://example.com/covers/11241.lrg.jpg",
                                            type = "image/jpeg",
                                            rels = setOf("http://opds-spec.org/image")
                                        )
                                    )
                                )
                            ),
                            type = Publication.TYPE.EPUB
                        )
                    ),
                    facets = mutableListOf(
                        Facet(
                            title = "Categories",
                            metadata = OpdsMetadata(title = "Categories"),
                            links = mutableListOf(
                                Link(
                                    href = "https://example.com/sci-fi",
                                    title = "Science-Fiction",
                                    rels = setOf("http://opds-spec.org/facet")
                                ),
                                Link(
                                    href = "https://example.com/romance",
                                    title = "Romance",
                                    rels = setOf("http://opds-spec.org/facet"),
                                    properties = Properties(mapOf("numberOfItems" to 600))
                                )
                            )
                        )
                    )
                ),
                publication = null,
                type = 1
            ),
            parseData
        )
    }

    @Test fun `parse full entry`() {
        val parseData = parse("entry.atom")
        assertEquals(
            ParseData(
                feed = null,
                publication = Publication(
                    metadata = Metadata(
                        identifier = "urn:uuid:6409a00b-7bf2-405e-826c-3fdff0fd0734",
                        localizedTitle = LocalizedString("Bob, Son of Bob"),
                        modified = parseDate("2010-01-10T10:01:11Z"),
                        languages = listOf("en"),
                        subjects = listOf(
                            Subject(
                                localizedName = LocalizedString("FICTION / Men's Adventure"),
                                scheme = "http://www.bisg.org/standards/bisac_subject/index.html",
                                code = "FIC020000"
                            )
                        ),
                        authors = listOf(
                            Contributor(
                                localizedName = LocalizedString("Bob the Recursive"),
                                links = listOf(Link(href = "http://opds-spec.org/authors/1285"))
                            )
                        ),
                        description = "The story of the son of the Bob and the gallant part he played in the lives of a man and a woman. Bob begins his humble life under the wandering eye of his senile mother, but quickly learns how to escape into the wilder world. Follow Bob as he uncovers his father's past and uses those lessons to improve the lives of others."
                    ),
                    links = listOf(
                        Link(
                            href = "https://example.com/covers/4561.thmb.gif",
                            type = "image/gif",
                            rels = setOf("http://opds-spec.org/image/thumbnail")
                        ),
                        Link(
                            href = "https://example.com/opds-catalogs/entries/4571.complete.xml",
                            type = "application/atom+xml;type=entry;profile=opds-catalog",
                            rels = setOf("self")
                        ),
                        Link(
                            href = "https://example.com/content/free/4561.epub",
                            type = "application/epub+zip",
                            rels = setOf("http://opds-spec.org/acquisition")
                        ),
                        Link(
                            href = "https://example.com/content/free/4561.mobi",
                            type = "application/x-mobipocket-ebook",
                            rels = setOf("http://opds-spec.org/acquisition")
                        )
                    ),
                    otherCollections = listOf(
                        PublicationCollection(
                            role = "images",
                            links = listOf(
                                Link(
                                    href = "https://example.com/covers/4561.lrg.png",
                                    type = "image/png",
                                    rels = setOf("http://opds-spec.org/image")
                                )
                            )
                        )
                    ),
                    type = Publication.TYPE.EPUB
                ),
                type = 1
            ),
            parseData
        )
    }

    private fun parse(filename: String, url: URL = URL("https://example.com")): ParseData =
        OPDS1Parser.parse(fixtures.bytesAt(filename), url)

    private fun parseDate(string: String): Date =
        DateTime.parse(string).toDate()

}