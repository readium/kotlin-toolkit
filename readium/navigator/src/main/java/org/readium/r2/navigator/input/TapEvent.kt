package org.readium.r2.navigator.input

import android.graphics.PointF
import android.graphics.RectF
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.content.Content

/**
 * Represents a tap event emitted by a navigator at the given [point].
 *
 * All the points are relative to the navigator view.
 *
 * @param targetElement The content element under the pointer, if recognized by the navigator
 * (e.g. an image). Only some navigators populate this property.
 */
@OptIn(ExperimentalReadiumApi::class)
public data class TapEvent(
    val point: PointF,
    @property:ExperimentalReadiumApi val targetElement: TargetElement? = null,
)

/**
 * A content element targeted by a pointer event, paired with its on-screen [rect].
 *
 * @param rect Frame of the element relative to the navigator's view, in device pixels.
 * @param content The content element under the pointer.
 */
@ExperimentalReadiumApi
public data class TargetElement(
    val rect: RectF,
    val content: Content.Element,
)
