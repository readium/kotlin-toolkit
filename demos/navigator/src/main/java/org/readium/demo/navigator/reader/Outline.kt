/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */
@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.readium.demo.navigator.decorations.Highlight
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

@Composable
fun Outline(
    modifier: Modifier = Modifier,
    publication: Publication,
    highlights: List<Highlight>,
    onBackActivated: () -> Unit,
    onTocItemActivated: (Url) -> Unit,
    onHighlightActivated: (Highlight) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBackActivated = onBackActivated
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier.padding(padding)
            ) {
                var selectedTab by remember { mutableIntStateOf(0) }

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    tabs = {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Contents") }
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Highlights") }
                        )
                    }
                )

                when (selectedTab) {
                    0 ->
                        Contents(
                            modifier = Modifier.fillMaxWidth(),
                            publication = publication,
                            onItemActivated = onTocItemActivated
                        )
                    else ->
                        Highlights(
                            modifier = Modifier.fillMaxWidth(),
                            highlights = highlights,
                            onItemActivated = onHighlightActivated
                        )
                }
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
        title = { Text("Outline") },
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
private fun Highlights(
    modifier: Modifier = Modifier,
    highlights: List<Highlight>,
    onItemActivated: (Highlight) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        itemsIndexed(highlights) { index, highlight ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemActivated(highlight) },
                headlineContent = {
                    Text("Highlight $index")
                }
            )
        }
    }
}

@Composable
private fun Contents(
    modifier: Modifier = Modifier,
    publication: Publication,
    onItemActivated: (Url) -> Unit,
) {
    val items = publication.tableOfContents
        .flatMap { it.toTocItems(publication) }

    val scrollState = rememberScrollState()

    Contents(
        modifier = modifier.verticalScroll(scrollState),
        items = items,
        onClick = onItemActivated
    )
}

private data class TocItem(
    val title: String,
    val url: Url,
    val depth: Int,
)

private fun Link.toTocItems(
    publication: Publication,
    depth: Int = 0,
): List<TocItem> {
    val title = title ?: url().filename ?: ""

    return buildList {
        add(TocItem(title, url(), depth))
        for (child in children) {
            addAll(child.toTocItems(publication, depth + 1))
        }
    }
}

@Composable
private fun Contents(
    modifier: Modifier = Modifier,
    items: List<TocItem>,
    onClick: (Url) -> Unit,
    depth: Int = 0,
) {
    Column(
        modifier = modifier
    ) {
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
private fun TocItem(
    modifier: Modifier = Modifier,
    item: TocItem,
    onClick: (Url) -> Unit,
    depth: Int = 0,
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.title,
            )
        },
        modifier = modifier
            .clickable { onClick(item.url) }
            .padding(start = 24.dp * depth)
    )
}
