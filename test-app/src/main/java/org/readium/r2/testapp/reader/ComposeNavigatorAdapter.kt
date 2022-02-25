package org.readium.r2.testapp.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator3.NavigatorState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression

class ComposeNavigatorAdapter(
    private val navigatorState: NavigatorState
) : VisualNavigator {
    private val coroutineScope: CoroutineScope = MainScope()

    private fun launchAndRun(runnable: suspend () -> Unit, callback: () -> Unit) =
        coroutineScope.launch { runnable() }.invokeOnCompletion { callback() }

    override val readingProgression: ReadingProgression
        get() = when (navigatorState.readingProgression) {
            org.readium.r2.navigator3.ReadingProgression.RTL -> ReadingProgression.RTL
            org.readium.r2.navigator3.ReadingProgression.LTR -> ReadingProgression.LTR
            org.readium.r2.navigator3.ReadingProgression.TTB -> ReadingProgression.TTB
            org.readium.r2.navigator3.ReadingProgression.BTT -> ReadingProgression.BTT
        }

    override val publication: Publication =
        navigatorState.publication

    override val currentLocator: StateFlow<Locator> =
        MutableStateFlow((Locator(href= "#", type="")))

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ navigatorState.go(locator) }, completion)
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ navigatorState.go(link) }, completion)
        return true
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ navigatorState.goForward() }, completion)
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ navigatorState.goBackward() }, completion)
        return true
    }
}