package org.readium.r2.testapp.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.testapp.MainViewModel
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.shared.views.PublicationCoverItem

@Composable
fun BookshelfScreen(
    mainViewModel: MainViewModel = viewModel(),
    viewModel: BookshelfViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    val appStoragePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importPublicationFromStorage(it) }
    }

    val sharedStoragePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importPublicationFromStorage(it) }
    }

    LaunchedEffect(Unit) {
        mainViewModel.updateTopBar(
            title = context.getString(R.string.title_bookshelf),
            actions = {}
        )
    }

    LaunchedEffect(viewModel.channel) {
        viewModel.channel.receive(lifecycleOwner = lifecycleOwner) { event ->
            when (event) {
                is BookshelfViewModel.Event.LaunchReader -> {
                    val intent = ReaderActivityContract().createIntent(context, event.arguments)
                    context.startActivity(intent)
                }
                is BookshelfViewModel.Event.OpenPublicationError -> {
//                    event.error.toUserError().show(requireActivity())
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddBookDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_book)
                )
            }
        }
    ) { padding ->
        if (uiState.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {

            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = padding,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                items(uiState.books) { book ->
                    PublicationCoverItem(
                        imageUrl = book.cover,
                        title = book.title!!,
                        onClick = { viewModel.openPublication(book.id!!) },
                        onLongClick = { bookToDelete = book }
                    )
                }
            }
        }
    }

    if (showAddBookDialog) {
        AddBookDialog(
            onDismiss = { showAddBookDialog = false },
            onConfirm = { selectedIndex ->
                showAddBookDialog = false
                when (selectedIndex) {
                    0 -> appStoragePickerLauncher.launch("*/*")
                    1 -> sharedStoragePickerLauncher.launch(arrayOf("*/*"))
                    2 -> showAddUrlDialog = true
                }
            }
        )
    }

    if (showAddUrlDialog) {
        AddUrlDialog(
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url ->
                val absoluteUrl = AbsoluteUrl(url)
                if (absoluteUrl != null) {
                    viewModel.addPublicationFromWeb(absoluteUrl)
                }
                showAddUrlDialog = false
            }
        )
    }

    bookToDelete?.let { book ->
        DeleteConfirmationDialog(
            bookTitle = book.title!!,
            onConfirm = { viewModel.deletePublication(book) },
            onDismiss = { bookToDelete = null }
        )
    }
}

@Composable
private fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (selectedIndex: Int) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = stringArrayResource(id = R.array.documentSelectorArray)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_book)) },
        text = {
            Column {
                options.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (index == selectedIndex),
                                onClick = { selectedIndex = index }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == selectedIndex),
                            onClick = { selectedIndex = index }
                        )
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIndex) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun AddUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.add_book)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(text = stringResource(id = R.string.enter_url)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url) },
                enabled = url.isNotBlank()
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.confirm_delete_book_title)) },
        text = { Text(text = stringResource(R.string.confirm_delete_book_text, bookTitle)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
