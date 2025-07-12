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
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

@Composable
fun Outline(
    modifier: Modifier = Modifier,
    publication: Publication,
    onBackActivated: () -> Unit,
    onTocItemActivated: (Url) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar(onBackActivated) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding),
                propagateMinConstraints = true
            ) {
                Contents(
                    modifier = Modifier.fillMaxSize(),
                    publication = publication,
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
