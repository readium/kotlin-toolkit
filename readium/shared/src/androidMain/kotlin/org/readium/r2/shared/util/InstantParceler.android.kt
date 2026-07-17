/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.os.Parcel
import kotlin.time.Instant
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
public actual object InstantParceler : Parceler<Instant> {

    override fun create(parcel: Parcel): Instant =
        Instant.fromEpochMilliseconds(parcel.readLong())

    override fun Instant.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(toEpochMilliseconds())
    }
}
