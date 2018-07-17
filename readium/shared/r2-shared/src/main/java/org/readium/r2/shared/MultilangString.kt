/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import java.io.Serializable

class MultilangString : Serializable{

    private val TAG = this::class.java.simpleName

    var singleString: String? = null
    var multiString: MutableMap<String, String> = mutableMapOf()

}