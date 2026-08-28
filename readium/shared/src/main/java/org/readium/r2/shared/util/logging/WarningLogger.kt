/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.logging

import org.json.JSONObject
import org.readium.r2.shared.util.logging.Warning.SeverityLevel.*
import timber.log.Timber

/**
 * Interface to be implemented by third-party apps if they want to observe warnings raised, for
 * example, during the parsing of a publication.
 */
public fun interface WarningLogger {

    /** Notifies that a warning occurred. */
    public fun log(warning: Warning)
}

/**
 * Implementation of a [WarningLogger] that accumulates the warnings in a list, to be used as a
 * convenience by third-party apps.
 */
public class ListWarningLogger : WarningLogger {

    /**
     * The list of accumulated [Warning]s.
     */
    public val warnings: List<Warning> get() = _warnings
    private val _warnings = mutableListOf<Warning>()

    override fun log(warning: Warning) {
        _warnings.add(warning)
    }
}

/**
 * Implementation of a [WarningLogger] printing the warnings to the console.
 */
public class ConsoleWarningLogger : WarningLogger {

    override fun log(warning: Warning) {
        val message = "[${warning.tag}] ${warning.message}"
        when (warning.severity) {
            MINOR, MODERATE -> Timber.w(message)
            MAJOR -> Timber.e(message)
        }
    }
}

/**
 * Represents a non-fatal warning message that can be raised by a Readium library.
 *
 * For example, while parsing an EPUB we, might want to report issues in the publication without
 * failing the whole parsing.
 */
public interface Warning {

    /**
     * Indicates how the user experience might be affected by a warning.
     *
     * @property MINOR The user probably won't notice the issue.
     * @property MODERATE The user experience might be affected, but it shouldn't prevent the user from enjoying the publication.
     * @property MAJOR The user experience will most likely be disturbed, for example with rendering issues.
     */
    public enum class SeverityLevel {
        MINOR,
        MODERATE,
        MAJOR,
    }

    /**
     * Tag used to group similar warnings together.
     *
     * For example json, metadata, etc.
     */
    public val tag: String

    /**
     * Localized user-facing message describing the issue.
     */
    public val message: String

    /**
     * Indicates the severity level of this warning.
     */
    public val severity: SeverityLevel
}

/**
 * Warning raised when parsing a model object from its JSON representation fails.
 *
 * @param modelClass Class of the model object to be parsed.
 * @param reason Details about the failure.
 * @param json Source [JSONObject].
 */
public data class JsonWarning(
    val modelClass: Class<*>,
    val reason: String,
    override val severity: Warning.SeverityLevel,
    val json: JSONObject? = null,
) : Warning {

    override val tag: String = "json"

    override val message: String get() = "${javaClass.simpleName} ${modelClass.name}: $reason"
}

/**
 * Raises a [JsonWarning].
 *
 * @param modelClass Class of the model object to be parsed.
 * @param reason Details about the failure.
 * @param severity The severity level of this warning.
 * @param json Source [JSONObject].
 */
public fun WarningLogger.log(
    modelClass: Class<*>,
    reason: String,
    json: JSONObject? = null,
    severity: Warning.SeverityLevel = Warning.SeverityLevel.MAJOR,
) {
    log(JsonWarning(modelClass, reason, severity, json))
}
