@file:OptIn(ExperimentalMaterial3Api::class)

package org.readium.demo.navigator.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.readium.demo.navigator.R

@Composable
fun RenditionTopBar(
    modifier: Modifier,
    visible: Boolean,
    onPreferencesActivated: () -> Unit,
    onOutlineActivated: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TopAppBar(
            title = { },
            actions = {
                IconButton(
                    onClick = onPreferencesActivated
                ) {
                    Icon(
                        painterResource(R.drawable.ic_preferences_24),
                        contentDescription = "Preferences",
                    )
                }
                IconButton(
                    onClick = onOutlineActivated
                ) {
                    Icon(
                        painterResource(R.drawable.ic_outline_24),
                        contentDescription = "Outline"
                    )
                }
            }
        )
    }
}
