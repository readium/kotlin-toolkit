package org.readium.navigator.image

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.navigator.image.preferences.ImageDefaults
import org.readium.navigator.image.preferences.ImagePreferences
import org.readium.navigator.image.preferences.ImageSettings
import org.readium.navigator.image.preferences.ImageSettingsResolver
import org.readium.navigator.image.viewer.ImageData
import org.readium.navigator.image.viewer.ImageViewerLayout
import org.readium.navigator.image.viewer.ImageViewerState
import org.readium.r2.navigator.SimplePresentation
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.Try
import timber.log.Timber

@ExperimentalReadiumApi
class ImageNavigatorState(
    internal val publication: Publication,
    images: List<ImageData>,
    private val positions: List<Locator>,
    initialPreferences: ImagePreferences,
    defaults: ImageDefaults,
    initialLocator: Locator?,
) : Configurable<ImageSettings, ImagePreferences> {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val settingsResolver: ImageSettingsResolver =
        ImageSettingsResolver(publication.metadata, defaults)

    private val settingsMutable: MutableStateFlow<ImageSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))

    private val initialResourceIndex: Int =
        initialLocator?.toResourceIndex() ?: 0

    private val locatorMutable: MutableStateFlow<Locator> =
        MutableStateFlow(publication.locatorFromLink(publication.readingOrder[initialResourceIndex])!!)

    internal val viewerState: ImageViewerState =
        ImageViewerState(
            images,
            settingsMutable.value.toViewerLayout(),
            initialResourceIndex
        )

    private val mutex: NavigatorMutex =
        NavigatorMutex()

    private fun ImageSettings.toViewerLayout(): ImageViewerLayout =
        ImageViewerLayout(
            contentScale = fit.toContentScale(),
            reverseLayout = readingProgression == ReadingProgression.RTL &&
                orientation == Orientation.Horizontal,
            snap = !scroll,
            orientation = orientation
        )

    private val ImageSettings.orientation: Orientation
        get() = when  {
            scroll && scrollAxis == Axis.VERTICAL -> Orientation.Vertical
            else -> Orientation.Horizontal
        }

    private fun Fit.toContentScale(): ContentScale =
        when (this) {
            Fit.CONTAIN -> ContentScale.Fit
            Fit.COVER -> ContentScale.Crop
            Fit.WIDTH -> ContentScale.FillWidth
            Fit.HEIGHT -> ContentScale.FillHeight
        }

    override val settings: StateFlow<ImageSettings> =
        settingsMutable

    override fun submitPreferences(preferences: ImagePreferences) {
        val newSettings = settingsResolver.settings(preferences)
        viewerState.updateLayout(newSettings.toViewerLayout())
        settingsMutable.value = newSettings
    }

    val presentation: StateFlow<VisualNavigator.Presentation>
        get() = settings.mapStateIn(coroutineScope) { it.toPresentation() }

    private fun ImageSettings.toPresentation() =
        SimplePresentation(
            readingProgression = readingProgression,
            scroll = scroll,
            axis = if (scroll) scrollAxis else Axis.HORIZONTAL
        )

    val locator: StateFlow<Locator>
        get() = locatorMutable

    /**
     * Go forward through the publication.
     *
     * The command execution will be canceled on new layouts.
     */
    suspend fun goForward(): Try<Unit, Exception> =
        mutex.relativeGo {
            goForwardWithoutLock()
        }

    private suspend fun goForwardWithoutLock(): Try<Unit, Exception> {
        Timber.v ("goForwardWithoutLock")
        val currentItemIndex = viewerState.lazyListState.firstVisibleItemIndex
        val totalItemCount = viewerState.lazyListState.layoutInfo.totalItemsCount

        if (currentItemIndex + 1 == totalItemCount) {
            return Try.failure(Exception.InvalidState("Reached end."))
        }
        viewerState.lazyListState.scrollToItem(currentItemIndex + 1)
        updateLocator()
        return Try.success(Unit)
    }

    /**
     * Go forward through the publication.
     *
     * The command execution will be canceled on new layouts.
     */
    suspend fun goBackward(): Try<Unit, Exception> =
        mutex.relativeGo {
            goBackwardWithoutLock()
        }

    private suspend fun goBackwardWithoutLock(): Try<Unit, Exception> {
        Timber.v("goBackwardWithoutLock")
        val currentItemIndex = viewerState.lazyListState.firstVisibleItemIndex

        if (currentItemIndex == 0) {
            return Try.failure(Exception.InvalidState("Reached beginning."))
        }
        viewerState.lazyListState.scrollToItem(currentItemIndex - 1)
        updateLocator()
        return Try.success(Unit)
    }

    /**
     * Go to the beginning of the given link. The command will survive new layouts.
     */
    suspend fun go(link: Link): Try<Unit, Exception> =
        mutex.absoluteGo {
            publication.locatorFromLink(link)
                ?.let { goWithoutLock(it) }
                ?: Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}."))
        }

    /**
     * Go to the given locator. The command will survive new layouts.
     */
    suspend fun go(locator: Locator): Try<Unit, Exception> =
        mutex.absoluteGo {
            goWithoutLock(locator)
        }

    private suspend fun goWithoutLock(locator: Locator): Try<Unit, Exception> {
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        viewerState.lazyListState.scrollToItem(itemIndex)
        updateLocator()
        return Try.success(Unit)
    }

    private fun updateLocator() {
        val currentIndex = viewerState.lazyListState.firstVisibleItemIndex
        val locator = positions[currentIndex]
        locatorMutable.value = locator
    }

    private fun Locator.toResourceIndex(): Int? =
        publication.readingOrder.indexOfFirstWithHref(href)

    sealed class Exception(override val message: String) : kotlin.Exception(message) {

        class InvalidArgument(message: String): Exception(message)

        class InvalidState(message: String) : Exception(message)
    }
}
