package org.readium.r2.testapp.reader.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.presentation.PresentationController
import org.readium.r2.navigator.presentation.PresentationKey
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.testapp.utils.compose.ToggleButtonGroup

@Composable
@OptIn(ExperimentalPresentation::class)
fun FixedSettingsView(presentation: PresentationController) {
    val settings by presentation.settings.collectAsState()
    FixedSettingsView(
        settings = settings,
        commit = { presentation.commit(it) }
    )
}

@Composable
@OptIn(ExperimentalPresentation::class)
private fun FixedSettingsView(settings: PresentationController.Settings, commit: (PresentationController.(PresentationController.Settings) -> Unit) -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "User settings",
            modifier = Modifier.padding(bottom = 16.dp),
            style = MaterialTheme.typography.subtitle1,
        )

        settings.readingProgression?.let { readingProgression ->
            val values = readingProgression.supportedValues ?: return@let

            Text(
                text = "Reading progression",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.subtitle2,
            )
            ToggleButtonGroup(
                options = values,
                selectedOption = readingProgression.value ?: ReadingProgression.LTR,
                onSelectOption = { value ->
                    commit {
                        set(readingProgression, value)
                    }
                }) { option ->
                Icon(
                    imageVector = when (option) {
                        ReadingProgression.LTR -> Icons.Default.KeyboardArrowRight
                        ReadingProgression.RTL -> Icons.Default.KeyboardArrowLeft
                        ReadingProgression.TTB -> Icons.Default.KeyboardArrowDown
                        ReadingProgression.BTT -> Icons.Default.KeyboardArrowUp
                        ReadingProgression.AUTO -> Icons.Default.Clear
                    },
                    contentDescription = readingProgression.labelForValue(context, option)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        settings.overflow?.let { overflow ->
            val values = overflow.supportedValues ?: return@let

            Text(
                text = "Overflow",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.subtitle2,
            )
            ToggleButtonGroup(
                options = values,
                selectedOption = overflow.value ?: Overflow.AUTO,
                onSelectOption = { value ->
                    commit {
                        set(overflow, value)
                    }
                }) { option ->
                Text(overflow.labelForValue(context, option))
            }
        }
    }
}
    }
}

@Composable
@Preview(showBackground = true)
@OptIn(ExperimentalPresentation::class)
fun PreviewFixedSettingsView() {
    FixedSettingsView(settings = PresentationController.Settings(
        PresentationKey.READING_PROGRESSION to PresentationController.EnumSetting(
            ReadingProgression,
            key = PresentationKey.READING_PROGRESSION,
            value = ReadingProgression.TTB,
            supportedValues = listOf(
                ReadingProgression.LTR, ReadingProgression.RTL,
                ReadingProgression.TTB, ReadingProgression.BTT
            ),
            isActive = true,
            isAvailable = true,
            labelForValue = { _, v -> v.name }
        ).stringSetting
    ), commit = {})
}
