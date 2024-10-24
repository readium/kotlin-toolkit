package org.readium.navigator.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface Configurable<S : Any, P : Any> {

    public val preferences: MutableState<P>

    public val settings: State<S>
}

@ExperimentalReadiumApi
public typealias Settings = org.readium.r2.navigator.preferences.Configurable.Settings

@ExperimentalReadiumApi
public typealias Preferences<P> = org.readium.r2.navigator.preferences.Configurable.Preferences<P>
