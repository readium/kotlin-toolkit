/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

/**
 * A controller for decorations.
 */
@ExperimentalReadiumApi
public interface DecorationController<L : DecorationLocation> {

    /**
     * Declares the current state of the decorations for each group. This property must be observable.
     *
     * The controller will decide when to actually render each decoration efficiently. Your only
     * responsibility is to submit the updated list of decorations when there are changes.
     * Name each decoration group as you see fit. A good practice is to use the name of the feature
     * requiring decorations, e.g. annotation, search, tts, etc.
     */
    public var decorations: PersistentMap<String, PersistentList<Decoration<L>>>
}

/**
 * Marker interface for decoration location.
 */
@ExperimentalReadiumApi
public interface DecorationLocation : Location

/**
 * A decoration is a user interface element drawn on top of a publication. It associates a [style]
 * to be rendered with a discrete [location] in the publication.
 *
 * For example, decorations can be used to draw highlights, images or buttons.
 *
 * @param id An identifier for this decoration. It must be unique in the group the decoration is applied to.
 * @param location Location in the publication where the decoration will be rendered.
 * @param style Declares the look and feel of the decoration.
 */
@ExperimentalReadiumApi
public data class Decoration<out L : DecorationLocation>(
    val id: Id,
    val location: L,
    val style: Style,
) {

    /** Unique identifier for a decoration. */
    @JvmInline
    public value class Id(public val value: String)

    /**
     * The Decoration Style determines the look and feel of a decoration once rendered by a
     * Navigator.
     *
     * It is media type agnostic, meaning that each Navigator will translate the style into a set of
     * rendering instructions which makes sense for the resource type.
     */
    public interface Style {

        public data class Highlight(
            @ColorInt override val tint: Int,
            override val isActive: Boolean = false,
        ) : Style, Tinted, Activable

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
}

/** Represents an atomic change in a list of [Decoration] objects. */
@ExperimentalReadiumApi
public sealed class DecorationChange<out L : DecorationLocation> {

    public data class Added<L : DecorationLocation>(
        val decoration: Decoration<L>,
    ) : DecorationChange<L>()

    public data class Updated<L : DecorationLocation>(
        val decoration: Decoration<L>,
    ) : DecorationChange<L>()

    public data class Moved<L : DecorationLocation>(
        val id: Decoration.Id,
        val fromPosition: Int,
        val toPosition: Int,
    ) : DecorationChange<L>()

    public data class Removed<L : DecorationLocation>(
        val id: Decoration.Id,
    ) : DecorationChange<L>()
}

/**
 * Lists the atomic changes between the receiver list and the [target] list of [Decoration] objects.
 *
 * The changes need to be applied in the same order, one by one.
 */
@ExperimentalReadiumApi
public suspend fun <L : DecorationLocation> List<Decoration<L>>.changesByHref(
    target: List<Decoration<L>>,
): Map<Url, List<DecorationChange<L>>> = withContext(Dispatchers.Default) {
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
                sourceDecoration.location == targetDecoration.location &&
                sourceDecoration.style == targetDecoration.style
        }
    })

    val changes = mutableMapOf<Url, List<DecorationChange<L>>>()

    fun registerChange(change: DecorationChange<L>, href: Url) {
        val resourceChanges = changes[href] ?: emptyList()
        changes[href] = resourceChanges + change
    }

    result.dispatchUpdatesTo(object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            for (i in 0 until count) {
                val decoration = target[position + i]
                registerChange(DecorationChange.Added(decoration), decoration.location.href)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            for (i in 0 until count) {
                val decoration = source[position + i]
                registerChange(DecorationChange.Removed<L>(decoration.id), decoration.location.href)
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
                decoration.location.href
            )
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            for (i in 0 until count) {
                val decoration = target[position + i]
                registerChange(DecorationChange.Updated(decoration), decoration.location.href)
            }
        }
    })

    changes
}
