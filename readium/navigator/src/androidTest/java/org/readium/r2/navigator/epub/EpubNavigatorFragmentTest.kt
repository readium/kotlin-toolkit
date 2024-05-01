package org.readium.r2.navigator.epub

import android.app.Instrumentation
import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.readium.r2.navigator.R
import org.readium.r2.navigator.RestorationNotSupportedException
import org.readium.r2.navigator.saveScreenshot
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.InMemoryResource
import org.readium.r2.shared.util.zip.ZipArchiveOpener
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser


@OptIn(ExperimentalReadiumApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class EpubNavigatorFragmentTest {

    @get:Rule
    var nameRule = TestName()

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
        val context: Context = ApplicationProvider.getApplicationContext()
        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(
            contentResolver = context.contentResolver,
            httpClient = httpClient,
        )
        val publicationOpener = PublicationOpener(
            DefaultPublicationParser(
                context = ApplicationProvider.getApplicationContext(),
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )
//        val fixtures = Fixtures()
//        val url = fixtures.urlAt("childrens-literature.epub")

        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        val context2 = instrumentation.context
        val assetManager = context2.resources.assets
        val inputStream = assetManager.open("childrens-literature.epub")

        val epubFormatSpecification = Format(
            specification = FormatSpecification(Specification.Zip, Specification.Epub),
            mediaType = MediaType.EPUB,
            fileExtension = FileExtension("epub")
        )

        val asset = ZipArchiveOpener().open(epubFormatSpecification,
            InMemoryResource(inputStream.readBytes()))
            .getOrElse { throw Exception("Failed to open archive") }

//            val asset = assetRetriever.retrieve(asset)
//                .getOrElse { throw Exception("Failed to retrieve asset") }
        val publication = publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse { throw Exception("Failed to open publication: $it") }

        val scenario = launchFragmentInContainer<EpubNavigatorFragment>(
            factory = EpubNavigatorFactory(publication).createFragmentFactory(initialLocator = null)
        )
        scenario.onFragment { fragment ->
            fragment.goForward()
        }
        Thread.sleep(5000)
        val bitmap = onView(withId(R.id.constraint))
            .perform(swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft())
//            .check(matches(isDisplayed()))
            .captureToBitmap()

//        val bitmap = takeScreenshot()
//            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
        saveScreenshot(javaClass.simpleName, nameRule.methodName, bitmap)
//        println("PATH " + TestStorage.getOutputFileUri("${javaClass.simpleName}_${nameRule.methodName}"))
//        NativeScreenshot.capture("post_addition");
    }
}