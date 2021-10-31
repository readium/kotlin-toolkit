package org.readium.r2.navigator2.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.R
import org.readium.r2.navigator2.settings.PresentationProperties
import org.readium.r2.navigator2.settings.PresentationSettings
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class NavigatorView(context: Context, attributes: AttributeSet? = null) : FrameLayout(context, attributes) {
    private val readingView: ReadingView =
        inflate(context, R.layout.navigator2_navigator_view, this)
            .findViewById(R.id.r2_navigator_view_reading_view)

    private var navEngine: NavigatorEngine? = null

    private val engine: NavigatorEngine
        get() = checkNotNull(navEngine) { "Load a publication before using the navigator." }

    var configuration: NavigatorConfiguration = NavigatorConfiguration()
        set(value) {
            check(navEngine == null) { "Set a configuration before loading a publication." }
            field = value
        }
    var settings: PresentationSettings = PresentationSettings()
        set(value) {
            field = value
            this.navEngine?.settings = value
        }

    var listener: NavigatorListener? = null
        set(value) {
            field = value
            this.navEngine?.listener = listener
        }

    val properties: PresentationProperties
        get() = this.engine.properties

    fun loadPublication(publication: Publication, links: List<Link> = publication.readingOrder) {
        require(links.isNotEmpty())
        val engine = NavigatorEngine(context, publication, links, configuration, readingView)
        engine.listener = listener
        engine.settings = settings
        this.navEngine = engine
    }

    /*
    private val loadedViews: List<View> get() =
    (0 until this.adapter.itemCount).mapNotNull { this.readingView.findViewByPosition(it) }

    val visibleResources: List<ResourceAdapter> get() =
    this.adapter.resourcesForView(this.visibleView)

    val loadedResources: List<ResourceAdapter> get() =
    this.loadedViews.flatMap { this.adapter.resourcesForView(it) }
    */

    val currentLocation: Locator
        get() = this.engine.currentLocation

    suspend fun goTo(locator: Locator) {
       withContext(Dispatchers.Main) {
           engine.goTo(locator)
       }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }
}

