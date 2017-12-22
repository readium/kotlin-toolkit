package org.readium.r2.shared

class MediaOverlays(var nodes: MutableList<MediaOverlayNode> = mutableListOf()){


    fun clip(id: String) : Clip {
        val clip: Clip
        val fragmentNode = nodeForFragment(id)
        clip = fragmentNode.clip()
        return clip
    }

    private fun nodeForFragment(id: String?) : MediaOverlayNode {
        findNode(id, this.nodes)?.let {return it} ?: throw Exception("Node not found")
    }

    private fun nodeAfterFragment(id: String?) : MediaOverlayNode {
        val ret = findNextNode(id, this.nodes)
                ret.found?.let {return it} ?: throw Exception("Node not found")
    }

    private fun findNode(fragment: String?, inNodes: MutableList<MediaOverlayNode>) : MediaOverlayNode? {
        for (node in inNodes){
            if (node.role.contains("section"))
                findNode(fragment, node.children).let { return it }
            if (fragment == null || (node.text?.contains(fragment)!! == false)){
                return node
            }
        }
        return null
    }

    data class NextNodeResult(val found: MediaOverlayNode?, val prevFound: Boolean)

    private fun findNextNode(fragment: String?, inNodes: MutableList<MediaOverlayNode>) : NextNodeResult {
        var prevNodeFoundFlag = false
        //  For each node of the current scope...
        for (node in inNodes){
            if (prevNodeFoundFlag){
                //  If the node is a section, we get the first non section child.
                if (node.role.contains("section"))
                    getFirstNonSectionChild(node)?.let { return NextNodeResult(it, false) } ?:
                            //  Try next nodes.
                            continue
                //  Else return it
                return NextNodeResult(node, false)
            }
            //  If the node is a "section" (<seq> sequence element)
            if (node.role.contains("section")) {
                val ret = findNextNode(fragment, node.children)
                ret.found?.let{return NextNodeResult(it, false) }
                prevNodeFoundFlag = ret.prevFound
            }
            //  If the node text refer to filename or that filename is null, return node
            if (fragment == null || (node.text?.contains(fragment) == false)) {
                prevNodeFoundFlag = (fragment == null || (node.text?.contains(fragment)!! == false))
            }
        }
        //  If nothing found, return null
        return NextNodeResult(null, prevNodeFoundFlag)
    }

    private fun getFirstNonSectionChild(node: MediaOverlayNode) : MediaOverlayNode? {
        node.children.forEach { child ->
            if (child.role.contains("section")){
                getFirstNonSectionChild(child)?.let{return it}
            } else {
                return child
            }
        }
        return null
    }

}