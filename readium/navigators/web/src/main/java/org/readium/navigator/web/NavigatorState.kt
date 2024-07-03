package org.readium.navigator.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Composable
public fun rememberNavigatorState(): NavigatorState {
    return remember {
        NavigatorState()
    }
}

@Stable
public class NavigatorState
