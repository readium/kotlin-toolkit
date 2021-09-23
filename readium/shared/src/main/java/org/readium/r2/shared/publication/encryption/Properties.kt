/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.encryption

import org.json.JSONObject
import org.readium.r2.shared.publication.Properties

// Encryption extensions for link [Properties].

/**
 * Indicates that a resource is encrypted/obfuscated and provides relevant information for
 * decryption.
 */
val Properties.encryption: Encryption?
    get() = (this["encrypted"] as? Map<*, *>)
        ?.let { Encryption.fromJSON(JSONObject(it)) }
