package org.readium.navigator.demo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.readium.navigator.web.NavigatorView
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.toAbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
class DemoActivity : ComponentActivity() {

    private val viewModel: DemoViewModel by viewModels()

    private val sharedStoragePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val url = requireNotNull(it.toAbsoluteUrl())
                viewModel.open(url)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->

                    Box(
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        val state = viewModel.state.collectAsState()

                        when (val stateNow = state.value) {
                            DemoViewModel.State.BookSelection -> {
                                sharedStoragePickerLauncher.launch(arrayOf("*/*"))
                            }
                            is DemoViewModel.State.Error -> {
                                LaunchedEffect(stateNow.error) {
                                    snackbarHostState.showSnackbar(stateNow.error.message)
                                    viewModel.acknowledgeError()
                                }
                            }
                            DemoViewModel.State.Loading -> {
                                // Display nothing
                            }
                            is DemoViewModel.State.Reader -> {
                                NavigatorView(stateNow.state)
                            }
                        }
                    }
                }
            }
        }
    }
}
