package org.readium.navigator.pdf

import org.readium.navigator.common.ReadingOrder
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public data class PdfReadingOrder(
    override val items: List<PdfReadingOrderItem>
) : ReadingOrder

@ExperimentalReadiumApi
public data class PdfReadingOrderItem(
    override val href: Url
) : ReadingOrder.Item
