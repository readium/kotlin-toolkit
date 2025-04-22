# Accessibility

Some publications declare their accessibility features and limitations as metadata which broadly mirror the [EPUB Accessibility](https://www.w3.org/TR/epub-a11y-11) specification.

```kotlin
val accessibility = publication.metadata.accessibility ?: Accessibility()

if (accessibility.accessModesSufficient.contains(setOf(PrimaryAccessMode.TEXTUAL))) {
    // This publication can be read aloud with a text-to-speech engine.
}

if (accessibility.features.contains(Feature.DISPLAY_TRANSFORMABILITY)) {
    // The text and layout of this publication can be customized.
}
```

## Displaying accessibility metadata

While the [RWPM Accessibility models](https://readium.org/webpub-manifest/contexts/default/#accessibility-metadata) provide valuable information, they may be too complex and detailed to present to the user as it is. To simplify the presentation of this metadata to users, the Readium toolkit implements the [Accessibility Metadata Display Guide](https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/) specification, developed by the W3C.

### How is the display guide structured?

The guide contains a list of fields that can be displayed as sections in your user interface. Each field has a list of related statements. For example, the `WaysOfReading` field provides information about whether the user can customize the text and layout (`visualAdjustments`) or if it is readable with text-to-speech or dynamic braille (`nonvisualReading`).

```kotlin
val guide = AccessibilityMetadataDisplayGuide(publication)

when (guide.waysOfReading.visualAdjustments) {
    VisualAdjustments.MODIFIABLE -> {
        // The text and layout of the publication can be customized.
    }
    VisualAdjustments.UNMODIFIABLE -> {
        // The text and layout cannot be modified.
    }
    VisualAdjustments.UNKNOWN -> {
        // No metadata provided
    }
}
```

### Localized accessibility statements

While you are free to manually inspect the accessibility fields, the toolkit offers an API to automatically convert them into a list of localized statements (or claims) for direct display to the user.

Each statement has a *compact* and *descriptive* variant. The *descriptive* string is longer and provides more details about the claim.

For example:
- **Compact**: Prerecorded audio clips
- **Descriptive**: Prerecorded audio clips are embedded in the content

```kotlin
for (statement in guide.waysOfReading.statements) {
    print(statement.localizedString(context, descriptive = false))
}
```

If translations are missing in your language, **you are encouraged to submit a contribution [to the official W3C repository](https://github.com/w3c/publ-a11y-display-guide-localizations)**.

### Displaying all the recommended fields

If you wish to display all the accessibility fields as recommended in the official specification, you can iterate over all the fields and their statements in the guide.

The `shouldDisplay` property indicates whether the field does not have any meaningful statement. In which case, you may skip it in your user interface.

```kotlin
for (field in guide.fields) {
    if (!field.shouldDisplay) {
        continue
    }

    print("Section: ${field.localizedTitle(context)}")

    for (statement in field.statements) {
        print(statement.localizedString(context, descriptive = false))
    }
}
```

### Sample implementation in Jetpack Compose

```kotlin
@Composable
fun AccessibilityMetadata(guide: AccessibilityMetadataDisplayGuide, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showDescriptiveStatements: Boolean by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Accessibility Claims",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Text("Show descriptive statements")
            Switch(
                checked = showDescriptiveStatements,
                onCheckedChange = { showDescriptiveStatements = it }
            )
        }

        for (field in guide.fields) {
            if (!field.shouldDisplay) {
                continue
            }

            Text(
                text = field.localizedTitle(context),
                style = MaterialTheme.typography.labelMedium
            )

            for (statement in field.statements) {
                Text(
                    text = statement.localizedString(context, descriptive = showDescriptiveStatements),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```