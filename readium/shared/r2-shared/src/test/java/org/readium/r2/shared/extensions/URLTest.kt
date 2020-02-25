/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class URLTest {

    @Test fun `remove last component`() {
        assertEquals(URL("http://domain.com/two/"), URL("http://domain.com/two/paths").removeLastComponent())
        assertEquals(URL("http://domain.com/two/"), URL("http://domain.com/two/paths/").removeLastComponent())
        assertEquals(URL("http://domain.com/"), URL("http://domain.com/path").removeLastComponent())
        assertEquals(URL("http://domain.com/"), URL("http://domain.com/path/").removeLastComponent())
        assertEquals(URL("http://domain.com/"), URL("http://domain.com/").removeLastComponent())
        assertEquals(URL("http://domain.com"), URL("http://domain.com").removeLastComponent())
    }

}
