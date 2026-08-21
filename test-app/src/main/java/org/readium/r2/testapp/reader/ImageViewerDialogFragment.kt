/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.use
import org.readium.r2.testapp.utils.compose.AppTheme

/**
 * A simple full-screen dialog displaying a tapped image and its caption.
 *
 * This demonstrates the experimental [org.readium.r2.navigator.input.TapEvent.targetElement] API.
 */
class ImageViewerDialogFragment : DialogFragment() {

    private val viewModel: ReaderViewModel by activityViewModels()

    private val href: Url by lazy {
        val href = requireArguments().getString(ARG_HREF)
        checkNotNull(href?.let { Url(it) }) { "Missing or invalid image href" }
    }

    private val caption: String? get() = arguments?.getString(ARG_CAPTION)

    private val accessibilityLabel: String? get() = arguments?.getString(ARG_ACCESSIBILITY_LABEL)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppTheme {
                    ImageViewer(
                        caption = caption,
                        accessibilityLabel = accessibilityLabel,
                        loadBitmap = ::loadBitmap,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }

    override fun onStart() {
        super.onStart()
        // Make the dialog full-screen.
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private suspend fun loadBitmap(): Bitmap? =
        withContext(Dispatchers.IO) {
            val resource = viewModel.publication.get(href)
                ?: return@withContext null
            val bytes = resource.use { it.read() }
                .getOrElse { return@withContext null }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

    companion object {
        const val TAG = "ImageViewerDialogFragment"

        private const val ARG_HREF = "href"
        private const val ARG_CAPTION = "caption"
        private const val ARG_ACCESSIBILITY_LABEL = "accessibilityLabel"

        @OptIn(ExperimentalReadiumApi::class)
        fun newInstance(image: Content.ImageElement): ImageViewerDialogFragment =
            ImageViewerDialogFragment().apply {
                arguments = bundleOf(
                    ARG_HREF to image.embeddedLink.url().toString(),
                    ARG_CAPTION to (image.caption ?: image.text),
                    ARG_ACCESSIBILITY_LABEL to image.accessibilityLabel
                )
            }
    }
}

@Composable
private fun ImageViewer(
    caption: String?,
    accessibilityLabel: String?,
    loadBitmap: suspend () -> Bitmap?,
    onDismiss: () -> Unit,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null) {
        val loaded = loadBitmap()
        if (loaded == null) onDismiss() else value = loaded
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = accessibilityLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }

            caption?.let {
                Text(
                    text = it,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}
