/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import java.io.Serializable

data class MediaOverlays(private val nodes: List<MediaOverlayNode> = listOf()) : Serializable {
    fun clip(ref: String): Clip? {
        val fragmentNode = nodeForFragment(ref)
        return fragmentNode?.clip
    }

    fun nodeForFragment(ref: String?): MediaOverlayNode? = findNode(ref, this.nodes)

    private fun findNode(ref: String?, inNodes: List<MediaOverlayNode>): MediaOverlayNode? {
        for (node in inNodes) {
            if (node.role.contains("section"))
                return findNode(ref, node.children)
            else if (ref == null || node.text == ref)
                return node
        }
        return null
    }

    data class NextNodeResult(val found: MediaOverlayNode?, val prevFound: Boolean)

    private fun nodeAfterFragment(ref: String?): MediaOverlayNode? = findNextNode(ref, this.nodes).found

    private fun findNextNode(fragment: String?, inNodes: List<MediaOverlayNode>): NextNodeResult {
        var prevNodeFoundFlag = false
        //  For each node of the current scope...
        for (node in inNodes) {
            if (prevNodeFoundFlag) {
                //  If the node is a section, we get the first non section child.
                if (node.role.contains("section"))
                    getFirstNonSectionChild(node)?.let { return NextNodeResult(it, false) }
                else
                    return NextNodeResult(node, false)
            } else {
                //  If the node is a "section" (<seq> sequence element)
                if (node.role.contains("section")) {
                    val ret = findNextNode(fragment, node.children)
                    ret.found?.let { return NextNodeResult(it, false) }
                    prevNodeFoundFlag = ret.prevFound
                }
                //  If the node text refer to filename or that filename is null, return node
                else if (fragment == null || node.text == fragment) {
                    prevNodeFoundFlag = true
                }
            }
        }
        //  If nothing found, return null
        return NextNodeResult(null, prevNodeFoundFlag)
    }

    private fun getFirstNonSectionChild(node: MediaOverlayNode): MediaOverlayNode? {
        node.children.forEach { child ->
            if (child.role.contains("section")) {
                getFirstNonSectionChild(child)?.let { return it }
            } else {
                return child
            }
        }
        return null
    }

}
