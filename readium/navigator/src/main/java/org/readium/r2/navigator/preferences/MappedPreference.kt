package org.readium.r2.navigator.preferences

/**
 * Creates a new [Preference] object wrapping the receiver and converting its value [from] and [to]
 * the target type [V].
 */
public fun <T, V> Preference<T>.map(from: (T) -> V, to: (V) -> T): Preference<V> =
    MappedPreference(this, from, to)

/**
 * Creates a new [EnumPreference] object wrapping the receiver with the provided [supportedValues].
 */
public fun <T> Preference<T>.withSupportedValues(vararg supportedValues: T): EnumPreference<T> =
    withSupportedValues(supportedValues.toList())

/**
 * Creates a new [EnumPreference] object wrapping the receiver with the provided [supportedValues].
 */
public fun <T> Preference<T>.withSupportedValues(supportedValues: List<T>): EnumPreference<T> =
    PreferenceWithSupportedValues(this, supportedValues)

/**
 * Creates a new [EnumPreference] object wrapping the receiver and converting its value and
 * [supportedValues], [from] and [to] the target type [V].
 */
public fun <T, V> EnumPreference<T>.map(
    from: (T) -> V,
    to: (V) -> T,
    supportedValues: (List<T>) -> List<V> = { it.map(from) },
): EnumPreference<V> =
    MappedEnumPreference(this, from, to, supportedValues)

/**
 * Creates a new [EnumPreference] object wrapping the receiver with the provided [supportedValues].
 */
public fun <T> EnumPreference<T>.withSupportedValues(vararg supportedValues: T): EnumPreference<T> =
    withSupportedValues(supportedValues.toList())

/**
 * Creates a new [EnumPreference] object wrapping the receiver with the provided [supportedValues].
 */
public fun <T> EnumPreference<T>.withSupportedValues(supportedValues: List<T>): EnumPreference<T> =
    map(from = { it }, to = { it }, supportedValues = { supportedValues })

/**
 * Creates a new [EnumPreference] object wrapping the receiver and transforming its supported
 * values with [transform].
 */
public fun <T> EnumPreference<T>.mapSupportedValues(transform: (List<T>) -> List<T>): EnumPreference<T> =
    map(from = { it }, to = { it }, supportedValues = transform)

/**
 * Creates a new [RangePreference] object wrapping the receiver and converting its value and
 * [supportedRange], [from] and [to] the target type [V].
 *
 * The value formatter, or [increment] and [decrement] strategy of the receiver can be overwritten.
 */
public fun <T : Comparable<T>, V : Comparable<V>> RangePreference<T>.map(
    from: (T) -> V,
    to: (V) -> T,
    supportedRange: (ClosedRange<T>) -> ClosedRange<V> = { from(it.start)..from(it.endInclusive) },
    formatValue: ((V) -> String)? = null,
    increment: (RangePreference<V>.() -> Unit)? = null,
    decrement: (RangePreference<V>.() -> Unit)? = null,
): RangePreference<V> =
    MappedRangePreference(
        this,
        from,
        to,
        transformSupportedRange = supportedRange,
        valueFormatter = formatValue,
        incrementer = increment,
        decrementer = decrement
    )

/**
 * Creates a new [RangePreference] object wrapping the receiver and transforming its
 * [supportedRange], or overwriting its [formatValue] or [increment] and [decrement] strategy.
 */
public fun <T : Comparable<T>> RangePreference<T>.map(
    supportedRange: (ClosedRange<T>) -> ClosedRange<T> = { it },
    formatValue: ((T) -> String)? = null,
    increment: (RangePreference<T>.() -> Unit)? = null,
    decrement: (RangePreference<T>.() -> Unit)? = null,
): RangePreference<T> =
    MappedRangePreference(
        this,
        { it },
        { it },
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
public fun <T : Comparable<T>> RangePreference<T>.withSupportedRange(
    range: ClosedRange<T> = supportedRange,
    progressionStrategy: ProgressionStrategy<T>,
): RangePreference<T> =
    map(
        supportedRange = { range },
        increment = {
            val currentValue = value ?: effectiveValue
            this.set(progressionStrategy.increment(currentValue))
        },
        decrement = {
            val currentValue = value ?: effectiveValue
            set(progressionStrategy.decrement(currentValue))
        }
    )

private open class MappedPreference<T, V>(
    protected open val original: Preference<T>,
    protected val from: (T) -> V,
    protected val to: (V) -> T,
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

private class PreferenceWithSupportedValues<T>(
    override val original: Preference<T>,
    override val supportedValues: List<T>,
) : MappedPreference<T, T>(original, from = { it }, to = { it }), EnumPreference<T>

private class MappedEnumPreference<T, V>(
    override val original: EnumPreference<T>,
    from: (T) -> V,
    to: (V) -> T,
    private val transformSupportedValues: (List<T>) -> List<V>,
) : MappedPreference<T, V>(original, from, to), EnumPreference<V> {

    override val supportedValues: List<V>
        get() = transformSupportedValues(original.supportedValues)

    override fun set(value: V?) {
        require(value == null || value in supportedValues)
        super.set(value)
    }
}

private class MappedRangePreference<T : Comparable<T>, V : Comparable<V>>(
    override val original: RangePreference<T>,
    from: (T) -> V,
    to: (V) -> T,
    private val transformSupportedRange: (ClosedRange<T>) -> ClosedRange<V>,
    private val valueFormatter: ((V) -> String)?,
    private val incrementer: (RangePreference<V>.() -> Unit)?,
    private val decrementer: (RangePreference<V>.() -> Unit)?,
) : MappedPreference<T, V>(original, from, to), RangePreference<V> {

    override val supportedRange: ClosedRange<V>
        get() = transformSupportedRange(original.supportedRange)

    override fun set(value: V?) {
        super.set(value?.coerceIn(supportedRange))
    }

    override fun increment() {
        incrementer?.invoke(this) ?: original.increment()
    }

    override fun decrement() {
        decrementer?.invoke(this) ?: original.decrement()
    }

    override fun formatValue(value: V): String =
        valueFormatter?.invoke(value) ?: original.formatValue(to(value))
}
