/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

/**
 * Smart pointer holding a mutable reference to an object.
 *
 * Get the reference by calling `ref()`
 * Conveniently, the reference can be reset by setting the `ref` property.
 */
class Ref<T>(var ref: T? = null) {

    operator fun invoke(): T? = ref
}
