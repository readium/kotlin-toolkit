package org.readium.navigator.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public interface HyperlinkListener {

    public fun onReadingOrderLinkActivated(url: Url, context: LinkContext?)

    public fun onResourceLinkActivated(url: Url, context: LinkContext?)

    public fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?)
}

@ExperimentalReadiumApi
public sealed interface LinkContext

/**
 * @param noteContent Content of the footnote. Look at the [Link.mediaType] for the format
 * of the footnote (e.g. HTML).
 */
@ExperimentalReadiumApi
public data class FootnoteContext(
    public val noteContent: String
) : LinkContext

@ExperimentalReadiumApi
@Composable
public fun <R : ReadingOrder> defaultHyperlinkListener(
    navigatorState: Navigator<R>,
    shouldFollowReadingOrderLink: (Url, LinkContext?) -> Boolean = { _, _ -> true },
    // shouldFollowResourceLink: (Url, LinkContext?) -> Boolean = { _, _ -> true },
    onExternalLinkActivated: (AbsoluteUrl, LinkContext?) -> Unit = { _, _ -> }
): HyperlinkListener {
    val coroutineScope = rememberCoroutineScope()
    val navigationHistory: MutableList<Int> = remember { mutableListOf() }

    BackHandler(enabled = navigationHistory.isNotEmpty()) {
        val previousItem = navigationHistory.removeLast()
        coroutineScope.launch { navigatorState.goTo(previousItem) }
    }

    return DefaultHyperlinkListener(
        coroutineScope = coroutineScope,
        navigatorState = navigatorState,
        navigationHistory = navigationHistory,
        shouldFollowReadingOrderLink = shouldFollowReadingOrderLink,
        onExternalLinkActivatedDelegate = onExternalLinkActivated
    )
}

@ExperimentalReadiumApi
private class DefaultHyperlinkListener<R : ReadingOrder>(
    private val coroutineScope: CoroutineScope,
    private val navigatorState: Navigator<R>,
    private val navigationHistory: MutableList<Int>,
    private val shouldFollowReadingOrderLink: (Url, LinkContext?) -> Boolean,
    private val onExternalLinkActivatedDelegate: (AbsoluteUrl, LinkContext?) -> Unit
) : HyperlinkListener {

    override fun onReadingOrderLinkActivated(url: Url, context: LinkContext?) {
        if (shouldFollowReadingOrderLink(url, context)) {
            val item = checkNotNull(navigatorState.readingOrder.indexOfHref(url))
            navigationHistory.add(item)
            coroutineScope.launch { navigatorState.goTo(item) }
        }
    }

    override fun onResourceLinkActivated(url: Url, context: LinkContext?) {
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
        onExternalLinkActivatedDelegate(url, context)
    }
}
