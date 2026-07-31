# EPUB Image Preview

This guide explains how to detect when a user taps an image in an EPUB publication to present it in a dedicated view, using the experimental `TapEvent.targetElement` API.

> [!IMPORTANT]
> `targetElement` is an experimental API gated behind `@ExperimentalReadiumApi`. You must opt in at the call site and accept that the API may change in a future release.

## Detecting image taps

The EPUB navigator populates `TapEvent.targetElement` when it recognizes the content element under the pointer. The `TargetElement` value exposes two properties:

* `content` – the `Content.Element` under the pointer (e.g. `Content.ImageElement`, `Content.SvgElement`)
* `frame` – the element's on-screen frame (`RectF`) relative to the navigator's view, in device pixels

Register an `InputListener` on the navigator and downcast `content` to the specific type you want to handle:

```kotlin
@OptIn(ExperimentalReadiumApi::class)
navigator.addInputListener(object : InputListener {
    override fun onTap(event: TapEvent): Boolean {
        val image = event.targetElement?.content as? Content.ImageElement
            ?: return false

        // The user tapped an image – handle it here.
        showImagePreview(image)
        return true
    }
})
```

Returning `true` consumes the event, preventing other input listeners from handling the same tap. Bind this listener *before* listeners that react to generic taps (such as toggling the navigation bar) so it takes precedence.

> [!NOTE]
> `targetElement` is not populated for images wrapped in an interactive element, such as `<a><img/></a>`. The navigator treats the tap as an activation of the interactive element (e.g. following the hyperlink) and does not forward it to the input listeners.

## Working with `Content.ImageElement`

`Content.ImageElement` describes an embedded image (`<img>`, or `<svg>` referencing an external resource) and provides the following properties:

| Property             | Type          | Description                                                                             |
|----------------------|---------------|-----------------------------------------------------------------------------------------|
| `embeddedLink`       | **`Link`**    | Points to the image resource in the publication, ready to load with `publication.get()` |
| `caption`            | **`String?`** | Caption extracted from `alt`, `title`, a surrounding `<figcaption>`, etc.               |
| `accessibilityLabel` | **`String?`** | Accessibility label extracted from the `aria-label` attribute                           |

The `text` property returns the caption when available, otherwise the accessibility label — a convenient fallback when you need a single display string.

To load and display the image, read its resource from the publication:

```kotlin
val bytes = publication.get(image.embeddedLink)
    ?.use { it.read() }
    ?.getOrNull()
```

`Content.SvgElement` follows a similar shape for inline SVG (`<svg>`), but exposes a `svg: String` property holding the raw SVG source instead of `embeddedLink`.

