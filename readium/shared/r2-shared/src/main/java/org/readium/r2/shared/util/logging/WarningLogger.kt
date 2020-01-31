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

/**
 * Interface to be implemented by third-party apps if they want to observe warnings raised, for
 * example, during the parsing of a [Publication].
 */
interface WarningLogger<in W : Warning> {

    fun log(warning: W)

}

/**
 * Implementation of a [WarningLogger] that accumulates the warnings in a list, to be used as a
 * convenience by third-party apps.
 */
class ListWarningLogger<W : Warning> : WarningLogger<W> {

    /**
     * The list of accumulated [Warning]s.
     */
    val warnings: List<W> get() = _warnings
    private val _warnings = mutableListOf<W>()

    override fun log(warning: W) {
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
     * User-facing message describing the warning.
     */
    val message: String

}

/**
 * Warning raised when parsing a model object from its JSON representation fails.
 *
 * @param modelClass Class of the model object to be parsed.
 * @param reason Details about the failure.
 * @param json Source [JSONObject].
 */
data class JsonWarning(
    val modelClass: Class<*>,
    val reason: String,
    val json: JSONObject? = null
) : Warning {
    override val message: String get() = "${javaClass.name} ${modelClass.name}: $reason"

}

/**
 * Raises a [JsonWarning].
 *
 * @param modelClass Class of the model object to be parsed.
 * @param reason Details about the failure.
 * @param json Source [JSONObject].
 */
fun WarningLogger<JsonWarning>.log(modelClass: Class<*>, reason: String, json: JSONObject? = null) {
    log(JsonWarning(modelClass, reason, json))
}
