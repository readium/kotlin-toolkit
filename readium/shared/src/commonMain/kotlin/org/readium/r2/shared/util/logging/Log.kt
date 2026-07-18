/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.logging

import org.readium.r2.shared.InternalReadiumApi

/**
 * Severity of a log message.
 */
public enum class Severity {
    Verbose,
    Debug,
    Info,
    Warning,
    Error,
}

/**
 * Destination for the log messages emitted by Readium.
 *
 * Set an implementation on [ReadiumLog.logger] to redirect the logs to your own logging framework.
 */
public interface ReadiumLogger {
    public fun log(severity: Severity, tag: String, message: String, throwable: Throwable?)
}

/**
 * Entry point for the Readium logging facade.
 *
 * By default, logs go to the platform logger (Logcat on Android, NSLog on iOS). Apps can plug
 * their own [ReadiumLogger], or disable logging entirely by setting [logger] to `null`.
 */
public object ReadiumLog {

    /** Destination of the Readium log messages; `null` disables logging. */
    public var logger: ReadiumLogger? = defaultLogger()

    private const val TAG: String = "Readium"

    @InternalReadiumApi
    public fun v(message: String, throwable: Throwable? = null) {
        log(Severity.Verbose, message, throwable)
    }

    @InternalReadiumApi
    public fun d(message: String, throwable: Throwable? = null) {
        log(Severity.Debug, message, throwable)
    }

    @InternalReadiumApi
    public fun i(message: String, throwable: Throwable? = null) {
        log(Severity.Info, message, throwable)
    }

    @InternalReadiumApi
    public fun w(message: String, throwable: Throwable? = null) {
        log(Severity.Warning, message, throwable)
    }

    @InternalReadiumApi
    public fun w(throwable: Throwable, message: String? = null) {
        log(Severity.Warning, message ?: throwable.toString(), throwable)
    }

    @InternalReadiumApi
    public fun e(message: String, throwable: Throwable? = null) {
        log(Severity.Error, message, throwable)
    }

    @InternalReadiumApi
    public fun e(throwable: Throwable, message: String? = null) {
        log(Severity.Error, message ?: throwable.toString(), throwable)
    }

    private fun log(severity: Severity, message: String, throwable: Throwable?) {
        logger?.log(severity, TAG, message, throwable)
    }
}

/**
 * Platform default [ReadiumLogger]: Logcat on Android, NSLog on iOS.
 */
internal expect fun defaultLogger(): ReadiumLogger
