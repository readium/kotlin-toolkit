/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.getEquivalent
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.StringResource

class TestContainer(
    private val resources: Map<Url, String> = emptyMap(),
) : Container<Resource> {

    companion object {

        operator fun invoke(vararg entry: Pair<Url, String>): TestContainer =
            TestContainer(entry.toMap())
    }

    override val entries: Set<Url> =
        resources.keys

    override fun get(url: Url): Resource? =
        resources.getEquivalent(url)?.let { StringResource(it) }

    override fun close() {}
}
