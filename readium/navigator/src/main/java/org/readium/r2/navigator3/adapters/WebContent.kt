package org.readium.r2.navigator3

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebContent(url: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                isScrollContainer = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                loadUrl(url)
            }
        },
        update = { webview ->


        }
    )
}