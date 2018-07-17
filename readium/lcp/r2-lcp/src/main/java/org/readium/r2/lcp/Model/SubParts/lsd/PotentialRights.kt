/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.Model.SubParts.lsd

import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*

class PotentialRights(json: JSONObject) {
    var end: Date?

    init {
        end = DateTime(json.getString("end")).toDate()
    }
}