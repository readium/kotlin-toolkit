/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Translated from the vendored `java.nio.channels` mirror previously located in
// `:readium:readium-shared-zip-legacy` (same package, Java), itself derived from AOSP.

package org.readium.r2.shared.util.zip.jvm

/**
 * A [ByteChannel] is both readable and writable.
 *
 * The methods for the byte channel are precisely those defined by readable and writable byte
 * channels.
 *
 * @see ReadableByteChannel
 * @see WritableByteChannel
 */
internal interface ByteChannel : ReadableByteChannel, WritableByteChannel
