package org.readium.r2.shared

import java.io.Serializable


class CbzPublication() : Serializable {

    /// The version of the publication, if the type needs any.
    /// The metadata (title, identifier, contributors, etc.).
    var metadata: Metadata = Metadata()
    /// org.readium.r2shared.Publication.org.readium.r2shared.Link to special resources which are added to the publication.
    //TODO store in links any extra files found in the CBZ ( like .nfo files .. )
    var extraFileList: MutableList<Link> = mutableListOf()
    /// Table of content of the publication.
    var pageList: MutableList<Link> = mutableListOf()
    /// Extension point for links that shouldn't show up in the manifest.
    var coverLink: Link?  = null
        get() = pageList.firstOrNull()
}