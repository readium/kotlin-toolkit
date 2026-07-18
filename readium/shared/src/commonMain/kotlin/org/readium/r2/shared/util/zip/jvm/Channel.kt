/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Translated from the vendored `java.nio.channels` mirror previously located in
// `:readium:readium-shared-zip-legacy` (same package, Java), itself derived from AOSP.

package org.readium.r2.shared.util.zip.jvm

import org.readium.r2.shared.util.Closeable

/**
 * A channel is a conduit to I/O services covering such items as files, sockets, hardware devices,
 * I/O ports or some software component.
 *
 * Channels are open upon creation, and can be closed explicitly. Once a channel is closed it
 * cannot be re-opened, and any attempts to perform I/O operations on the closed channel result in
 * a [ClosedChannelException].
 *
 * Unlike the original `java.nio` contract, I/O operations are suspending: implementations bridge
 * to suspending Readium sources ([org.readium.r2.shared.util.data.Readable]) and must never block
 * a thread with `runBlocking`.
 */
internal interface Channel : Closeable {

    /**
     * Returns true if this channel is open.
     */
    val isOpen: Boolean
}
