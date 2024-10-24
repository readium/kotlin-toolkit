package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
public interface LocatorAdapter<L : Location, G : GoLocation> {

    public fun Locator.toGoLocation(): G

    public fun L.toLocator(): Locator
}
