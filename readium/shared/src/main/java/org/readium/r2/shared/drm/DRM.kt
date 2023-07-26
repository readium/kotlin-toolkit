/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.drm

import java.io.Serializable

@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
public class DRM {

    @Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
    public enum class Brand(public val rawValue: String) : Serializable {
        lcp("lcp");
    }

    @Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
    public enum class Scheme(public val rawValue: String) : Serializable {
        lcp("http://readium.org/2014/01/lcp");
    }
}

@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
public interface DRMLicense : Serializable {
    public val encryptionProfile: String?
    public fun decipher(data: ByteArray): ByteArray?
    public val canCopy: Boolean
    public fun copy(text: String): String?
}
