/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.readium.r2.shared.extensions.tryOrLog

/**
 * JSON serializer for a [Setting].
 */
interface SettingCoder<V> {
    fun decode(json: JsonElement): V?
    fun encode(value: V): JsonElement
}

/**
 * A [SettingCoder] using the default Kotlin Serializer associated with the type [V].
 */
class SerializerSettingCoder<V>(private val serializer: KSerializer<V>) : SettingCoder<V> {
    companion object {
        inline operator fun <reified V> invoke(): SerializerSettingCoder<V> =
            SerializerSettingCoder(serializer())
    }

    override fun decode(json: JsonElement): V? =
        tryOrLog { Json.decodeFromJsonElement(serializer, json) }

    override fun encode(value: V): JsonElement =
        Json.encodeToJsonElement(serializer, value)
}
