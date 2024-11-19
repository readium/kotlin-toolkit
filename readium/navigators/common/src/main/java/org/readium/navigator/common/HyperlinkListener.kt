/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

/**
 * This listener lets you decide what to do when hyperlinks are activated, whether they point to
 * a readingOrder item, a non-linear resource or external content.
 */
@ExperimentalReadiumApi
public interface HyperlinkListener {

    public fun onReadingOrderLinkActivated(location: HyperlinkLocation, context: LinkContext?)

    public fun onNonLinearLinkActivated(location: HyperlinkLocation, context: LinkContext?)

    public fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?)
}

@ExperimentalReadiumApi
public data class HyperlinkLocation(
    public val href: Url,
    public val fragment: String? = null,
)

@ExperimentalReadiumApi
public sealed interface LinkContext

@ExperimentalReadiumApi
public data class FootnoteContext(
    public val noteContent: String,
) : LinkContext

@ExperimentalReadiumApi
public class NullHyperlinkListener : HyperlinkListener {
    override fun onReadingOrderLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
    }

    override fun onNonLinearLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
    }
}

/**
 * A [HyperlinkListener] following links to readingOrder items.
 *
 * Activations of links to external content or non-linear items are ignored by default.
 * To handle them, pass [onNonLinearLinkActivated] and [onExternalLinkActivated] delegates.
 */
@ExperimentalReadiumApi
@Composable
public fun <L : Location> defaultHyperlinkListener(
    controller: NavigationController<L, *>,
    shouldFollowReadingOrderLink: (HyperlinkLocation, LinkContext?) -> Boolean = { _, _ -> true },
    onNonLinearLinkActivated: (HyperlinkLocation, LinkContext?) -> Unit = { _, _ -> },
    onExternalLinkActivated: (AbsoluteUrl, LinkContext?) -> Unit = { _, _ -> },
): HyperlinkListener {
    val coroutineScope = rememberCoroutineScope()

    return DefaultHyperlinkListener(
        coroutineScope = coroutineScope,
        controller = controller,
        shouldFollowReadingOrderLink = shouldFollowReadingOrderLink,
        onNonLinearLinkActivatedDelegate = onNonLinearLinkActivated,
        onExternalLinkActivatedDelegate = onExternalLinkActivated
    )
}

@ExperimentalReadiumApi
private class DefaultHyperlinkListener<L : Location>(
    private val coroutineScope: CoroutineScope,
    private val controller: NavigationController<L, *>,
    private val shouldFollowReadingOrderLink: (HyperlinkLocation, LinkContext?) -> Boolean,
    private val onNonLinearLinkActivatedDelegate: (HyperlinkLocation, LinkContext?) -> Unit,
    private val onExternalLinkActivatedDelegate: (AbsoluteUrl, LinkContext?) -> Unit,
) : HyperlinkListener {

    override fun onReadingOrderLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
        if (shouldFollowReadingOrderLink(location, context)) {
            coroutineScope.launch { controller.goTo(location) }
        }
    }

    override fun onNonLinearLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
        onNonLinearLinkActivatedDelegate(location, context)
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
        onExternalLinkActivatedDelegate(url, context)
    }
}
