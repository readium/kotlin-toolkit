/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import org.json.JSONObject

/**
 * Interface to be implemented by third-party apps if they want to observe raised warnings.
 */
interface WarningLogger {

    fun log(warning: Warning, level: Warning.Level = Warning.Level.WARNING)

}

/**
 * Represents a non-fatal warning message that can be raised by a Readium library.
 *
 * For example, while parsing an EPUB we might want to report issues in the publication without
 * failing the whole parsing.
 */
sealed class Warning {

    /**
     * Qualify the severity of a [Warning].
     */
    enum class Level {
        INFO, WARNING, SEVERE
    }

    /**
     * Message describing the warning.
     * To be overriden in subclasses.
     */
    abstract val message: String

    /**
     * Warning raised when parsing a model object from its JSON representation fails.
     *
     * @param type Class of the model object to be parsed.
     * @param reason Details about the failure.
     * @param json Source [JSONObject].
     */
    data class JsonParsing(
        val type: Class<*>,
        val reason: String,
        val json: JSONObject? = null
    ) : Warning() {
        override val message: String get() = "${javaClass.name} ${type.name}: $reason"
    }

    /**
     * Warning raised when parsing a model object from an EPUB package.
     *
     * @param type Class of the model object to be parsed.
     * @param reason Details about the failure.
     * @param source Data used for the parsing (eg. XML).
     */
    data class EpubParsing(
        val type: Class<*>,
        val reason: String,
        val source: String? = null
    ) : Warning() {
        override val message: String get() = "${javaClass.name} ${type.name}: $reason"
    }

}
