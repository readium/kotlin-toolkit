/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.logging

import android.util.Log

internal actual fun defaultLogger(): ReadiumLogger = LogcatLogger

private object LogcatLogger : ReadiumLogger {
    override fun log(severity: Severity, tag: String, message: String, throwable: Throwable?) {
        val fullMessage = throwable
            ?.let { message + "\n" + Log.getStackTraceString(it) }
            ?: message

        when (severity) {
            Severity.Verbose -> Log.v(tag, fullMessage)
            Severity.Debug -> Log.d(tag, fullMessage)
            Severity.Info -> Log.i(tag, fullMessage)
            Severity.Warning -> Log.w(tag, fullMessage)
            Severity.Error -> Log.e(tag, fullMessage)
        }
    }
}
