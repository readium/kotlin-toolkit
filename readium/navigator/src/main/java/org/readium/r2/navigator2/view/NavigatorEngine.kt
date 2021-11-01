package org.readium.r2.navigator2.view

import android.content.Context
import android.content.res.Configuration
import org.readium.r2.navigator2.settings.PresentationProperties
import org.readium.r2.navigator2.settings.PresentationSettings
import org.readium.r2.navigator2.view.html.HtmlSpreadAdapterFactory
import org.readium.r2.navigator2.view.image.ImageSpreadAdapterFactory
import org.readium.r2.navigator2.view.layout.EffectiveReadingProgression
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

internal class NavigatorEngine(
    private val context: Context,
    private val publication: Publication,
    private val links: List<Link>,
    private val configuration: NavigatorConfiguration,
    private val readingView: ReadingView,
) {
    var listener: NavigatorListener? = null
        set(value) {
            field = value
            this.readingView.scrollListener =  { value?.onLocationChanged(this.currentLocation) }
            this.readingView.singleTapListener = { value?.onTap(it) }
        }

    var settings: PresentationSettings = PresentationSettings()
        set(value) {
            val previous = field
            field = value
            if (previous == value) {
                return
            }

            properties = computeProperties()

            if (previous.spread != value.spread) {
                this.renewAdapter()
            }

            this.applyProperties(properties)
        }

    var properties: PresentationProperties = this.computeProperties()
        private set

    var adapter: NavigatorAdapter = this.renewAdapter()
        private set

    init {
        this.applyProperties(properties)
    }

    val currentLocation: Locator
        get() {
            val firstVisiblePosition = this.readingView.findFirstVisiblePosition()
            val firstVisibleView = this.readingView.findViewByPosition(firstVisiblePosition)!!
            val firstResource = this.adapter.resourcesForView(firstVisibleView).first()
            return firstResource.currentLocation.copyWithLocations(position = firstVisiblePosition)
        }

    private fun applyProperties(properties: PresentationProperties) {
        this.readingView.setReadingProgression(properties.readingProgression)
        this.readingView.setContinuous(properties.continuous)
        val adapterSettings = computeAdapterSettings()
        adapter.applySettings(adapterSettings)
    }

    private fun computeProperties(): PresentationProperties {
        val readingProgression = this.computeReadingProgression()
        val continuous = this.computeContinuous()
        return PresentationProperties(
            readingProgression = readingProgression,
            continuous = continuous,
        )
    }

    private fun computeReadingProgression(): EffectiveReadingProgression =
        when (settings.readingProgression) {
            ReadingProgression.RTL -> EffectiveReadingProgression.RTL
            ReadingProgression.LTR -> EffectiveReadingProgression.LTR
            ReadingProgression.TTB -> EffectiveReadingProgression.TTB
            ReadingProgression.BTT -> EffectiveReadingProgression.BTT
            ReadingProgression.AUTO ->  configuration.layoutPolicy.resolveReadingProgression(publication)
        }

    private fun computeContinuous() =
        settings.continuous == true || configuration.layoutPolicy.resolveContinuous(publication)

    private fun renewAdapter(): NavigatorAdapter {
        val adapterFactories = createAdapterFactories()
        adapter = NavigatorAdapter(context, links, adapterFactories)
        this.readingView.setAdapter(adapter)
        return adapter
    }

    private fun computeAdapterSettings(): NavigatorAdapter.Settings =
        NavigatorAdapter.Settings(
            fontSize = settings.fontSize
        )

    private fun createAdapterFactories(): List<SpreadAdapterFactory> {
        val imageSpreadAdapterFactory =
            ImageSpreadAdapterFactory(
                publication,
                properties.readingProgression,
                this::resolveSpreadHint,
                configuration.errorBitmap,
                configuration.emptyBitmap
            )

        val htmlSpreadAdapterFactory =
            configuration.baseUrl?.let { HtmlSpreadAdapterFactory(publication, it.toString()) }

        return listOfNotNull(
            imageSpreadAdapterFactory,
            htmlSpreadAdapterFactory
        )
    }

    private fun resolveSpreadHint(link: Link): Boolean {
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return when (settings.spread) {
            Presentation.Spread.AUTO -> configuration.layoutPolicy.resolveSpreadHint(link, publication, isLandscape)
            Presentation.Spread.BOTH -> true
            Presentation.Spread.NONE -> false
            Presentation.Spread.LANDSCAPE -> isLandscape
        }
    }

    suspend fun goTo(locator: Locator) {
        val spreadIndex = adapter.positionForHref(locator.href)
        readingView.scrollToPosition(spreadIndex)
        val view = checkNotNull(readingView.findViewByPosition(spreadIndex))
        adapter.scrollTo(locator.locations, view, spreadIndex)
    }
}
