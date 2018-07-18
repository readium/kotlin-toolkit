/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.drm

import java.io.Serializable


class Drm(brand: Brand) : Serializable {

    private val TAG = this::class.java.simpleName

    var scheme: Scheme

    var profile: String? = null
    var license: DrmLicense? = null

    enum class Brand(var v:String):Serializable {
        lcp("lcp")
    }
    enum class Scheme(var v:String):Serializable {
        lcp("http://readium.org/2014/01/lcp")
    }

    init {
        when (brand) {
            Brand.lcp -> scheme = Scheme.lcp
        }
    }

}