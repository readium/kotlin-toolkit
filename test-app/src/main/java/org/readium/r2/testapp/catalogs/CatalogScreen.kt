package org.readium.r2.testapp.catalogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.opds.Group
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.testapp.MainViewModel
import org.readium.r2.testapp.Screen
import org.readium.r2.testapp.data.model.Catalog
import org.readium.r2.testapp.shared.views.PublicationCoverItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    catalog: Catalog,
    mainViewModel: MainViewModel,
    catalogViewModel: CatalogViewModel = viewModel(),
    navController: NavController,
    onFacetClick: (facet: Facet) -> Unit
) {
    val state by catalogViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(catalog) {
        catalogViewModel.parseCatalog(catalog)
    }

    LaunchedEffect(state) {
        val feed = (state as? CatalogUiState.Success)?.parseData?.feed
        mainViewModel.updateTopBar(
            title = catalog.title,
            actions = {
                if (!feed?.facets.isNullOrEmpty()) {
                    FacetMenu(
                        facets = feed.facets,
                        onFacetClick = { link ->

                        }
                    )
                }
            }
        )
    }

    val navigateToPublication = { publication: Publication ->
        catalogViewModel.setPublication(publication)
        navController.navigate("publication")
    }

    when (val currentState = state) {
        is CatalogUiState.Loading ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        is CatalogUiState.Success -> {
            val feed = currentState.parseData.feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (feed?.navigation?.isNotEmpty() == true) {
                    item {
                        NavigationSection(
                            links = feed.navigation,
                            onNavigationLinkClick = { link ->
                                val newCatalog = Catalog(
                                    href = link.href.toString(),
                                    title = link.title!!,
                                    type = catalog.type,
                                    id = null
                                )
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "catalog",
                                    newCatalog
                                )
                                navController.navigate(Screen.CatalogDetail.route)
                            }
                        )
                    }
                }

                if (feed?.publications?.isNotEmpty() == true) {
                    item {
                        PublicationGrid(
                            publications = feed.publications,
                            onPublicationClick = navigateToPublication
                        )
                    }
                }

                items(feed?.groups ?: emptyList()) { group ->
                    GroupRow(
                        group = group,
                        onPublicationClick = navigateToPublication,
                        onMoreClick = {
                            group.links.firstOrNull()?.let { link ->
                                val newCatalog = Catalog(
                                    href = link.href.toString(),
                                    title = group.title,
                                    type = catalog.type,
                                    id = null
                                )
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "catalog",
                                    newCatalog
                                )
                                navController.navigate(Screen.CatalogDetail.route)
                            }
                        }
                    )
                }
            }
        }

        is CatalogUiState.Error -> {

        }
    }
}

@Composable
private fun FacetMenu(facets: List<Facet>, onFacetClick: (Facet) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            facets.forEach { facet ->
                DropdownMenuItem(
                    text = { Text(facet.title) },
                    onClick = {
                        onFacetClick(facet)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NavigationSection(links: List<Link>, onNavigationLinkClick: (Link) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { link ->
            Button(
                onClick = { onNavigationLinkClick(link) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(link.title ?: "")
            }
        }
    }
}

@Composable
private fun PublicationGrid(
    publications: List<Publication>,
    onPublicationClick: (Publication) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(publications) { publication ->
            val imageUrl = publication.linkWithRel("http://opds-spec.org/image/thumbnail")?.href?.toString()
                ?: publication.images.firstOrNull()?.href?.toString()
            PublicationCoverItem(
                imageUrl = imageUrl,
                title = publication.metadata.title!!,
                onClick = { onPublicationClick(publication) }
            )
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    onPublicationClick: (Publication) -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (group.links.isNotEmpty()) {
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = "More")
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(group.publications) { publication ->
                val imageUrl = publication.linkWithRel("http://opds-spec.org/image/thumbnail")?.href?.toString()
                    ?: publication.images.firstOrNull()?.href?.toString()
                PublicationCoverItem(
                    imageUrl = imageUrl,
                    title = publication.metadata.title!!,
                    onClick = { onPublicationClick(publication) }
                )
            }
        }
    }
}
