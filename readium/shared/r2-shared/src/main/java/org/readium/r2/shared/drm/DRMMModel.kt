/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.shared.drm

import java.io.Serializable

data class DRMMModel(val type:String, val state:String, val provider:String, val issued:String, val updated:String, val start:String?, val end:String?, val prints:String, val copies:String):Serializable