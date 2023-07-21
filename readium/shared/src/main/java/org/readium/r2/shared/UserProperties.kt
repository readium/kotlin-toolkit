/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.readium.r2.shared

import java.io.Serializable

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
sealed class UserProperty(var ref: String, var name: String)

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
class Enumerable(var index: Int, private val values: List<String>, ref: String, name: String)

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
class Incremental(
    var value: Float,
    val min: Float,
    val max: Float,
    private val step: Float,
    private val suffix: String,
    ref: String,
    name: String
)

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
class Switchable(onValue: String, offValue: String, var on: Boolean, ref: String, name: String)

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
class UserProperties : Serializable
