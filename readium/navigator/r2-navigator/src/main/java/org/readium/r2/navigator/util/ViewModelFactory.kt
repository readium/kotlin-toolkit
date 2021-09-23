/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Creates a [ViewModelProvider.Factory] for a single type of [ViewModel] using the result of the
 * given [factory] closure.
 */
internal inline fun <reified T : ViewModel> createViewModelFactory(crossinline factory: () -> T): ViewModelProvider.Factory =

    object : ViewModelProvider.Factory {
        override fun <V : ViewModel?> create(modelClass: Class<V>): V {
            if (!modelClass.isAssignableFrom(T::class.java)) {
                throw IllegalAccessException("Unknown ViewModel class")
            }
            @Suppress("UNCHECKED_CAST")
            return factory() as V
        }
    }
