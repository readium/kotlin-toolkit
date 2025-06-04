/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */
@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.LocatorAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun <G : GoLocation> Outline(
    modifier: Modifier = Modifier,
    publication: Publication,
    locatorAdapter: LocatorAdapter<*, G>,
    onBackActivated: () -> Unit,
    onTocItemActivated: (G) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize(),
        topBar = { TopBar(onBackActivated) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Contents(
                    modifier = modifier,
                    publication = publication,
                    locatorAdapter = locatorAdapter,
                    onItemActivated = onTocItemActivated
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBackActivated: () -> Unit,
) {
    TopAppBar(
        title = { Text("Contents") },
        navigationIcon = {
            IconButton(
                onClick = onBackActivated
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}

@Composable
private fun <G : GoLocation> Contents(
    modifier: Modifier = Modifier,
    publication: Publication,
    locatorAdapter: LocatorAdapter<*, G>,
    onItemActivated: (G) -> Unit,
) {
    val items = publication.tableOfContents
        .flatMap { it.toTocItems(publication, locatorAdapter) }

    val scrollState = rememberScrollState()

    Contents(
        modifier = modifier.verticalScroll(scrollState),
        items = items,
        onClick = onItemActivated
    )
}

private data class TocItem<G : GoLocation>(
    val title: String,
    val locator: G,
    val depth: Int,
)

private fun <G : GoLocation> Link.toTocItems(
    publication: Publication,
    locatorAdapter: LocatorAdapter<*, G>,
    depth: Int = 0,
): List<TocItem<G>> {
    val locator = publication.locatorFromLink(this@toTocItems)
        ?: return emptyList()

    val goLocation = with(locatorAdapter) { locator.toGoLocation() }

    val title = locator.title ?: locator.href.filename ?: ""

    return buildList {
        add(TocItem(title, goLocation, depth))
        for (child in children) {
            addAll(child.toTocItems(publication, locatorAdapter, depth + 1))
        }
    }
}

@Composable
private fun <G : GoLocation> Contents(
    modifier: Modifier = Modifier,
    items: List<TocItem<G>>,
    onClick: (G) -> Unit,
    depth: Int = 0,
) {
    Column(modifier) {
        for (item in items) {
            TocItem(
                item = item,
                onClick = onClick,
                depth = depth,
            )
        }
    }
}

@Composable
private fun <G : GoLocation> TocItem(
    modifier: Modifier = Modifier,
    item: TocItem<G>,
    onClick: (G) -> Unit,
    depth: Int = 0,
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.title,
            )
        },
        modifier = modifier
            .clickable { onClick(item.locator) }
            .padding(start = 24.dp * depth)
    )
}
