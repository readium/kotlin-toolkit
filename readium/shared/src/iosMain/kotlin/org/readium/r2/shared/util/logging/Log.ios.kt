/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.logging

import platform.Foundation.NSLog

internal actual fun defaultLogger(): ReadiumLogger = NSLogLogger

private object NSLogLogger : ReadiumLogger {
    override fun log(severity: Severity, tag: String, message: String, throwable: Throwable?) {
        val fullMessage = throwable
            ?.let { message + "\n" + it.stackTraceToString() }
            ?: message

        NSLog("%s [%s] %s", severity.name.uppercase(), tag, fullMessage)
    }
}
