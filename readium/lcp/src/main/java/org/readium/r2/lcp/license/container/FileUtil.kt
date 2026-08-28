/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import java.io.File
internal fun File.moveTo(target: File) {
    if (!this.renameTo(target)) {
        this.copyTo(target, overwrite = true)
        this.delete()
    }
}
