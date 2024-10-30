package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * The state of the rendition, giving access to a [Navigator] after the first composition.
 */
@ExperimentalReadiumApi
public interface RenditionState<N : Navigator<*, *>> {

    public val navigator: N?
}
