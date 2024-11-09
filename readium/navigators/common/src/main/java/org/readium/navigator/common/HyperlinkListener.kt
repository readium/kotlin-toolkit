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

@ExperimentalReadiumApi
public interface HyperlinkListener {

    public fun onReadingOrderLinkActivated(location: HyperlinkLocation, context: LinkContext?)

    public fun onResourceLinkActivated(location: HyperlinkLocation, context: LinkContext?)

    public fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?)
}

@ExperimentalReadiumApi
public interface HyperlinkLocation : GoLocation {
    public val href: Url
    public val fragment: String?
}

@ExperimentalReadiumApi
public sealed interface LinkContext

@ExperimentalReadiumApi
public data class FootnoteContext(
    public val noteContent: String
) : LinkContext

@ExperimentalReadiumApi
public class NullHyperlinkListener : HyperlinkListener {
    override fun onReadingOrderLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
    }

    override fun onResourceLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
    }
}

@ExperimentalReadiumApi
@Composable
public fun <L : Location> defaultHyperlinkListener(
    controller: RenditionController<L, *>,
    shouldFollowReadingOrderLink: (HyperlinkLocation, LinkContext?) -> Boolean = { _, _ -> true },
    // TODO: shouldFollowResourceLink: (HyperlinkLocation, LinkContext?) -> Boolean = { _, _ -> true },
    onExternalLinkActivated: (AbsoluteUrl, LinkContext?) -> Unit = { _, _ -> }
): HyperlinkListener {
    val coroutineScope = rememberCoroutineScope()

    return DefaultHyperlinkListener(
        coroutineScope = coroutineScope,
        controller = controller,
        shouldFollowReadingOrderLink = shouldFollowReadingOrderLink,
        onExternalLinkActivatedDelegate = onExternalLinkActivated
    )
}

@ExperimentalReadiumApi
private class DefaultHyperlinkListener<L : Location>(
    private val coroutineScope: CoroutineScope,
    private val controller: RenditionController<L, *>,
    private val shouldFollowReadingOrderLink: (HyperlinkLocation, LinkContext?) -> Boolean,
    private val onExternalLinkActivatedDelegate: (AbsoluteUrl, LinkContext?) -> Unit
) : HyperlinkListener {

    override fun onReadingOrderLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
        if (shouldFollowReadingOrderLink(location, context)) {
            coroutineScope.launch { controller.goTo(location) }
        }
    }

    override fun onResourceLinkActivated(location: HyperlinkLocation, context: LinkContext?) {
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
        onExternalLinkActivatedDelegate(url, context)
    }
}
