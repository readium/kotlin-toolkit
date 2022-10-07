/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Repository for values automatically set to null every time the [Fragment]'s view is destroyed.
 *
 * This is especially useful for binding properties to avoid memory leaks when Fragments are on
 * the back stack without any view attached.
 */
class LifecycleDelegates(private val fragment: Fragment):  DefaultLifecycleObserver {

    /**
     * This delegate is very similar to [Delegates.notNull].
     * We still need it to be able to reset the value to null.
     */
    private class ViewLifecycleAwareVar<T : Any> : ReadWriteProperty<Fragment, T> {
        var nullableValue: T? = null

        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            return nullableValue
                ?: throw IllegalStateException("Lifecycle-aware value not available at the moment.")
        }

        override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
            nullableValue = value
        }
    }

    private val viewLifecycleValues: MutableList<ViewLifecycleAwareVar<*>> =
        mutableListOf()

    override fun onCreate(owner: LifecycleOwner) {
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
            viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    onViewDestroy()
                }
            })
        }
    }

    private fun onViewDestroy() {
        viewLifecycleValues.forEach { delegate ->
            delegate.nullableValue = null
        }
    }

    /**
     * Make a value that will be automatically set to null every time the [Fragment]'s view is destroyed.
     */
    fun <T: Any> viewLifecycleAware():  ReadWriteProperty<Fragment, T> {
        val delegate = ViewLifecycleAwareVar<T>()
        viewLifecycleValues.add(delegate as ViewLifecycleAwareVar<*>)
        return delegate
    }
}

/**
 * Make a single value automatically set to null every time the [Fragment]'s view is destroyed.
 */
fun <T: Any> Fragment.viewLifecycle() =
    LifecycleDelegates(this).viewLifecycleAware<T>()