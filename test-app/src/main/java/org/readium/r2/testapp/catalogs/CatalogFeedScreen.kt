package org.readium.r2.testapp.catalogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.readium.r2.testapp.MainViewModel
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Catalog

@Composable
fun CatalogFeedScreen(
    catalogViewModel: CatalogFeedListViewModel = viewModel(),
    mainViewModel: MainViewModel,
    navController: NavController,
) {
    val title = stringResource(R.string.title_catalogs)

    LaunchedEffect(Unit) {
        mainViewModel.updateTopBar(title = title)
    }

    val catalogs by catalogViewModel.catalogs.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddCatalogDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(catalogs) { catalog ->
                CatalogItem(
                    catalog = catalog,
                    onDelete = { catalogId ->
                        catalogViewModel.deleteCatalog(catalogId)
                    },
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "catalog",
                            catalog
                        )
                        navController.navigate("catalog_detail")
                    }
                )
                HorizontalDivider()
            }
        }

        FloatingActionButton(
            onClick = { showAddCatalogDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_catalog)
            )
        }
    }

    if (showAddCatalogDialog) {
        AddCatalogDialog(
            onDismiss = { showAddCatalogDialog = false },
            onConfirm = { title, url ->
                catalogViewModel.parseCatalog(url, title)
                showAddCatalogDialog = false
            }
        )
    }
}

@Composable
private fun CatalogItem(
    catalog: Catalog,
    onDelete: (id: Long) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = catalog.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                catalog.id?.let { id -> onDelete(id) }
            },
            enabled = (catalog.id != null)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete)
            )
        }
    }
}

@Composable
private fun AddCatalogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.add_catalog)) },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(R.string.enter_title)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(text = stringResource(R.string.enter_url)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, url) },
                enabled = title.isNotBlank() && url.isNotBlank()
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
