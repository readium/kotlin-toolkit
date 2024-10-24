package org.readium.navigator.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
public object NullHyperlinkListener : HyperlinkListener {
    override fun onReadingOrderLinkActivated(url: Url, context: LinkContext?) {
    }

    override fun onResourceLinkActivated(url: Url, context: LinkContext?) {
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
    }
}

@ExperimentalReadiumApi
@Composable
public fun <L : Location> defaultHyperlinkListener(
    navigator: Navigator<L, *>,
    shouldFollowReadingOrderLink: (Url, LinkContext?) -> Boolean = { _, _ -> true },
    // TODO: shouldFollowResourceLink: (Url, LinkContext?) -> Boolean = { _, _ -> true },
    onExternalLinkActivated: (AbsoluteUrl, LinkContext?) -> Unit = { _, _ -> }
): HyperlinkListener {
    val coroutineScope = rememberCoroutineScope()
    val navigationHistory: MutableState<List<L>> = remember { mutableStateOf(emptyList()) }

    BackHandler(enabled = navigationHistory.value.isNotEmpty()) {
        val previousItem = navigationHistory.value.last()
        navigationHistory.value -= previousItem
        coroutineScope.launch { navigator.goTo(previousItem) }
    }

    val onPreFollowingReadingOrder = {
        navigationHistory.value += navigator.location.value
    }

    return DefaultHyperlinkListener(
        coroutineScope = coroutineScope,
        navigator = navigator,
        shouldFollowReadingOrderLink = shouldFollowReadingOrderLink,
        onPreFollowingReadingOrderLink = onPreFollowingReadingOrder,
        onExternalLinkActivatedDelegate = onExternalLinkActivated
    )
}

@ExperimentalReadiumApi
private class DefaultHyperlinkListener<L : Location>(
    private val coroutineScope: CoroutineScope,
    private val navigator: Navigator<L, *>,
    private val shouldFollowReadingOrderLink: (Url, LinkContext?) -> Boolean,
    private val onPreFollowingReadingOrderLink: () -> Unit,
    private val onExternalLinkActivatedDelegate: (AbsoluteUrl, LinkContext?) -> Unit
) : HyperlinkListener {

    override fun onReadingOrderLinkActivated(url: Url, context: LinkContext?) {
        if (shouldFollowReadingOrderLink(url, context)) {
            onPreFollowingReadingOrderLink()
            coroutineScope.launch { navigator.goTo(Link(url)) }
        }
    }

    override fun onResourceLinkActivated(url: Url, context: LinkContext?) {
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
        onExternalLinkActivatedDelegate(url, context)
    }
}
