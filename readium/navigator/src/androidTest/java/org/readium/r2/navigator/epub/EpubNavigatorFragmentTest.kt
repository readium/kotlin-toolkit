package org.readium.r2.navigator.epub

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.RestorationNotSupportedException

@RunWith(AndroidJUnit4::class)
@LargeTest
class EpubNavigatorFragmentTest {

    @Test
    fun crashesWithADummyFactory() {
        try {
            launchFragmentInContainer<EpubNavigatorFragment>(
                factory = EpubNavigatorFragment.createDummyFactory()
            )
            assert(false) { "RestorationNotSupportedException should have been thrown" }
        } catch (e: Exception) {
            assert(e.cause is RestorationNotSupportedException)
        }
    }
}