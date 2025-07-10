/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

/**
 * This listener lets you decide what to do when hyperlinks are activated, whether they point to
 * a readingOrder item, a non-linear resource or external content.
 */
@ExperimentalReadiumApi
public interface HyperlinkListener {

    public fun onReadingOrderLinkActivated(url: Url, context: LinkContext?)

    public fun onNonLinearLinkActivated(url: Url, context: LinkContext?)

    public fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?)
}

@ExperimentalReadiumApi
public data class HyperlinkLocation(
    public val href: Url,
    public val fragment: String? = null,
) {
    public constructor(link: Link) : this(
        href = link.url(),
        fragment = link.url().fragment
    )
}

@ExperimentalReadiumApi
public sealed interface LinkContext

@ExperimentalReadiumApi
public data class FootnoteContext(
    public val noteContent: String,
) : LinkContext

@ExperimentalReadiumApi
public class NullHyperlinkListener : HyperlinkListener {
    override fun onReadingOrderLinkActivated(url: Url, context: LinkContext?) {
    }

    override fun onNonLinearLinkActivated(url: Url, context: LinkContext?) {
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
public fun <L : ExportableLocation> defaultHyperlinkListener(
    controller: NavigationController<L, *>?,
    shouldFollowReadingOrderLink: (NavigationController<L, *>).(Url, LinkContext?) -> Boolean = { _, _ -> true },
    onNonLinearLinkActivated: (NavigationController<L, *>).(Url, LinkContext?) -> Unit = { _, _ -> },
    onExternalLinkActivated: (NavigationController<L, *>).(AbsoluteUrl, LinkContext?) -> Unit = { _, _ -> },
): HyperlinkListener {
    val coroutineScope = rememberCoroutineScope()

    return remember(
        controller,
        shouldFollowReadingOrderLink,
        onNonLinearLinkActivated,
        onExternalLinkActivated
    ) {
        if (controller == null) {
            NullHyperlinkListener()
        } else {
            DefaultHyperlinkListener(
                coroutineScope = coroutineScope,
                controller = controller,
                shouldFollowReadingOrderLink = { link, context ->
                    controller.shouldFollowReadingOrderLink(link, context)
                },
                onNonLinearLinkActivatedDelegate = { link, context ->
                    controller.onNonLinearLinkActivated(link, context)
                },
                onExternalLinkActivatedDelegate = { url, context ->
                    controller.onExternalLinkActivated(url, context)
                }
            )
        }
    }
}

@ExperimentalReadiumApi
private class DefaultHyperlinkListener<L : ExportableLocation>(
    private val coroutineScope: CoroutineScope,
    private val controller: NavigationController<L, *>,
    private val shouldFollowReadingOrderLink: (Url, LinkContext?) -> Boolean,
    private val onNonLinearLinkActivatedDelegate: (Url, LinkContext?) -> Unit,
    private val onExternalLinkActivatedDelegate: (AbsoluteUrl, LinkContext?) -> Unit,
) : HyperlinkListener {

    override fun onReadingOrderLinkActivated(url: Url, context: LinkContext?) {
        if (shouldFollowReadingOrderLink(url, context)) {
            coroutineScope.launch { controller.goTo(url) }
        }
    }

    override fun onNonLinearLinkActivated(url: Url, context: LinkContext?) {
        onNonLinearLinkActivatedDelegate(url, context)
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?) {
        onExternalLinkActivatedDelegate(url, context)
    }
}
