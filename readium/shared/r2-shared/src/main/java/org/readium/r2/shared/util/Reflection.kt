/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util

import kotlin.reflect.KProperty0

val KProperty0<*>.isLazyInitialized: Boolean
    get() = (getDelegate() as Lazy<*>).isInitialized()

