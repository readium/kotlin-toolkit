/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.readium.r2.shared.publication.Link

@Parcelize
data class Facet(val title: String) : Parcelable {
    var metadata: OpdsMetadata = OpdsMetadata(title = title)
    var links = mutableListOf<Link>()
}
