package org.readium.r2.lcp

import org.junit.Test

import org.junit.Assert.*
import java.net.URL

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    val lcpLicense = LcpLicense(URL("Tests/lcplicense/lcpl"), false)

    @Test
    fun getData(){
        assert(true)
    }
}
