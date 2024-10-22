package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface Navigator<R : ReadingOrder> {

    public val readingOrder: R

    public suspend fun goTo(item: Int)
}
