/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.guided

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Url

/**
 * A guided navigation document.
 */
@ExperimentalReadiumApi
public data class GuidedNavigationDocument(
    val links: List<Link> = emptyList(),
    val guided: List<GuidedNavigationObject>,
)

/**
 * A guided navigation object.
 */
@ExperimentalReadiumApi
public data class GuidedNavigationObject(
    val children: List<GuidedNavigationObject> = emptyList(),
    val roles: Set<GuidedNavigationRole> = emptySet(),
    val refs: Set<GuidedNavigationRef> = emptySet(),
    val text: GuidedNavigationText? = null,
)

/**
 * A string containing some SSML markup.
 */
@ExperimentalReadiumApi
@JvmInline
public value class SsmlString(public val value: String)

/**
 * Text holder for a guided navigation object.
 */
@ExperimentalReadiumApi
public data class GuidedNavigationText(
    val plain: String?,
    val ssml: SsmlString? = null,
    val language: Language? = null,
) {
    init {
        require(plain != null || ssml != null)
        require(plain == null || plain.isNotEmpty())
        require(ssml == null || ssml.value.isNotEmpty())
    }
}

/**
 * A reference to external content.
 */
@ExperimentalReadiumApi
public sealed interface GuidedNavigationRef {
    public val url: Url
}

/**
 * A reference to external text content.
 */
@ExperimentalReadiumApi
public data class GuidedNavigationTextRef(
    override val url: Url,
) : GuidedNavigationRef

/**
 * A reference to external image content.
 */
@ExperimentalReadiumApi
public data class GuidedNavigationImageRef(
    override val url: Url,
) : GuidedNavigationRef

/**
 * A reference to external audio content.
 */
@ExperimentalReadiumApi
public data class GuidedNavigationAudioRef(
    override val url: Url,
) : GuidedNavigationRef
