/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.readium.r2.shared.InternalReadiumApi

/**
 * Finds the first cause instance of the given type.
 */
@InternalReadiumApi
public fun <R : Throwable> Throwable.findInstance(klass: Class<R>): R? =
    @Suppress("UNCHECKED_CAST")
    when {
        klass.isInstance(this) -> this as R
        else -> cause?.findInstance(klass)
    }
