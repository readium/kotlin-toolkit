package org.readium.r2.navigator2.view

import android.graphics.Bitmap
import android.util.Size
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.navigator2.view.layout.DefaultLayoutPolicy
import org.readium.r2.navigator2.view.layout.LayoutPolicy
import java.net.URL

data class NavigatorConfiguration(
    val baseUrl: URL? = null,
    val errorBitmap: (Size) -> Bitmap = { BitmapFactory.createBitmap(it.width, it.height, android.graphics.Color.RED) },
    val emptyBitmap: (Size) -> Bitmap = { BitmapFactory.createBitmap(it.width, it.height, android.graphics.Color.BLACK) },
    val ignoreDefaultSpreadAdapters: Boolean = false,
    val layoutPolicy: LayoutPolicy = DefaultLayoutPolicy
)
