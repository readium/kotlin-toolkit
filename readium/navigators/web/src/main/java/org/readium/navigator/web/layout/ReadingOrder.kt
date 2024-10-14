package org.readium.navigator.web.layout

import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Url

internal data class ReadingOrder(
    val items: List<ReadingOrderItem>
)

internal data class ReadingOrderItem(
    val href: Url,
    val page: Presentation.Page?
)
