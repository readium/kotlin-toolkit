package org.readium.r2.navigator.epub

import SnapshotTest
import android.app.Instrumentation
import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.readium.r2.navigator.R
import org.readium.r2.navigator.RestorationNotSupportedException
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
        val context: Context = ApplicationProvider.getApplicationContext()
        val httpClient = DefaultHttpClient()

        val init = CompletableDeferred<Unit>()

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
            factory = EpubNavigatorFactory(publication).createFragmentFactory(
                initialLocator = null,
                paginationListener = object : EpubNavigatorFragment.PaginationListener {
                    override fun onPageLoaded() {
                        if (!init.isCompleted) {
                            init.complete(Unit)
                        }
                    }
                }
            )
        )
        scenario.onFragment { fragment ->
            fragment.goForward()
        }

        init.await()

        onView(withId(R.id.constraint))
//            .perform(swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft())
            .checkSnapshot()
    }
}
