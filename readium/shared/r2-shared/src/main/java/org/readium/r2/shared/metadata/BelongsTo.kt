/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.shared.metadata

import org.readium.r2.shared.Collection


class BelongsTo() {

    var series:MutableList<Collection> = mutableListOf()
    var collection:MutableList<Collection> = mutableListOf()

}
