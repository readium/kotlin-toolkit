package org.readium.r2.navigator.settings

import org.readium.r2.navigator.R
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Try
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

@ExperimentalReadiumApi
interface Modifier<V> {
    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value
     * and each modifier from outside in.
     */
    fun <R> fold(initial: R, operation: (R, Modifier<V>) -> R): R = operation(initial, this)

    fun <M : Modifier<V>, R> foldAs(
        klass: KClass<*>,
        initial: R,
        operation: (R, M) -> R
    ): R =
        klass.safeCast(this)
            ?.let { operation(initial, it) }
            ?: initial

    companion object {
        operator fun <V> invoke(): Modifier<V> = object : Modifier<V> {}
    }
}

@ExperimentalReadiumApi
interface ModifierHolder<V> {
    val modifier: Modifier<V>
}

@ExperimentalReadiumApi
data class CombinedModifier<V>(
    private val outer: Modifier<V>,
    private val inner: Modifier<V>,
) : Modifier<V> {

    override fun <R> fold(initial: R, operation: (R, Modifier<V>) -> R): R =
        inner.fold(outer.fold(initial, operation), operation)

    override fun <M : Modifier<V>, R> foldAs(
        cast: (M)
        klass: KClass<*>,
        initial: R,
        operation: (R, M) -> R
    ): R =
        inner.foldInstanceOf(klass, outer.foldInstanceOf(klass, initial, operation), operation)
}

@ExperimentalReadiumApi
infix fun <V> Modifier<V>.then(other: Modifier<V>): Modifier<V> =
    CombinedModifier(this, other)

//
//    override fun validate(value: V): Try<Unit, UserException> =
//        inner.validate(value).flatMap { outer.validate(value) }
//
//    override fun isActiveForValues(values: PresentationValues): Boolean =
//        inner.isActiveForValues(values) && outer.isActiveForValues(values)
//
//    override fun activateInValues(values: PresentationValues): Try<PresentationValues, UserException> =
//        inner.activateInValues(values)
//            .flatMap { outer.activateInValues(it) }
//
//    override fun <R> fold(initial: R, operation: (R, PresentationConstraints<V>) -> R): R =
//        inner.fold(outer.fold(initial, operation), operation)
//)

@ExperimentalReadiumApi
data class RangeModifier<V : Comparable<V>>(
    val start: V?,
    val endInclusive: V?
) : Modifier<V>

@ExperimentalReadiumApi
val <V : Comparable<V>> ModifierHolder<V>.start: V? get() =
    modifier.fold<V?>(null) { acc, mod ->
        val start = (mod as? RangeModifier<V>)?.start ?: return@fold acc
        val acc = acc ?: return@fold start
        maxOf(start, acc)
    }

@ExperimentalReadiumApi
val <V : Comparable<V>> ModifierHolder<V>.end: V? get() =
    modifier.foldInstanceOf<V?>(RangeModifier::class, null) { acc, mod ->
        val start = (mod as? RangeModifier<V>)?.start ?: return@fold acc
        val acc = acc ?: return@fold start
        maxOf(start, acc)
    }

@ExperimentalPresentation
val PresentationConstraints<PresentationRange>.stepCount: Int? get() =
    fold<Int?>(null) { acc, constraints ->
        (constraints as? PresentationRangeConstraints)?.stepCount ?: acc
    }

@ExperimentalPresentation
val PresentationConstraints<PresentationRange>.step: Double?
    get() = stepCount?.takeIf { it > 0 }?.let { 1.0 / it }

@ExperimentalPresentation
class PresentationEnumConstraints<E : Enum<E>>(
    val supportedValues: List<E>? = null
) : PresentationConstraints<E> {

    override fun validate(value: E): Try<Unit, UserException> {
        if (supportedValues?.contains(value) == false) {
            return Try.failure(UserException(R.string.readium_presentation_valueNotSupportedError))
        }
        return super.validate(value)
    }
}

@ExperimentalPresentation
val <E: Enum<E>> PresentationConstraints<E>.supportedValues: List<E>? get() =
    fold<List<E>?>(null) { acc, constraints ->
        (constraints as? PresentationEnumConstraints)?.supportedValues ?: acc
    }


@ExperimentalPresentation
class PresentationDependencyConstraints<V>(
    private val requiredValues: Preferences,
) : PresentationConstraints<V> {

    override fun isActiveForValues(values: Preferences): Boolean {
        val requiredVals = requiredValues.values.filterValues { it != null }
        for ((key, value) in requiredVals) {
            if (value != values.values[key]) {
                return false
            }
        }
        return true
    }

    override fun activateInValues(values: Preferences): Try<Preferences, UserException> =
        Try.success(values.merge(requiredValues))
}

@ExperimentalPresentation
fun <V> PresentationConstraints<V>.require(values: Preferences): PresentationConstraints<V> =
    this + PresentationDependencyConstraints(values)
