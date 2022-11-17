package org.readium.navigator.image

import android.util.Size
import androidx.compose.ui.graphics.asImageBitmap
import org.readium.navigator.image.preferences.ImageDefaults
import org.readium.navigator.image.preferences.ImagePreferences
import org.readium.navigator.image.preferences.ImagePreferencesEditor
import org.readium.navigator.image.viewer.ImageData
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.allAreBitmap
import org.readium.r2.shared.publication.services.positions

@ExperimentalReadiumApi
class ImageNavigatorFactory private constructor(
    private val publication: Publication,
    private val images: List<ImageData>,
    private val positions: List<Locator>,
    private val configuration: Configuration = Configuration()
) {

    /**
     * Configuration for the [ImageNavigatorFactory].
     *
     * @param defaults navigator fallbacks for some preferences
     */
    data class Configuration(
        val defaults: ImageDefaults = ImageDefaults()
    )

    fun createNavigatorState(
        initialLocator: Locator? = null,
        initialPreferences: ImagePreferences? = null
    ): ImageNavigatorState {

        return ImageNavigatorState(
            images = images,
            positions = positions,
            publication = publication,
            initialPreferences = initialPreferences ?: ImagePreferences(),
            defaults = configuration.defaults,
            initialLocator = initialLocator,
        )
    }

    fun createPreferencesEditor(initialPreferences: ImagePreferences) =
        ImagePreferencesEditor(
            initialPreferences,
            publication.metadata,
            configuration.defaults
        )

    companion object {

        suspend fun create(publication: Publication): ImageNavigatorFactory {
            require(publication.readingOrder.allAreBitmap)
            val images = publication.readingOrder.map { computeImageData(it, publication) }
            val positions = publication.positions()
            return ImageNavigatorFactory(publication, images, positions)
        }

        private suspend fun computeImageData(link: Link, publication: Publication): ImageData {
            val width = link.width
            val height = link.height
            val size = if (width != null && height != null) {
                Size(width, height)
            } else {
                val resourceStream = ResourceInputStream(publication.get(link))
                BitmapFactory.getBitmapSize(resourceStream)
            }
            return ImageData(size) {
                publication.get(link)
                    .readAsBitmap()
                    .getOrNull()
                    ?.asImageBitmap()
            }
        }
    }
}