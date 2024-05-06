package org.readium.r2.navigator.epub

import androidx.fragment.app.testing.launchFragmentInContainer
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.readium.r2.navigator.RestorationNotSupportedException
import org.readium.r2.navigator.SnapshotTest

class EpubNavigatorFragmentTest : SnapshotTest() {

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

    @Test
    fun open() = runTest {
        EpubNavigatorRobot()
            .open("childrens-literature.epub")
            .onNavigator {
                goForward()
            }
            .onView()
//            .perform(swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft())
            .checkSnapshot()
    }
}
