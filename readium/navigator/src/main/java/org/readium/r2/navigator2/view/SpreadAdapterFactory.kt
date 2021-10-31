package org.readium.r2.navigator2.view

import android.content.Context
import android.view.View
import android.util.Size
import org.readium.r2.shared.publication.Link

interface SpreadAdapterFactory {

    fun createSpread(links: List<Link>): Pair<SpreadAdapter, List<Link>>?

    fun createView(context: Context, viewportSize: Size): View
}