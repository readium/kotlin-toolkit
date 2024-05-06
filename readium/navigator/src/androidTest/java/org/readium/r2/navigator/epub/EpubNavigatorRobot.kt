package org.readium.r2.navigator.epub

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.withId
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.readium.r2.navigator.R
import org.readium.r2.navigator.require
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.DefaultArchiveOpener
import org.readium.r2.shared.util.asset.DefaultFormatSniffer
import org.readium.r2.shared.util.file.AndroidAssetResourceFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

@OptIn(ExperimentalReadiumApi::class, ExperimentalCoroutinesApi::class)
class EpubNavigatorRobot {

    companion object {
        private val assetsUrl = AbsoluteUrl("file:///android_asset/publications/")!!
    }

    private val assetRetriever: AssetRetriever
    private val publicationOpener: PublicationOpener
    private val scenario = CompletableDeferred<FragmentScenario<EpubNavigatorFragment>>()

    init {
        val context: Context = ApplicationProvider.getApplicationContext()

        val httpClient = DefaultHttpClient()

        assetRetriever = AssetRetriever(
            resourceFactory = AndroidAssetResourceFactory(context.assets),
            archiveOpener = DefaultArchiveOpener(),
            formatSniffer = DefaultFormatSniffer()
        )

        publicationOpener = PublicationOpener(
            DefaultPublicationParser(
                context = ApplicationProvider.getApplicationContext(),
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )
    }

    suspend fun open(filename: String): EpubNavigatorRobot {
        val url = assetsUrl.resolve(RelativeUrl(filename)!!)
        val asset = assetRetriever.retrieve(url).require()
        val publication = publicationOpener.open(asset, allowUserInteraction = false).require()

        val pageLoaded = CompletableDeferred<Unit>()

        val loadingScenario = launchFragmentInContainer<EpubNavigatorFragment>(
            factory = EpubNavigatorFactory(publication).createFragmentFactory(
                initialLocator = null,
                paginationListener = object : EpubNavigatorFragment.PaginationListener {
                    override fun onPageLoaded() {
                        if (!pageLoaded.isCompleted) {
                            pageLoaded.complete(Unit)
                        }
                    }
                }
            )
        )

        try {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(30.seconds) {
                    pageLoaded.await()
                }
            }
        } catch (ex: CancellationException) {
            throw AssertionError("EpubNavigatorFragment did not load the publication in time")
        }

        scenario.complete(loadingScenario)

        return this
    }

    fun onNavigator(action: EpubNavigatorFragment.() -> Unit): EpubNavigatorRobot {
        scenario.getCompleted().onFragment {
            it.action()
        }
        return this
    }

    fun onView(): ViewInteraction {
        require(scenario.isCompleted)
        return androidx.test.espresso.Espresso.onView(withId(R.id.constraint))
    }
}