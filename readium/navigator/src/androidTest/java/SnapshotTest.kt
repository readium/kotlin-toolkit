import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith

/**
 * A test suite able to take snapshots of the UI and compare them to expected screenshots.
 *
 * When a test fails, the actual screenshots are stored under:
 * readium/navigator/build/outputs/connected_android_test_additional_output/
 *
 * After validating them, you should copy them as new references to:
 * readium/navigator/src/androidTest/assets/snapshots/
 *
 * The screenshots are stored under a folder named after the test class, and the file name is
 * <API version>-<width>x<height>-<test method name>-<snapshot number>.png
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
abstract class SnapshotTest {

    /**
     * Captures a screenshot of the current view and compares it to the expected snapshot.
     */
    fun ViewInteraction.checkSnapshot(): ViewInteraction {
        snapshotCount += 1

        val screenshot = captureToBitmap()

        val name = buildString {
            append("snapshots/")
            append(this@SnapshotTest.javaClass.name.replace(".", "/") + "/")
            val api = android.os.Build.VERSION.SDK_INT
            append("$api-${screenshot.width}x${screenshot.height}")
            append("-${nameRule.methodName}")
            append("-" + String.format("%02d", snapshotCount))
        }

        try {
            val snapshot = loadSnapshot(name)
            if (!screenshot.sameAs(snapshot)) {
                throw Exception("Screenshot does not match expected snapshot: $name.png")
            }
        } catch (e: Exception) {
            screenshot.writeToTestStorage(name)
            failures.add(e)
        }

        return this
    }

    private fun loadSnapshot(name: String): Bitmap =
        try {
            assets.open("$name.png")
                .use { BitmapFactory.decodeStream(it) }
                ?: throw Exception("Invalid expected snapshot: assets/$name.png")
        } catch (e: FileNotFoundException) {
            throw Exception("Expected snapshot not found: assets/$name.png")
        }

    @get:Rule
    var nameRule = TestName()

    private var snapshotCount = 0
    private var failures: MutableList<Exception> = mutableListOf()
    private val assets = InstrumentationRegistry.getInstrumentation().context.resources.assets

    @Before
    fun before() {
        snapshotCount = 0
        failures.clear()
    }

    @After
    fun after() {
        if (failures.isNotEmpty()) {
            val message = buildString {
                append("Failed snapshots:\n")
                for (failure in failures) {
                    append(" - ${failure.message}\n")
                }
            }
            throw AssertionError(message)
        }
    }
}

