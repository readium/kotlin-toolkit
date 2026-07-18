/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Translated from the vendored `java.nio.channels` mirror previously located in
// the vendored Java channel shims (same package, deleted at the end of phase 05), themselves
// derived from AOSP.
//
// The original hierarchy also had `AsynchronousCloseException`, `ClosedByInterruptException` and
// `NonReadableChannelException`; they are referenced nowhere in the zip stack (channels are
// cooperative suspending code, and the read-only adapters can always read), so they were not
// ported.

package org.readium.r2.shared.util.zip.jvm

import okio.IOException

/**
 * A [ClosedChannelException] is thrown when a channel is closed for the type of operation
 * attempted.
 */
internal class ClosedChannelException : IOException()

/**
 * A [NonWritableChannelException] is thrown when attempting to write to a channel that is not
 * open for writing.
 */
internal class NonWritableChannelException : IllegalStateException()
