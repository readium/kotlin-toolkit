package org.readium.r2.navigator.epub

import org.readium.r2.navigator.SnapshotTest
import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.readium.r2.navigator.R
import org.readium.r2.navigator.RestorationNotSupportedException
import org.readium.r2.navigator.require
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.DefaultArchiveOpener
import org.readium.r2.shared.util.asset.DefaultFormatSniffer
import org.readium.r2.shared.util.file.AndroidAssetResourceFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
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
            resourceFactory = AndroidAssetResourceFactory(context.assets),
            archiveOpener = DefaultArchiveOpener(),
            formatSniffer = DefaultFormatSniffer()
        )

        val publicationOpener = PublicationOpener(
            DefaultPublicationParser(
                context = ApplicationProvider.getApplicationContext(),
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )

        val file = AbsoluteUrl("file:///android_asset/publications/childrens-literature.epub")!!

        val asset = assetRetriever.retrieve(file).require()
        val publication = publicationOpener.open(asset, allowUserInteraction = false).require()

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
