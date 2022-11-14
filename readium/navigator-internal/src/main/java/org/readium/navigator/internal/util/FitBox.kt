package org.readium.navigator.internal.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.ceil

@Composable
fun FitBox(
    parentSize: Size,
    contentScale: ContentScale,
    scaleSetting: Float,
    itemSize: Size,
    content: @Composable BoxScope.() -> Unit
) {
    val initialItemScale = remember {
        contentScale.computeScaleFactor(itemSize, parentSize).scaleX
    }

    val itemScale = initialItemScale * scaleSetting

    val width =  with(LocalDensity.current) {
        ceil(itemSize.width * itemScale).toDp()
    }

    val height =  with(LocalDensity.current) {
        ceil(itemSize.height * itemScale).toDp()
    }

    Box(
        modifier = Modifier.requiredSize(width, height),
        content = content
    )
}
