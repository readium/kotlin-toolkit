# Fixed-layouts’ Reading Systems Requirements

[Implementers’ doc] [Authors’ doc]

Source: [EPUB Content Documents 3.2](https://w3c.github.io/publ-epub-revision/epub32/spec/epub-contentdocs.html#sec-fixed-layouts)

There’s a few criteria Reading Systems must meet in order to be conformant: 

- it should allocate the full content display area (i.e. as much of the available Viewport area as possible) for the document;
- it must use the dimensions expressed in the `viewport` meta tag to render XHTML Content Documents;
- it must use the dimensions expressed in the `viewbox`, and/or `x`, `y`, `width` and `height` attributes in SVG to render SVG Content Documents.

## Styling 

You should not inject additional content such as borders, margins, headers or footers into the viewport or the appplication area surrounding the viewport.

## Scaling

### XHTML (viewport meta)

When the aspect ratio does not match the aspect ratio of the Reading System, you may position the containing block inside the area to accommodate the user interface (letter-boxing).

Content positioned outside the containing block will not be visible.

### SVG

SVG is a little bit more complex as there are different options to take into account.

#### Viewbox attribute

If the SVG only has a `viewbox` attribute, the coordinate system it defines is mapped to the viewport, keeping the aspect ratio.

#### Sizing and positioning attributes

If the pixel values defined by `x`, `y`, `height` and `width` exceed the viewport’s pixel values, the graphic must not be rescaled (it must be clipped on the viewport boundaries).

#### Both

The coordinate system defined by the `viewbox` is mapped to the viewport, keeping the aspect ratio.

## Notes

The `rendition:spread portrait` and `rendition:viewport` [are deprecated in EPUB 3.2](https://w3c.github.io/publ-epub-revision/epub32/spec/epub-packages.html#sec-package-metadata-fxl).

Consequently, the `rendition:spread-portrait` spine override is deprecated as well.