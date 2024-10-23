package org.readium.navigator.pdf

import org.readium.navigator.common.ReadingOrder
import org.readium.r2.shared.util.Url

public data class PdfReadingOrder(
    override val items: List<PdfReadingOrderItem>
) : ReadingOrder

public data class PdfReadingOrderItem(
    override val href: Url
) : ReadingOrder.Item
