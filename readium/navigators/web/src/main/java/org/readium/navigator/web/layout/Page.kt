package org.readium.navigator.web.layout

import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Url

internal data class Page(
    val url: Url,
    val page: Presentation.Page?
)
