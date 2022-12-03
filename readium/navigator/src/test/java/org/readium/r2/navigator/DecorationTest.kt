package org.readium.r2.navigator

import android.graphics.Color
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalDecorator::class)
@RunWith(RobolectricTestRunner::class)
class DecorationTest {

    @Test
    fun `Decorations can be compared`() {
        val d1a = Decoration(
            id = "1",
            locator = Locator("chapter.html", "text/html"),
            style = Decoration.Style.Highlight(tint = Color.RED),
            extras = mapOf("param" to "value"),
        )
        val d1b = Decoration(
            id = "1",
            locator = Locator("chapter.html", "text/html"),
            style = Decoration.Style.Highlight(tint = Color.RED),
            extras = mapOf("param" to "value"),
        )
        val d2 = Decoration(
            id = "2",
            locator = Locator("chapter2.html", "text/html"),
            style = Decoration.Style.Highlight(tint = Color.RED),
            extras = mapOf("param" to "value"),
        )

        assertTrue { d1a == d1a }
        assertTrue { d1a == d1b }
        assertFalse { d1a == d2 }
    }
}
