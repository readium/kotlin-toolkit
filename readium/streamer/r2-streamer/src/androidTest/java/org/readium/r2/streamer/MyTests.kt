package org.readium.r2.streamer

import org.junit.Test
import org.readium.r2.streamer.Containers.Container
import org.readium.r2.streamer.Parser.EpubParser
import org.readium.r2.shared.Publication

class MyTests{

    private var pubBox = EpubParser().parse("/sdcard/Download/book.epub")
    private var publication: Publication
    private var container: Container

    init {
        publication = pubBox!!.publication
        container = pubBox!!.container
    }

    @Test
    fun checkMetadata(){
        var allOk = true
        with (publication.metadata){
            allOk = (title == "Burn-out"
                    && publishers.first().name == "Stéphane fatrov"
                    && languages.first() == "en"
                    && authors.first().name == "Stéphane fatrov"
                    && identifier == "urn:uuid:16af19d1-3c99-4998-991f-50ee5c48aaa6")
        }
        assert(allOk)
    }
}