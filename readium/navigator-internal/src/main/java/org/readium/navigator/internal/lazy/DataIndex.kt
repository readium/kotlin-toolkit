/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.readium.navigator.internal.lazy

/**
 * Represents an index in the list of items of lazy list.
 */
@Suppress("NOTHING_TO_INLINE", "INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
internal inline class DataIndex(val value: Int) {
    inline operator fun inc(): org.readium.navigator.internal.lazy.DataIndex =
        org.readium.navigator.internal.lazy.DataIndex(value + 1)
    inline operator fun dec(): org.readium.navigator.internal.lazy.DataIndex =
        org.readium.navigator.internal.lazy.DataIndex(value - 1)
    inline operator fun plus(i: Int): org.readium.navigator.internal.lazy.DataIndex =
        org.readium.navigator.internal.lazy.DataIndex(value + i)
    inline operator fun minus(i: Int): org.readium.navigator.internal.lazy.DataIndex =
        org.readium.navigator.internal.lazy.DataIndex(value - i)
    inline operator fun minus(i: org.readium.navigator.internal.lazy.DataIndex): org.readium.navigator.internal.lazy.DataIndex =
        org.readium.navigator.internal.lazy.DataIndex(value - i.value)
    inline operator fun compareTo(other: org.readium.navigator.internal.lazy.DataIndex): Int = value - other.value
}
