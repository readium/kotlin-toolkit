package org.readium.r2.navigator3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator3.lazylist.ZoomableLazyList
import org.readium.r2.navigator3.lazylist.Direction
import org.readium.r2.navigator3.lazylist.rememberZoomableLazyListState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression

@Composable
fun Navigator(
    publication: Publication,
    baseUrl: String,
    links: List<Link> = publication.readingOrder,
    modifier: Modifier = Modifier
) {
    ZoomableLazyList(
        direction = when (publication.metadata.readingProgression) {
            ReadingProgression.RTL -> Direction.RTL
            ReadingProgression.LTR -> Direction.LTR
            ReadingProgression.BTT -> Direction.BTT
            ReadingProgression.TTB -> Direction.TTB
            ReadingProgression.AUTO -> Direction.TTB
        },
        modifier = modifier.fillMaxSize(),
        state = rememberZoomableLazyListState()
    ) {
        items(links) { item ->
            Column(
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
            ){
                Spacer(modifier = Modifier.padding(1.dp))
                //TestContent()
                when {
                    item.mediaType.isBitmap ->
                        ImageContent(publication, item, )
                    item.mediaType.isHtml ->
                        WebContent(item.withBaseUrl(baseUrl).href)
                }
            }
        }
    }
}

@Composable
fun TestContent() {
    Row {
        Box(
            modifier = Modifier
                .height(100.dp)
                .width(300.dp)
                .clip(RectangleShape)
                .background(Color.Red)
        )
        Box(
            modifier = Modifier
                .height(100.dp)
                .width(300.dp)
                .clip(RectangleShape)
                .background(Color.Yellow)
        )
    }
}