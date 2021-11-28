package org.readium.r2.navigator2.view.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.util.Size
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.navigator.extensions.readAsBitmap
import org.readium.r2.navigator2.view.ResourceAdapter
import org.readium.r2.navigator2.view.SpreadAdapter
import org.readium.r2.navigator2.view.layout.EffectiveReadingProgression
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import kotlin.math.max


internal class ImageSpreadAdapter(
    override val links: List<Link>,
    private val publication: Publication,
    private val readingProgression: EffectiveReadingProgression,
    private val errorBitmap: (Size) -> Bitmap,
    private val emptyBitmap: (Size) -> Bitmap
) : SpreadAdapter {

    private var bindingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun bind(view: View) {
        check(view is ImageView)

        //val oneMaxSize = Size(view.width / 2, view.height)
        val oneMaxSize = Size(3000, 3000)

        //view.setImageBitmap(errorBitmap(oneMaxSize))

       // view.adjustViewBounds = publication.metadata.presentation.continuous


        bindingJob = scope.launch {
            val bitmaps = links.map {
                publication.get(it)
                    .readAsBitmap(oneMaxSize)
                    .getOrElse { errorBitmap(oneMaxSize) }
            }
            val image =
                when (bitmaps.size) {
                    0 -> emptyBitmap(oneMaxSize)
                    1 -> bitmaps[0]
                    2 -> mergeBitmaps(bitmaps[0], bitmaps[1])
                    else -> throw IllegalStateException()
                }
            view.setImageBitmap(image)
            //val params = RecyclerView.LayoutParams(image.width, image.height);
            //view.setLayoutParams(params)
            //view.requestLayout()
        }
    }

    override fun unbind(view: View) {
        bindingJob?.cancel()
        bindingJob = null
    }

    override fun scrollForLocations(locations: Locator.Locations, view: View): PointF {
        return PointF(0f, 0f)
    }

    override fun resourceAdapters(view: View): List<ResourceAdapter> {
        return links.map {
            ImageResourceAdapter(it, view)
        }
    }

    private fun mergeBitmaps(first: Bitmap, second: Bitmap): Bitmap {
        val left = when(readingProgression) {
            EffectiveReadingProgression.LTR -> first
            EffectiveReadingProgression.RTL -> second
            else -> throw java.lang.IllegalStateException()
        }
        val right = when(readingProgression) {
            EffectiveReadingProgression.LTR -> second
            EffectiveReadingProgression.RTL -> first
            else -> throw java.lang.IllegalStateException()
        }
        val width = left.width + right.width
        val height = max(left.height, right.height)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(left, 0f, 0f, null)
        canvas.drawBitmap(right, left.width.toFloat(), 0f, null)
        return result
    }
}
