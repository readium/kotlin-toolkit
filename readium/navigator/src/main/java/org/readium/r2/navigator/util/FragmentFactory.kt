/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull

/**
 * A [FragmentFactory] which will instantiate a single type of [Fragment] using the result of the
 * given [factory] closure.
 */
public class SingleFragmentFactory<T : Fragment>(
    public val fragmentClass: Class<T>,
    private val factory: () -> T,
) : FragmentFactory() {

    public operator fun invoke(): T =
        factory()

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            fragmentClass.name -> factory()
            else -> super.instantiate(classLoader, className)
        }
    }
}

/**
 * Creates a [FragmentFactory] for a single type of [Fragment] using the result of the given
 * [factory] closure.
 */
@InternalReadiumApi
public inline fun <reified T : Fragment> createFragmentFactory(noinline factory: () -> T): SingleFragmentFactory<T> =
    SingleFragmentFactory(T::class.java, factory)

/**
 * A [FragmentFactory] which will iterate over a provided list of [factories] until finding one
 * instantiating successfully the requested [Fragment].
 *
 * ```
 * supportFragmentManager.fragmentFactory = CompositeFragmentFactory(
 *     EpubNavigatorFragment.createFactory(publication, baseUrl, initialLocator, this),
 *     PdfNavigatorFragment.createFactory(publication, initialLocator, this)
 * )
 * ```
 */
@InternalReadiumApi
public class CompositeFragmentFactory(private val factories: List<FragmentFactory>) : FragmentFactory() {

    public constructor(vararg factories: FragmentFactory) : this(factories.toList())

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        for (factory in factories) {
            tryOrNull { factory.instantiate(classLoader, className) }
                ?.let { return it }
        }

        return super.instantiate(classLoader, className)
    }
}
