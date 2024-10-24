package org.readium.navigator.common

import androidx.compose.runtime.State
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface NavigatorState<N : Navigator<*, *, *>> {

    public val state: State<NavigatorInitializationState<N>>
}

@ExperimentalReadiumApi
public sealed interface NavigatorInitializationState<N> {

    public val navigator: N?

    public class Pending<N> : NavigatorInitializationState<N> {

        override val navigator: Nothing? = null
    }

    public data class Initialized<N>(
        override val navigator: N
    ) : NavigatorInitializationState<N>
}
