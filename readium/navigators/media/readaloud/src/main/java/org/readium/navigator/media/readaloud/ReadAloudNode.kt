/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationObject
import org.readium.r2.shared.guided.GuidedNavigationRef
import org.readium.r2.shared.guided.GuidedNavigationRole
import org.readium.r2.shared.guided.GuidedNavigationText

@ExperimentalReadiumApi
public class ReadAloudNode(
    public val text: GuidedNavigationText?,
    public val refs: Set<GuidedNavigationRef>,
    public val roles: Set<GuidedNavigationRole>,
    public val children: List<ReadAloudNode>,
) {

    public var parent: ReadAloudNode? = null
        internal set

    public companion object {

        internal fun fromGuidedNavigationObject(
            guidedNavigationObject: GuidedNavigationObject,
        ): ReadAloudNode {
            val children = guidedNavigationObject.children
                .map { fromGuidedNavigationObject(it) }

            val node = ReadAloudNode(
                text = guidedNavigationObject.text,
                refs = guidedNavigationObject.refs,
                roles = guidedNavigationObject.roles,
                children = children,
            )

            children.forEach { it.parent = node }

            return node
        }
    }
}

@ExperimentalReadiumApi
public fun ReadAloudNode.next(): ReadAloudNode? {
    nextDown()?.let { return it }

    nextRight()?.let { return it }

    return nextUp()
}

@ExperimentalReadiumApi
public fun ReadAloudNode.previous(): ReadAloudNode? {
    previousLeft()?.lastDescendant()?.let { return it }

    previousUp()?.lastDescendant()?.let { return it }

    return null
}

private fun ReadAloudNode.lastDescendant(): ReadAloudNode? {
    val lastChild = children.lastOrNull()

    return if (lastChild == null) {
        this
    } else {
        lastChild.lastDescendant()
    }
}

private fun ReadAloudNode.nextDown(): ReadAloudNode? {
    if (children.isEmpty()) {
        return null
    }

    return children.first()
}

private fun ReadAloudNode.nextUp(): ReadAloudNode? {
    return parent?.nextRight() ?: parent?.nextUp()
}

private fun ReadAloudNode.previousUp(): ReadAloudNode? {
    return parent?.previousLeft() ?: parent?.previousUp()
}

private fun ReadAloudNode.nextRight(): ReadAloudNode? {
    val parent = parent ?: return null

    val siblings = parent.children

    val currentIndex = siblings.indexOf(this)
    check(currentIndex != -1)

    return if (currentIndex < siblings.size - 1) {
        siblings[currentIndex + 1]
    } else {
        null
    }
}

private fun ReadAloudNode.previousLeft(): ReadAloudNode? {
    val parent = parent ?: return null

    val siblings = parent.children

    val currentIndex = siblings.indexOf(this)
    check(currentIndex != -1)

    return if (currentIndex > 0) {
        siblings[currentIndex - 1]
    } else {
        null
    }
}
