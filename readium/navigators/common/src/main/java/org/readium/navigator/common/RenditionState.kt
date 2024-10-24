package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface RenditionState<N : Navigator<*, *, *>> {

    public val navigator: N?
}
