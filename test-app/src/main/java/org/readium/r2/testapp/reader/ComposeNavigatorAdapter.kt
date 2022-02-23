package org.readium.r2.testapp.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator3.NavigatorState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class ComposeNavigatorAdapter(
    private val navigatorState: NavigatorState
) : Navigator {
    override val publication: Publication =
        navigatorState.publication

    override val currentLocator: StateFlow<Locator> =
        MutableStateFlow((Locator(href= "#", type="")))

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }
}