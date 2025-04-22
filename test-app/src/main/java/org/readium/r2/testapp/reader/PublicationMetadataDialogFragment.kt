/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import java.text.SimpleDateFormat
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide
import org.readium.r2.shared.publication.Accessibility
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Instant
import org.readium.r2.testapp.utils.compose.ComposeBottomSheetDialogFragment

class PublicationMetadataDialogFragment : ComposeBottomSheetDialogFragment(
    isScrollable = true
) {
    private val viewModel: ReaderViewModel by activityViewModels()

    @Composable
    override fun Content() {
        PublicationMetadata(
            publication = viewModel.publication,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
}

@Composable
fun PublicationMetadata(publication: Publication, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Metadata",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PublicationMetadata(
                metadata = publication.metadata,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            HorizontalDivider()

            AccessibilityMetadata(
                guide = AccessibilityMetadataDisplayGuide(publication),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PublicationMetadata(metadata: Metadata, modifier: Modifier = Modifier) {
    Section("Publication", modifier) {
        metadata.title?.let { title ->
            MetadataRow("Title", title)
        }

        metadata.identifier?.let { id ->
            MetadataRow("Identifier", id)
        }

        MetadataRow(
            singleTitle = "Author",
            pluralTitle = "Authors",
            content = metadata.authors.map { it.name }
        )

        MetadataRow(
            singleTitle = "Publisher",
            pluralTitle = "Publishers",
            content = metadata.publishers.map { it.name }
        )

        metadata.published?.let { published ->
            MetadataRow("Publication date", SimpleDateFormat.getDateInstance().format(published.toJavaDate()))
        }
    }
}

@Composable
private fun AccessibilityMetadata(guide: AccessibilityMetadataDisplayGuide, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var alwaysDisplayFields: Boolean by rememberSaveable { mutableStateOf(false) }
    var showDescriptiveStatements: Boolean by rememberSaveable { mutableStateOf(false) }

    Section("Accessibility Claims", modifier) {
        Column {
            Switch(
                label = "Show fields with no metadata",
                checked = alwaysDisplayFields,
                onCheckedChange = { alwaysDisplayFields = it }
            )

            Switch(
                label = "Show descriptive statements",
                checked = showDescriptiveStatements,
                onCheckedChange = { showDescriptiveStatements = it }
            )
        }

        for (field in guide.fields) {
            if (field.statements.isEmpty() || !(alwaysDisplayFields || field.shouldDisplay)) {
                continue
            }

            MetadataRow(title = field.localizedTitle(context)) {
                for (statement in field.statements) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", modifier = Modifier.alignByBaseline())

                        Text(
                            text = statement.localizedString(context, descriptive = showDescriptiveStatements),
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Switch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp)
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        content()
    }
}

@Composable
private fun MetadataRow(singleTitle: String, pluralTitle: String, content: List<String>, modifier: Modifier = Modifier) {
    if (content.isEmpty()) {
        return
    }

    if (content.size > 1) {
        MetadataRow(title = pluralTitle, content = content.joinToString(separator = ", "), modifier)
    } else {
        MetadataRow(title = singleTitle, content = content[0], modifier)
    }
}

@Composable
private fun MetadataRow(title: String, content: String, modifier: Modifier = Modifier) {
    MetadataRow(title = title, modifier = modifier) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MetadataRow(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium
        )

        content()
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewPublicationMetadata() {
    PublicationMetadata(
        publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    localizedTitle = LocalizedString("Alice's Adventures in Wonderland"),
                    identifier = "urn:isbn:1503222683",
                    authors = listOf(
                        Contributor(name = "Lewis Carroll"),
                        Contributor(name = "Other Author"),
                    ),
                    publishers = listOf(
                        Contributor(name = "Macmillan")
                    ),
                    published = Instant.now(),
                    accessibility = Accessibility(
                        conformsTo = setOf(Accessibility.Profile.EPUB_A11Y_10_WCAG_20_AA),
                        accessModes = setOf(Accessibility.AccessMode.TEXTUAL),
                        features = setOf(Accessibility.Feature.DESCRIBED_MATH, Accessibility.Feature.LARGE_PRINT),
                        hazards = setOf(Accessibility.Hazard.FLASHING, Accessibility.Hazard.NONE)
                    )
                )
            )
        )
    )
}
