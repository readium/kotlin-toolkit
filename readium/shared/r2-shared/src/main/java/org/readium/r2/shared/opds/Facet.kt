/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import org.readium.r2.shared.Link
import java.io.Serializable


data class Facet(val title: String): Serializable {
    var metadata: OpdsMetadata
    var links = mutableListOf<Link>()

    init {
        this.metadata = OpdsMetadata(title = title)
    }

}
