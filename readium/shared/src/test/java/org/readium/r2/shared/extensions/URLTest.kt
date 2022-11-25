/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Test

class URLTest {

    @Test fun `remove last component`() {
        assertEquals(URL("http://domain.com/two/"), URL("http://domain.com/two/paths").removeLastComponent())
        assertEquals(URL("http://domain.com/two/"), URL("http://domain.com/two/paths/").removeLastComponent())
        assertEquals(URL("http://domain.com/"), URL("http://domain.com/path").removeLastComponent())
        assertEquals(URL("http://domain.com/"), URL("http://domain.com/path/").removeLastComponent())
        assertEquals(URL("http://domain.com/"), URL("http://domain.com/").removeLastComponent())
        assertEquals(URL("http://domain.com"), URL("http://domain.com").removeLastComponent())
        assertEquals(URL("http://domain.com/two/"), URL("http://domain.com/two/paths?a=1&b=2").removeLastComponent())
        assertEquals(URL("http://domain.com/two/"), URL("http://domain.com/two/paths/?a=1b=2").removeLastComponent())
    }
}
