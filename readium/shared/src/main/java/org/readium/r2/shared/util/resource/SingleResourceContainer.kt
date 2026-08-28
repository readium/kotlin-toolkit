/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container

/** A [Container] for a single [Resource]. */
public class SingleResourceContainer(
    private val entryUrl: Url,
    private val resource: Resource,
) : Container<Resource> {

    override val entries: Set<Url> = setOf(entryUrl)

    override fun get(url: Url): Resource? {
        if (!url.removeFragment().removeQuery().isEquivalent(entryUrl)) {
            return null
        }

        return resource.borrow()
    }

    override fun close() {
        resource.close()
    }
}
