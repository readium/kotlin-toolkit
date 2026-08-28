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

    /**
     * Called when a link to a reading order item is activated.
     */
    public fun onReadingOrderLinkActivated(url: Url, context: LinkContext?)

    /**
     * Called when a link to a non-linear item is activated.
     */
    public fun onNonLinearLinkActivated(url: Url, context: LinkContext?)

    /**
     * Called when a link to an external URL was activated in the navigator.
     *
     * If it is an HTTP URL, you should open it with a `CustomTabsIntent` or `WebView`, for
     * example:
     *
     * ```kotlin
     * override fun onExternalLinkActivated(url: AbsoluteUrl) {
     *     if (!url.isHttp) return
     *
     *     val context = requireActivity()
     *     val uri = url.toUri()
     *
     *     try {
     *         CustomTabsIntent.Builder()
     *             .build()
     *             .launchUrl(context, uri)
     *     } catch (e: ActivityNotFoundException) {
     *         context.startActivity(Intent(Intent.ACTION_VIEW, uri))
     *     }
     * }
     * ```
     */
    public fun onExternalLinkActivated(url: AbsoluteUrl, context: LinkContext?)
}

/**
 * This holds additional information about a link. For instance, it will be an instance of [FootnoteContext] if the
 * link is a reference mark.
 */
@ExperimentalReadiumApi
public sealed interface LinkContext

/**
 * @param noteContent Content of the footnote. Look at the [Link.mediaType] for the format
 * of the footnote (e.g. HTML).
 */
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
 * The default [HyperlinkListener], following links to readingOrder items if
 * [shouldFollowReadingOrderLink] returns true, which is always the case by default.
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
