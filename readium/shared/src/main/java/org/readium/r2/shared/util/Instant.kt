@file:OptIn(InternalReadiumApi::class, kotlin.time.ExperimentalTime::class)
@file:Suppress("DEPRECATION")

package org.readium.r2.shared.util

import android.os.Parcel
import android.os.Parcelable
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.toInstant
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull

private typealias KotlinInstant = kotlin.time.Instant

/**
 * A moment in time.
 */
@Deprecated(
    message = "Use kotlin.time.Instant instead",
    replaceWith = ReplaceWith("Instant", imports = ["kotlin.time.Instant"])
)
@Parcelize
@TypeParceler<KotlinInstant, InstantParceler>()
@Serializable(with = InstantSerializer::class)
public class Instant private constructor(
    private val value: KotlinInstant,
) : Parcelable, Comparable<Instant> {
    public companion object {
        /**
         * Parses an [Instant] from its ISO-8601 representation.
         *
         * Returns null if it can't be parsed.
         */
        @Deprecated(
            message = "Migrate to kotlin.time.Instant and use kotlin.time.Instant.parse() for strings with a UTC offset, or kotlinx.datetime for date-only and offset-less strings."
        )
        public fun parse(input: String): Instant? {
            val instant = tryOrNull { KotlinInstant.parse(input) }
                ?: tryOrNull { LocalDateTime.parse(input).toInstant(TimeZone.UTC) }
                ?: tryOrNull { LocalDate.parse(input).atStartOfDayIn(TimeZone.UTC) }

            return instant?.let { Instant(it) }
        }

        @Deprecated(
            message = "Use kotlin.time.Instant directly instead of wrapping/unwrapping"
        )
        public fun fromKotlinInstant(value: KotlinInstant): Instant = Instant(value)

        @Deprecated(
            message = "Use Instant.fromEpochMilliseconds(date.time)",
            replaceWith = ReplaceWith(
                "kotlin.time.Instant.fromEpochMilliseconds(date.time)",
                "kotlin.time.Instant"
            )
        )
        public fun fromJavaDate(date: java.util.Date): Instant =
            Instant(KotlinInstant.fromEpochMilliseconds(date.time))

        @Deprecated(
            message = "Use Instant.fromEpochMilliseconds()",
            replaceWith = ReplaceWith(
                "kotlin.time.Instant.fromEpochMilliseconds(milliseconds)",
                "kotlin.time.Instant"
            )
        )
        public fun fromEpochMilliseconds(milliseconds: Long): Instant =
            Instant(KotlinInstant.fromEpochMilliseconds(milliseconds))

        /**
         * Returns an [Instant] representing the current moment in time.
         */
        @Deprecated(
            message = "Use Clock.System.now()",
            replaceWith = ReplaceWith("Clock.System.now()", "kotlin.time.Clock")
        )
        public fun now(): Instant = Instant(Clock.System.now())
    }

    @Deprecated(
        message = "Use kotlin.time.Instant directly instead of wrapping/unwrapping"
    )
    public fun toKotlinInstant(): KotlinInstant = value

    @Deprecated(
        message = "Convert using epoch milliseconds",
        replaceWith = ReplaceWith(
            "java.util.Date(this.toEpochMilliseconds())",
            "java.util.Date"
        )
    )
    public fun toJavaDate(): java.util.Date = java.util.Date(value.toEpochMilliseconds())

    @Deprecated(
        message = "Migrate to kotlin.time.Instant and call toEpochMilliseconds() on that directly"
    )
    public fun toEpochMilliseconds(): Long = value.toEpochMilliseconds()

    /**
     * Returns the ISO-8601 representation of the instant.
     */
    override fun toString(): String =
        value.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun compareTo(other: Instant): Int = value.compareTo(other.value)
}

/**
 * A serializer for [Instant] that uses the ISO-8601 representation.
 *
 * JSON example: `"2020-12-09T09:16:56.000124Z"`
 */
public object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("org.readium.Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString())
            ?: throw IllegalArgumentException("Instant cannot be deserialized")

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}

@InternalReadiumApi
public object InstantParceler : Parceler<KotlinInstant> {

    override fun create(parcel: Parcel): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(parcel.readLong())

    override fun kotlin.time.Instant.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(toEpochMilliseconds())
    }
}
