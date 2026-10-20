/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class, ExperimentalReadiumApi::class)

package org.readium.r2.shared.extensibility

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi

/**
 * A type-safe representation of an extensible value (such as a dynamic JSON value).
 */
@ExperimentalReadiumApi
@Serializable(with = ValueSerializer::class)
public sealed interface Value : Parcelable {
    public val boolean: Boolean? get() = (this as? BooleanValue)?.value
    public val string: String? get() = (this as? StringValue)?.value
    public val integer: Long? get() = (this as? IntValue)?.value
    public val int: Int? get() = (this as? IntValue)?.toInt()
    public val double: Double?
        get() = when (this) {
            is DoubleValue -> value
            is IntValue -> value.toDouble()
            else -> null
        }
    public val list: List<Value>? get() = (this as? ListValue)?.value
    public val map: Map<String, Value>? get() = (this as? MapValue)?.value
    public val isNull: Boolean get() = this is NullValue
}

@ExperimentalReadiumApi
public operator fun Value.get(key: String): Value? = (this as? MapValue)?.value?.get(key)

@ExperimentalReadiumApi
public operator fun Value.get(index: Int): Value? = (this as? ListValue)?.value?.getOrNull(index)

@ExperimentalReadiumApi
@Parcelize
public data object NullValue : Value

@ExperimentalReadiumApi
@Parcelize
public data class BooleanValue(val value: Boolean) : Value

@ExperimentalReadiumApi
@Parcelize
public data class IntValue(val value: Long) : Value {
    public constructor(value: Int) : this(value.toLong())

    public fun toInt(): Int = value.toInt()
}

@ExperimentalReadiumApi
@Parcelize
public data class DoubleValue(val value: Double) : Value {
    public constructor(value: Float) : this(value.toDouble())
}

@ExperimentalReadiumApi
@Parcelize
public data class StringValue(val value: String) : Value

@ExperimentalReadiumApi
@Parcelize
@Serializable(with = ListValueSerializer::class)
public data class ListValue(val value: List<Value> = emptyList()) : Value {
    public constructor(vararg elements: Value) : this(elements.toList())
}

@ExperimentalReadiumApi
@Parcelize
@Serializable(with = MapValueSerializer::class)
public data class MapValue(val value: Map<String, Value> = emptyMap()) : Value {
    public constructor(vararg entries: Pair<String, Value>) : this(entries.toMap())
}

// JSON conversions

@InternalReadiumApi
public fun Value.toJsonElement(): JsonElement = when (this) {
    is NullValue -> JsonNull
    is BooleanValue -> JsonPrimitive(value)
    is IntValue -> JsonPrimitive(value)
    is DoubleValue -> JsonPrimitive(value)
    is StringValue -> JsonPrimitive(value)
    is ListValue -> toJsonArray()
    is MapValue -> toJsonObject()
}

@InternalReadiumApi
public fun ListValue.toJsonArray(): JsonArray = JsonArray(value.map { it.toJsonElement() })

@InternalReadiumApi
public fun MapValue.toJsonObject(): JsonObject =
    JsonObject(value.mapValues { (_, value) -> value.toJsonElement() })

@InternalReadiumApi
public fun JsonElement.toValue(): Value = when (this) {
    is JsonNull -> NullValue
    is JsonPrimitive -> {
        if (isString) {
            StringValue(content)
        } else {
            booleanOrNull?.let { BooleanValue(it) }
                ?: longOrNull?.let { IntValue(it) }
                ?: doubleOrNull?.let { DoubleValue(it) }
                ?: StringValue(content)
        }
    }
    is JsonArray -> toListValue()
    is JsonObject -> toMapValue()
}

@InternalReadiumApi
public fun JsonArray.toListValue(): ListValue = ListValue(map { it.toValue() })

@InternalReadiumApi
public fun JsonObject.toMapValue(): MapValue = MapValue(mapValues { (_, element) -> element.toValue() })

// Serializers (internal)

internal object ValueSerializer : KSerializer<Value> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Value {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Value can only be deserialized with Json")
        return jsonDecoder.decodeJsonElement().toValue()
    }

    override fun serialize(encoder: Encoder, value: Value) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Value can only be serialized with Json")
        jsonEncoder.encodeJsonElement(value.toJsonElement())
    }
}

internal object MapValueSerializer : KSerializer<MapValue> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): MapValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("MapValue can only be deserialized with Json")
        val jsonObject = jsonDecoder.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("Expected JsonObject for MapValue")
        return jsonObject.toMapValue()
    }

    override fun serialize(encoder: Encoder, value: MapValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("MapValue can only be serialized with Json")
        jsonEncoder.encodeJsonElement(value.toJsonObject())
    }
}

internal object ListValueSerializer : KSerializer<ListValue> {
    override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

    override fun deserialize(decoder: Decoder): ListValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ListValue can only be deserialized with Json")
        val jsonArray = jsonDecoder.decodeJsonElement() as? JsonArray
            ?: throw SerializationException("Expected JsonArray for ListValue")
        return jsonArray.toListValue()
    }

    override fun serialize(encoder: Encoder, value: ListValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ListValue can only be serialized with Json")
        jsonEncoder.encodeJsonElement(value.toJsonArray())
    }
}
