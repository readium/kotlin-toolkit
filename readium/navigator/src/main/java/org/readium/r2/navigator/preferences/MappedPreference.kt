package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Creates a new [Preference] object wrapping the receiver and converting its value [from] and [to]
 * the target type [V].
 */
@ExperimentalReadiumApi
fun <T, V> Preference<T>.map(from: (T) -> V, to: (V) -> T): Preference<V> =
    MappedPreference(this, from, to)

/**
 * Creates a new [EnumPreference] object wrapping the receiver and converting its value and
 * [supportedValues], [from] and [to] the target type [V].
 */
@ExperimentalReadiumApi
fun <T, V> EnumPreference<T>.map(
    from: (T) -> V,
    to: (V) -> T,
    supportedValues: (List<T>) -> List<V> = { it.map(from) }
): EnumPreference<V> =
    MappedEnumPreference(this, from, to, supportedValues)

/**
 * Creates a new [EnumPreference] object wrapping the receiver with the provided [supportedValues].
 */
@ExperimentalReadiumApi
fun <T> EnumPreference<T>.withSupportedValues(supportedValues: List<T>): EnumPreference<T> =
    map(from = { it }, to = { it }, supportedValues = { supportedValues })

/**
 * Creates a new [EnumPreference] object wrapping the receiver and transforming its supported
 * values with [transform].
 */
@ExperimentalReadiumApi
fun <T> EnumPreference<T>.mapSupportedValues(transform: (List<T>) -> List<T>): EnumPreference<T> =
    map(from = { it }, to = { it }, supportedValues = transform)

/**
 * Creates a new [RangePreference] object wrapping the receiver and converting its value and
 * [supportedRange], [from] and [to] the target type [V].
 *
 * The value formatter, or [increment] and [decrement] strategy of the receiver can be overwritten.
 */
@ExperimentalReadiumApi
fun <T : Comparable<T>, V : Comparable<V>> RangePreference<T>.map(
    from: (T) -> V,
    to: (V) -> T,
    supportedRange: (ClosedRange<T>) -> ClosedRange<V> = { from(it.start)..from(it.endInclusive) },
    formatValue: ((V) -> String)? = null,
    increment: (() -> Unit)? = null,
    decrement: (() -> Unit)? = null,
): RangePreference<V> =
    MappedRangePreference(
        this, from, to,
        transformSupportedRange = supportedRange,
        valueFormatter = formatValue,
        incrementer = increment,
        decrementer = decrement
    )

/**
 * Creates a new [RangePreference] object wrapping the receiver and transforming its
 * [supportedRange], or overwriting its [formatValue] or [increment] and [decrement] strategy.
 */
@ExperimentalReadiumApi
fun <T : Comparable<T>> RangePreference<T>.map(
    supportedRange: (ClosedRange<T>) -> ClosedRange<T> = { it },
    formatValue: ((T) -> String)? = null,
    increment: (() -> Unit)? = null,
    decrement: (() -> Unit)? = null,
): RangePreference<T> =
    MappedRangePreference(
        this, { it }, { it },
        transformSupportedRange = supportedRange,
        valueFormatter = formatValue,
        incrementer = increment,
        decrementer = decrement
    )

/**
 * Creates a new [RangePreference] object wrapping the receiver and using a different supported
 * [range]. A new [progressionStrategy] can be provided to customize the implementation of increment
 * and decrement.
 */
@ExperimentalReadiumApi
fun <T : Comparable<T>> RangePreference<T>.withSupportedRange(range: ClosedRange<T>, progressionStrategy: ProgressionStrategy<T>? = null): RangePreference<T> =
    map(
        supportedRange = { range },
        increment = progressionStrategy?.run {{
            val currentValue = value ?: effectiveValue
            val newValue = increment(currentValue).coerceIn(range)
            set(newValue)
        }},
        decrement = progressionStrategy?.run {{
            val currentValue = value ?: effectiveValue
            val newValue = decrement(currentValue).coerceIn(range)
            set(newValue)
        }}
    )

@ExperimentalReadiumApi
private open class MappedPreference<T, V>(
    protected open val original: Preference<T>,
    protected val from: (T) -> V,
    protected val to: (V) -> T
) : Preference<V> {
    override val value: V?
        get() = original.value?.let(from)
    override val effectiveValue: V
        get() = original.effectiveValue.let(from)
    override val isEffective: Boolean
        get() = original.isEffective

    override fun set(value: V?) {
        original.set(value?.let(to))
    }
}

@ExperimentalReadiumApi
private class MappedEnumPreference<T, V>(
    override val original: EnumPreference<T>,
    from: (T) -> V,
    to: (V) -> T,
    private val transformSupportedValues: (List<T>) -> List<V>
) : MappedPreference<T, V>(original, from, to), EnumPreference<V> {

    override val supportedValues: List<V>
        get() = transformSupportedValues(original.supportedValues)
}

@ExperimentalReadiumApi
private class MappedRangePreference<T : Comparable<T>, V : Comparable<V>>(
    override val original: RangePreference<T>,
    from: (T) -> V,
    to: (V) -> T,
    private val transformSupportedRange: (ClosedRange<T>) -> ClosedRange<V>,
    private val valueFormatter: ((V) -> String)?,
    private val incrementer: (() -> Unit)?,
    private val decrementer: (() -> Unit)?
) : MappedPreference<T, V>(original, from, to), RangePreference<V> {

    override val supportedRange: ClosedRange<V>
        get() = transformSupportedRange(original.supportedRange)

    override fun increment() {
        incrementer?.invoke() ?: original.increment()
    }

    override fun decrement() {
        decrementer?.invoke() ?: original.decrement()
    }

    override fun formatValue(value: V): String =
        valueFormatter?.invoke(value) ?: original.formatValue(to(value))
}
