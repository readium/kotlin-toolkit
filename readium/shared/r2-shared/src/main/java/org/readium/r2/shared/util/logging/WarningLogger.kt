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


// FIXME: Mark this interface as functional to benefit from the SAM-conversion in Kotlin 1.4 https://blog.jetbrains.com/kotlin/2020/03/kotlin-1-4-m1-released/#new-type-inference
/**
 * Interface to be implemented by third-party apps if they want to observe warnings raised, for
 * example, during the parsing of a [Publication].
 */
interface WarningLogger {

    /** Notifies that a warning occurred. */
    fun log(warning: Warning)

}

/**
 * Implementation of a [WarningLogger] that accumulates the warnings in a list, to be used as a
 * convenience by third-party apps.
 */
internal class ListWarningLogger : WarningLogger {

    /**
     * The list of accumulated [Warning]s.
     */
    val warnings: List<Warning> get() = _warnings
    private val _warnings = mutableListOf<Warning>()

    override fun log(warning: Warning) {
        _warnings.add(warning)
    }

}

/**
 * Represents a non-fatal warning message that can be raised by a Readium library.
 *
 * For example, while parsing an EPUB we, might want to report issues in the publication without
 * failing the whole parsing.
 */
interface Warning {

    /**
     * Indicates how the user experience might be affected by a warning.
     *
     * @property MINOR The user probably won't notice the issue.
     * @property MODERATE The user experience might be affected, but it shouldn't prevent the user from enjoying the publication.
     * @property MAJOR The user experience will most likely be disturbed, for example with rendering issues.
     */
    enum class SeverityLevel {
        MINOR,
        MODERATE,
        MAJOR
    }

    /**
     * Tag used to group similar warnings together.
     *
     * For example json, metadata, etc.
     */
    val tag: String

    /**
     * Localized user-facing message describing the issue.
     */
    val message: String

    /**
     * Indicates the severity level of this warning.
     */
    val severity: SeverityLevel

}

/**
 * Warning raised when parsing a model object from its JSON representation fails.
 *
 * @param modelClass Class of the model object to be parsed.
 * @param reason Details about the failure.
 * @param json Source [JSONObject].
 */
internal data class JsonWarning(
    val modelClass: Class<*>,
    val reason: String,
    override val severity: Warning.SeverityLevel,
    val json: JSONObject? = null
) : Warning {

    override val tag: String = "json"

    override val message: String get() = "${javaClass.name} ${modelClass.name}: $reason"
}

/**
 * Raises a [JsonWarning].
 *
 * @param modelClass Class of the model object to be parsed.
 * @param reason Details about the failure.
 * @param severity The severity level of this warning.
 * @param json Source [JSONObject].
 */
internal fun WarningLogger.log(
    modelClass: Class<*>, reason: String,
    json: JSONObject? = null,
    severity: Warning.SeverityLevel = Warning.SeverityLevel.MAJOR
) {
    log(JsonWarning(modelClass, reason, severity, json))
}
