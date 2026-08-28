# Extracting the content of a publication

:warning: The described feature is still experimental and the implementation incomplete.

Many high-level features require access to the raw content (text, media, etc.) of a publication, such as:

* Text-to-speech
* Accessibility reader
* Basic search
* Full-text search indexing
* Image or audio indexes

The `ContentService` provides a way to iterate through a publication's content, extracted as semantic elements.

First, request the publication's `Content`, starting from a given `Locator`. If the locator is missing, the `Content` will be extracted from the beginning of the publication.

```kotlin
val content = publication.content(startLocator)
if (content == null) {
    // Abort as the content cannot be extracted
}
```

## Extracting the raw text content

Getting the whole raw text of a publication is such a common use case that a helper is available on `Content`:

```kotlin
val wholeText = content.text()
```

This is an expensive operation, proceed with caution and cache the result if you need to reuse it.

## Iterating through the content

The individual `Content` elements can be iterated through with a regular `for` loop:

```kotlin
for (element in content) {
    // Process element
}
```

Alternatively, you can get the whole list of elements with `content.elements()`, or use the lower level APIs to iterate the content manually:

```kotlin
val iterator = content.iterator()
while (iterator.hasNext()) {
    val element = iterator.next()
}
```

Some `Content` implementations support bidirectional iterations. To iterate backwards, use:

```kotlin
while (iterator.hasPrevious()) {
    val element = iterator.hasPrevious()
}
```

## Processing the elements

The `Content` iterator yields `Content.Element` objects representing a single semantic portion of the publication, such as a heading, a paragraph or an embedded image.

Every element has a `locator` property targeting it in the publication. You can use the locator, for example, to navigate to the element or to draw a `Decoration` on top of it.

```kotlin
navigator.go(element.locator)
```

### Types of elements

Depending on the actual implementation of `Content.Element`, more properties are available to access the actual data. The toolkit ships with a number of default implementations for common types of elements.

#### Embedded media

The `Content.EmbeddedElement` interface is implemented by any element referencing an external resource. It contains an `embeddedLink` property you can use to get the actual content of the resource.

```kotlin
if (element is Content.EmbeddedElement) {
    val bytes = publication
        .get(element.embeddedLink)
        .read().getOrThrow()
}
```

Here are the default available implementations:

* `Content.AudioElement` - audio clips
* `Content.VideoElement` - video clips
* `Content.ImageElement` - bitmap images, with the additional property:
    * `caption: String?` - figure caption, when available

#### Text

##### Textual elements

The `Content.TextualElement` interface is implemented by any element which can be represented as human-readable text. This is useful when you want to extract the text content of a publication without caring for each individual type of elements.

```kotlin
val wholeText = publication.content()
    .elements()
    .filterIsInstance<Content.TextualElement>()
    .mapNotNull { it.text }
    .joinToString(separator = "\n")
```

##### Text elements

Actual text elements are instances of `Content.TextElement`, which represent a single block of text such as a heading, a paragraph or a list item. It is comprised of a `role` and a list of `segments`.

The `role` is the nature of the text element in the document. For example a heading, body, footnote or a quote. It can be used to reconstruct part of the structure of the original document.

A text element is composed of individual segments with their own `locator` and `attributes`. They are useful to associate attributes with a portion of a text element. For example, given the HTML paragraph:

```html
<p>It is pronounced <span lang="fr">croissant</span>.</p>
```

The following `TextElement` will be produced:

```kotlin
TextElement(
    segments = listOf(
        Segment(text = "It is pronounced "),
        Segment(text = "croissant", attributes = mapOf(LANGUAGE to "fr")),
        Segment(text = ".")
    )
)
```

If you are not interested in the segment attributes, you can also use `element.text` to get the concatenated raw text.

### Element attributes

All types of `Content.Element` can have associated attributes. Custom `ContentService` implementations can use this as an extensibility point.

## Use cases

### An index of all images embedded in the publication

This example extracts all the embedded images in the publication and displays them in a Jetpack Compose list. Clicking on an image jumps to its location in the publication.

```kotlin
data class Item(
    val locator: Locator,
    val text: String?,
    val bitmap: ImageBitmap?
)

var images by remember {
    mutableStateOf<List<Item>>(emptyList())
}

LaunchedEffect(publication) {
    publication.content()?.let { content ->
        images = content.elements()
            .filterIsInstance<Content.ImageElement>()
            .map { element ->
                Item(
                    locator = element.locator,
                    text = element.caption,
                    bitmap = publication.get(element.embeddedLink)
                        .readAsBitmap().getOrNull()?.asImageBitmap()
                )
            }
    }
}

LazyColumn {
    items(images) { item ->
        if (item.bitmap != null) {
            Column(
                modifier = Modifier.clickable {
                    navigator.go(item.locator)
                }
            ) {
                Image(bitmap = item.bitmap, contentDescription = item.text)
                Text(item.caption ?: "No caption")
            }
        }
    }
}
```

## References

* [Content Iterator proposal](https://github.com/readium/architecture/pull/177)
