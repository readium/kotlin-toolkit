/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator

import android.graphics.PointF
import android.graphics.RectF
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url

/**
 * A navigator able to render arbitrary decorations over a publication.
 */
public interface DecorableNavigator : Navigator {
    /**
     * Declares the current state of the decorations in the given decoration [group].
     *
     * The Navigator will decide when to actually render each decoration efficiently. Your only
     * responsibility is to submit the updated list of decorations when there are changes.
     * Name each decoration group as you see fit. A good practice is to use the name of the feature
     * requiring decorations, e.g. annotation, search, tts, etc.
     */
    public suspend fun applyDecorations(decorations: List<Decoration>, group: String)

    /**
     * Indicates whether the Navigator supports the given decoration [style] class.
     *
     * You should check whether the Navigator supports drawing the decoration styles required by a
     * particular feature before enabling it. For example, underlining an audiobook does not make
     * sense, so an Audiobook Navigator would not support the `underline` decoration style.
     */
    public fun <T : Decoration.Style> supportsDecorationStyle(style: KClass<T>): Boolean

    /**
     * Registers a new [listener] for decoration interactions in the given [group].
     */
    public fun addDecorationListener(group: String, listener: Listener)

    /**
     * Removes the given [listener] for all decoration interactions.
     */
    public fun removeDecorationListener(listener: Listener)

    public interface Listener {

        /**
         * Called when the user activates a decoration, e.g. with a click or tap.
         *
         * @param event Holds the metadata about the interaction event.
         * @return Whether the listener handled the interaction.
         */
        public fun onDecorationActivated(event: OnActivatedEvent): Boolean
    }

    /**
     * Holds the metadata about a decoration activation interaction.
     *
     * @param decoration Activated decoration.
     * @param group Name of the group the decoration belongs to.
     * @param rect Frame of the bounding rect for the decoration, in the coordinate of the
     *        navigator view. This is only useful in the context of a VisualNavigator.
     * @param point Event point of the interaction, in the coordinate of the navigator view. This is
     *        only useful in the context of a VisualNavigator.
     */
    public data class OnActivatedEvent(
        val decoration: Decoration,
        val group: String,
        val rect: RectF? = null,
        val point: PointF? = null,
    )
}

/**
 * A decoration is a user interface element drawn on top of a publication. It associates a [style]
 * to be rendered with a discrete [locator] in the publication.
 *
 * For example, decorations can be used to draw highlights, images or buttons.
 *
 * @param id An identifier for this decoration. It must be unique in the group the decoration is applied to.
 * @param locator Location in the publication where the decoration will be rendered.
 * @param style Declares the look and feel of the decoration.
 * @param extras Additional context data specific to a reading app. Readium does not use it.
 */
@Parcelize
public data class Decoration(
    val id: DecorationId,
    val locator: Locator,
    val style: Style,
    val extras: @WriteWith<JSONParceler> Map<String, Any> = mapOf(),
) : JSONable, Parcelable {

    /**
     * The Decoration Style determines the look and feel of a decoration once rendered by a
     * Navigator.
     *
     * It is media type agnostic, meaning that each Navigator will translate the style into a set of
     * rendering instructions which makes sense for the resource type.
     */
    public interface Style : Parcelable {
        @Parcelize
        public data class Highlight(
            @ColorInt override val tint: Int,
            override val isActive: Boolean = false,
        ) : Style, Tinted, Activable

        @Parcelize
        public data class Underline(
            @ColorInt override val tint: Int,
            override val isActive: Boolean = false,
        ) : Style, Tinted, Activable

        /** A type of [Style] which has a tint color. */
        public interface Tinted {
            @get:ColorInt public val tint: Int
        }

        /** A type of [Style] which can be in an "active" state. */
        public interface Activable {
            public val isActive: Boolean
        }
    }

    override fun toJSON(): JSONObject = JSONObject().apply {
        put("id", id)
        put("locator", locator.toJSON())
        putOpt("style", style::class.qualifiedName)
    }
}

/** Unique identifier for a decoration. */
public typealias DecorationId = String

/** Represents an atomic change in a list of [Decoration] objects. */
public sealed class DecorationChange {
    public data class Added(val decoration: Decoration) : DecorationChange()
    public data class Updated(val decoration: Decoration) : DecorationChange()
    public data class Moved(val id: DecorationId, val fromPosition: Int, val toPosition: Int) : DecorationChange()
    public data class Removed(val id: DecorationId) : DecorationChange()
}

/**
 * Lists the atomic changes between the receiver list and the [target] list of [Decoration] objects.
 *
 * The changes need to be applied in the same order, one by one.
 */
public suspend fun List<Decoration>.changesByHref(target: List<Decoration>): Map<Url, List<DecorationChange>> = withContext(
    Dispatchers.Default
) {
    val source = this@changesByHref
    val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int = source.size
        override fun getNewListSize(): Int = target.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            source[oldItemPosition].id == target[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val sourceDecoration = source[oldItemPosition]
            val targetDecoration = target[newItemPosition]
            return sourceDecoration.id == targetDecoration.id &&
                sourceDecoration.locator == targetDecoration.locator &&
                sourceDecoration.style == targetDecoration.style
        }
    })

    val changes = mutableMapOf<Url, List<DecorationChange>>()

    fun registerChange(change: DecorationChange, locator: Locator) {
        val resourceChanges = changes[locator.href] ?: emptyList()
        changes[locator.href] = resourceChanges + change
    }

    result.dispatchUpdatesTo(object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            for (i in 0 until count) {
                val decoration = target[position + i]
                registerChange(DecorationChange.Added(decoration), decoration.locator)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            for (i in 0 until count) {
                val decoration = source[position + i]
                registerChange(DecorationChange.Removed(decoration.id), decoration.locator)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            val decoration = target[toPosition]
            registerChange(
                DecorationChange.Moved(
                    decoration.id,
                    fromPosition = fromPosition,
                    toPosition = toPosition
                ),
                decoration.locator
            )
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            for (i in 0 until count) {
                val decoration = target[position + i]
                registerChange(DecorationChange.Updated(decoration), decoration.locator)
            }
        }
    })

    changes
}
