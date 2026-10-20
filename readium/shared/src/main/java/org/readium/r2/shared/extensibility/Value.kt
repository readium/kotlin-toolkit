/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class, ExperimentalReadiumApi::class, ExperimentalSerializationApi::class)

package org.readium.r2.shared.extensibility

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
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
@Serializable(with = NullValueSerializer::class)
public data object NullValue : Value

@ExperimentalReadiumApi
@Parcelize
@Serializable(with = BooleanValueSerializer::class)
public data class BooleanValue(val value: Boolean) : Value

@ExperimentalReadiumApi
@Parcelize
@Serializable(with = IntValueSerializer::class)
public data class IntValue(val value: Long) : Value {
    public constructor(value: Int) : this(value.toLong())

    public fun toInt(): Int = value.toInt()
}

@ExperimentalReadiumApi
@Parcelize
@Serializable(with = DoubleValueSerializer::class)
public data class DoubleValue(val value: Double) : Value {
    public constructor(value: Float) : this(value.toDouble())
}

@ExperimentalReadiumApi
@Parcelize
@Serializable(with = StringValueSerializer::class)
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

internal object ValueSerializer : JsonContentPolymorphicSerializer<Value>(Value::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Value> =
        when (element) {
            is JsonNull -> NullValue.serializer()
            is JsonPrimitive -> when {
                element.isString -> StringValue.serializer()
                element.booleanOrNull != null -> BooleanValue.serializer()
                element.longOrNull != null -> IntValue.serializer()
                element.doubleOrNull != null -> DoubleValue.serializer()
                else -> StringValue.serializer()
            }
            is JsonArray -> ListValue.serializer()
            is JsonObject -> MapValue.serializer()
        }
}

internal object NullValueSerializer : KSerializer<NullValue> {
    override val descriptor: SerialDescriptor = JsonNull.serializer().descriptor

    override fun deserialize(decoder: Decoder): NullValue {
        if (decoder.decodeNotNullMark()) {
            throw SerializationException("Expected null for NullValue")
        }
        decoder.decodeNull()
        return NullValue
    }

    override fun serialize(encoder: Encoder, value: NullValue) {
        encoder.encodeNull()
    }
}

internal object BooleanValueSerializer : KSerializer<BooleanValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BooleanValue", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): BooleanValue =
        BooleanValue(decoder.decodeBoolean())

    override fun serialize(encoder: Encoder, value: BooleanValue) {
        encoder.encodeBoolean(value.value)
    }
}

internal object IntValueSerializer : KSerializer<IntValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntValue", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): IntValue =
        IntValue(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: IntValue) {
        encoder.encodeLong(value.value)
    }
}

internal object DoubleValueSerializer : KSerializer<DoubleValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DoubleValue", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): DoubleValue =
        DoubleValue(decoder.decodeDouble())

    override fun serialize(encoder: Encoder, value: DoubleValue) {
        encoder.encodeDouble(value.value)
    }
}

internal object StringValueSerializer : KSerializer<StringValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringValue", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): StringValue =
        StringValue(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: StringValue) {
        encoder.encodeString(value.value)
    }
}

internal object MapValueSerializer : KSerializer<MapValue> {
    private val delegate = MapSerializer(String.serializer(), ValueSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): MapValue =
        MapValue(decoder.decodeSerializableValue(delegate))

    override fun serialize(encoder: Encoder, value: MapValue) {
        encoder.encodeSerializableValue(delegate, value.value)
    }
}

internal object ListValueSerializer : KSerializer<ListValue> {
    private val delegate = ListSerializer(ValueSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): ListValue =
        ListValue(decoder.decodeSerializableValue(delegate))

    override fun serialize(encoder: Encoder, value: ListValue) {
        encoder.encodeSerializableValue(delegate, value.value)
    }
}
