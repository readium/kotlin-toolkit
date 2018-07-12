/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.shared

import java.io.Serializable

class MetadataItem : Serializable{

    private val TAG = this::class.java.simpleName

    var property: String? = null
    var value: String? = null
    var children: MutableList<MetadataItem> = mutableListOf()
}