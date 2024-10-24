package org.readium.navigator.common

import androidx.compose.runtime.State
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface NavigatorState<N : Navigator<*, *, *>> {

    public sealed interface InitializationState<N> {
        public class Pending<N> : InitializationState<N> {
            override val navigator: Nothing? = null
        }

        public data class Initialized<N>(
            override val navigator: N
        ) : InitializationState<N>

        public val navigator: N?
    }

    public val initState: State<InitializationState<N>>

    public val navigator: N? get() =
        initState.value.navigator
}
