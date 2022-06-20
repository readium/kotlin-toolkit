/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.SuspendingCloseable
import java.util.*

data class Content(
    val locator: Locator,
    val data: Data,
    val extras: Map<String, Any> = emptyMap()
) {
    interface Data {
        data class Audio(
            val link: Link
        ) : Data

        data class Image(
            val link: Link,
            val description: String?
        ) : Data

        data class Text(
            val role: Role,
            val spans: List<Span>
        ) : Data {
            interface Role {
                data class Heading(val level: Int) : Role
                object Body : Role
                object Caption : Role
                object Footnote : Role
                object Quote : Role
            }

            data class Span(
                val locator: Locator,
                // FIXME: Language type
                val language: String?,
                val text: String,
                val extras: Map<String, Any> = emptyMap()
            ) {
                val locale: Locale?
                    get() = language
                        ?.let { Locale.forLanguageTag(it.replace("_", "-")) }
            }
        }
    }
}

interface ContentIterator : SuspendingCloseable {
    suspend fun previous(): Content?
    suspend fun next(): Content?
}
