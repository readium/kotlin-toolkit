package org.readium.r2.navigator2.view

import android.content.Context
import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.firstWithHref
import timber.log.Timber

internal class NavigatorAdapter(
    private val context: Context,
    private val links: List<Link>,
    private val adapterFactories: List<SpreadAdapterFactory>,
) : RecyclerView.Adapter<NavigatorAdapter.ViewHolder>() {

    data class ViewHolder(
        val view: View,
        var adapter: SpreadAdapter? = null
    ) : RecyclerView.ViewHolder(view)

    data class Settings(
        val fontSize: Double
    )

    private data class Spread(
        val adapter: SpreadAdapter,
        val viewType: Int
    )

    private val spreads: List<Spread> =
        computeSpreads()

    private val boundAdapters: MutableSet<SpreadAdapter> =
        mutableSetOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val factory = adapterFactories[viewType]
        val viewPortSize = Size(parent.width, parent.height)
        val view = factory.createView(context, viewPortSize)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val spread = spreads[position]
        spread.adapter.bind(holder.itemView)
        holder.adapter = spread.adapter
        holder.itemView.tag = position
        boundAdapters.add(spread.adapter)
    }

    override fun getItemCount(): Int =
        spreads.size

    override fun getItemViewType(position: Int): Int =
        spreads[position].viewType

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.adapter?.unbind(holder.view)
        holder.adapter = null
        holder.itemView.tag = null
        boundAdapters.remove(holder.adapter)
    }

    fun positionForHref(href: String): Int {
        return spreads.indexOfFirst { it.adapter.links.firstWithHref(href) != null }
    }

    fun hrefForPosition(position: Int) {

    }

    fun resourcesForView(view: View): List<ResourceAdapter> {
        val position = view.tag as Int
        val adapter = spreads[position].adapter
        return adapter.resourceAdapters(view)
    }

    fun scrollTo(locations: Locator.Locations, view: View, position: Int) {
        Timber.d("Adapter.scrollTo called")
        val adapter = spreads[position].adapter
        adapter.scrollForLocations(locations, view)
    }

    fun applySettings(settings: Settings) {
        spreads.forEach {
            it.adapter.applySettings()
        }
    }

    private fun computeSpreads(): List<Spread> {
        val spreads: MutableList<Spread> = mutableListOf()
        var remaining: List<Link> = links

        while (remaining.isNotEmpty()) {
            var found = false

            for ((index, factory) in adapterFactories.withIndex()) {
                val result = factory.createSpread(remaining)
                if (result != null) {
                    val spread = Spread(result.first, index)
                    spreads.add(spread)
                    remaining = result.second
                    found = true
                    break
                }
            }

            if (!found) {
                val first = remaining.first()
                remaining = remaining.subList(1, remaining.size)
                Timber.w("Skipping resource $first because no adapter supports it.")
            }
        }

        return spreads
    }
}