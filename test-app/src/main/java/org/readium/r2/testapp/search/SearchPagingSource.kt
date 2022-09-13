/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.readium.r2.shared.Search
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.SearchTry

@OptIn(Search::class)
class SearchPagingSource(
    private val listener: Listener?
) : PagingSource<Unit, Locator>() {

    interface Listener {
        suspend fun next(): SearchTry<LocatorCollection?>
    }

    override val keyReuseSupported: Boolean get() = true

    override fun getRefreshKey(state: PagingState<Unit, Locator>): Unit? = null

    override suspend fun load(params: LoadParams<Unit>): LoadResult<Unit, Locator> {
        listener ?: return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

        return try {
            val page = listener.next().getOrThrow()
            LoadResult.Page(
                data = page?.locators ?: emptyList(),
                prevKey = null,
                nextKey = if (page == null) null else Unit
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
