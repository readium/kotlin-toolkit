# EPUB Compatibility

[Implementers’ doc] [Authors’ info]

This document describes a collection of non-standard CSS properties, metadata, and attributes that implementers should take into account to support for compatibility with the *de facto* EPUB ecosystem.

It also lists important standard features that authors are accustomed to or might rely upon in the future.

## Introduction

There exists an increasingly large corpus of ebook content that depends on non-standard CSS properties, metadata and attributes. This document aims to describe the minimal set that Reading Systems might be willing to support for optimal ebook compatibility.

## CSS At-rules

### @page

```
@page
```

The `@page` CSS at-rule is used to modify the page margins in Adobe’s “Legacy RMSDK” (the one managing EPUB 2 files).

Margins applied in this rule will apply to any “screen” in paged views, unlike the ones set for `html` or `body`. It could therefore be used to apply extra margins to the web view or iframe for instance. 

The `@page` at-rule can be accessed via the CSS object model interface `CSSPageRule`.

MDN: https://developer.mozilla.org/en-US/docs/Web/CSS/@page 

### @supports

```
@supports
```

The `@supports` CSS at-rule lets authors specify declarations that depend on a browser’s support for one or more specific CSS features. This is called a *feature query.*

**It is critical for the advancement of modern CSS that implementers try their best at supporting this rule,** especially when pre-processing EPUB files before distribution. It indeed is one of the very few mechanisms allowing authors to do progressive enhancement (layout, typography, etc.), especially as it helps get around older Reading Systems which have no concept of fault tolerance, and will consequently ignore the entire stylesheet if they encounter some of those more modern properties.

The hard truth is that if authors feel this is not well-supported, they won’t use it. It will then be a lot more difficult to make progress happen.

MDN: https://developer.mozilla.org/en-US/docs/Web/CSS/@supports

## Non-standard Kindle Media queries

Since 2012, Amazon has been providing authors with non-standard media queries so that they specific styles can be declared for the old format (Mobi7) and the latest ones (KF8 and above).

```
@media amzn-kf8

@media amzn-mobi
```

**Authors expect the styles in those declarations to be ignored by EPUB Reading Systems.** Do not try to support them.

Docs: [Kindle Publishing Guidelines](https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf)

## Interactive and Fixed-layout EPUB 2

There exists a significant corpus of fixed-layout and/or interactive files which leveraged the display options created by iBooks and Kobo for EPUB 2. 

### Kobo

To declare an EPUB as a FLEPUB (Fixed-Layout EPUB), a file named `com.kobobooks.display-options.xml` must be present inside the EPUB’s `META-INF` folder (where the `container.xml` resides).

The contents of this display options file seem to always be:

```
<?xml version="1.0" encoding="UTF-8"?> 
<display_options>
  <platform name="*">
    <option name="fixed-layout">true</option>
  </platform>
</display_options>
```

As of November 2011, FLEPUB supports embedded fonts, audio, JavaScript (partially), media overlays (smil). It doesn’t support video and SVG.

### Apple

To declare an EPUB 2 file as fixed-layout or interactive, a file named `com.apple.ibooks.display-options.xml` must be present inside the EPUB’s `META-INF` folder (where the `container.xml` resides).

#### Display Options

Supported display options are: 

- `platform`: `*`, `iphone`, or `ipad`
- `specified-fonts`: `true` or `false`
- `interactive`: `true` or `false`
- `fixed-layout`: `true` or `false`
- `open-to-spread`: `true` or `false`
- `orientation-lock`: `landscape-only`, `portrait-only`, or `none` (default).

#### Example

```
<?xml version="1.0" encoding="UTF-8"?>
<display_options>
  <platform name="*">
    <option name="specified-fonts">true</option>
    <option name="interactive">true</option>
    <option name="fixed-layout">true</option>
    <option name="open-to-spread">true</option>
  </platform>
  <platform name="iphone">
    <option name="orientation-lock">landscape-only</option>
  </platform>
</display_options>
```

## Metadata

### Calibre

Calibre is using specific metadata for sorting, series and rating.

This metadata has a `calibre:` prefix.

#### Title sort

```
<meta content="Title of the Book, The" name="calibre:title_sort" />
```

This item allows Reading Systems to sort books in a non-stricly alphabetical way, by moving articles such as “The” and “An” to the end of the string.

#### Series

```
<meta content="Title of the Series" name="calibre:series" />
```

This item indicates the series the publication is part of.

#### Series index

```
<meta content="1.0" name="calibre:series_index" />
```

This item designates the position (index) of the publication in this series.

It can be a floating point number with up to two digits of precision e.g. `1.01`, and zero and negative numbers are allowed.

#### Rating

```
<meta content="10.0" name="calibre:rating" />
```

This item stores the rating (integer) the user has explicitely set for the publication. In Calibre, `0.0` is unrated, `2.0` is one star, and `10.0` is five stars.

### iBooks

iBooks is using specific metadata for embedded fonts, versioning, gaiji, scroll axis, and the EPUB3 files created with iBooks Author. 

This metadata has an `ibooks:` prefix.

Authors must add the following prefix attribute in their `<package>` first:

```
prefix="ibooks: http://vocabulary.itunes.apple.com/rdf/ibooks/vocabulary-extensions-1.0/"
```

Then, they can use the ibooks-specific metadata. 

#### Book versioning

```
<meta property="ibooks:version">1.0.0</meta>
```

When using book versioning, content providers are allowing iBooks customers who have downloaded the old version of the book to be notified that a new version is available for download. If the customer chooses to download it, the new version will replace the prior version on its devices.

Unfortunately, there is no way to retrieve the changelog for each version since this is managed at the iTunes level.

Docs: [iBooks Asset Guide](https://help.apple.com/itc/booksassetguide/?lang=en#/itc88a04b8d6)

#### Embedded fonts

```
<meta property="ibooks:specified-fonts">true</meta>
```

Authors can use the `specified-fonts` attribute to indicate the EPUB files contains embedded fonts, or override a user’s justification and hyphenation preferences. In other words, it preserves the `font-family`, `text-align`, and `hyphens` declarations as specified in the CSS stylesheet, as long as the user does not choose a different typeface when reading the book.

Docs: [iBooks Asset Guide](https://help.apple.com/itc/booksassetguide/?lang=en#/itc2cf4d26eb)

#### Gaiji and image sizing

```
<meta property="ibooks:respect-image-size-class">className</meta>
```

Gaiji are small, inline images that represent characters that are not available in a character or font set. Gaiji are typically used for older symbols or characters in Japanese that have fallen out of use. Authors can define a custom class name for which iBooks will respect an image’s dimensions. Those inline images will then not be altered like other images, for which iBooks may force sizing.

Some authors may have used this metadata to force sizing for specific images, and not only gaiji. Moreover, some might use it to invert specific images like illustrations in night mode, since iBooks does it automatically in order for gaiji to be the same color as text’s.

Docs: [iBooks Asset Guide](https://help.apple.com/itc/booksassetguide/?lang=en#/itca71ad3c33)

#### Scroll axis

```
<meta property="ibooks:scroll-axis">vertical | horizontal | default</meta>
```

iBooks’ scroll theme scrolls vertically for books with horizontal text, and scrolls horizontally for books with vertical text. By default, Japanese and Chinese books will thus scroll horizontally, while all other languages scroll vertically. This meta allows authors to redefine the scroll direction.

Docs: [iBooks Asset Guide](https://help.apple.com/itc/booksassetguide/#/itccf6b30e09)

#### iBooks Author EPUB3 output

```
<meta property="ibooks:format">RMT</meta>
```

This meta is automatically added to EPUB3 files generated with iBooks Author. It is probably used to trigger another rendering engine which supports very specific `-ibooks-` prefixed CSS properties (i.e. `-ibooks-layout-hint`, `-ibooks-list-text-indent`, `-ibooks-strikethru-type`, `-ibooks-strikethru-width`, `-ibooks-underline-type`, `-ibooks-underline-width`, `-ibooks-stroke`, `-ibooks-popover-background`, and `-ibooks-popover-shadow`) and the [Tab Stops for CSS](https://www.w3.org/People/howcome/t/970224HTMLERB-CSS/WD-tabs-970117.html).

Moreover, this format is using interactive widgets which are managed at the Reading System level (the EPUB file contains `<object>` with a type of `application/x-ibooks+widget` and custom data-attributes for styling).

It could be used as a flag to indicate that the EPUB file was meant for iBooks and there will be rendering issues, especially interactive widgets since they won’t work as intended by the author.

### Kindle

Kindle is using specific metadata for the primary writing mode and Fixed Layout – for both rendition and enabling FXL-specific features.

This metadata has no prefix.

Docs: [Kindle Publishing Guidelines](https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf)

[Further details and undocumented metadata](http://www.fantasycastlebooks.com/Tutorials/kindle-tutorial-part6.html).

#### Primary writing mode

```
<meta name="primary-writing-mode" content="horizontal-rl"/>
```

Values can be `horizontal-lr`, `horizontal-rl`, `vertical-lr`, or `vertical-rl`.

This is a recent addition to the set of Kindle-specific metadata, probably as part of an internationalization effort.

It indicates the writing mode that should be used for the entire publication, including the flow in which virtual panels and magnification regions must progress when swiped in Fixed Layout (manga/comics and children books).

#### Fixed layout

```
<meta name="fixed-layout" content="true"/>
```

This is an older version of

```
<meta property="rendition:layout">pre-paginated</meta>
```

It should therefore be considered an alias.

#### Original resolution

```
<meta name="original-resolution" content="1024x600"/>
```

This indicates the original design resolution of the content.

#### Orientation lock

```
<meta name="orientation-lock" content="landscape"/>
```

This is an older version of 

```
<meta property="rendition:orientation">landscape</meta>
```

It should therefore be considered an alias.

#### Book type

```
<meta name="book-type" content="children"/>
```

Values can be `children` or `comic`.

This removes reader functionality which may not be relevant for those genres (e.g. search, share, etc.). For more details, [see this chart](http://www.fantasycastlebooks.com/Tutorials/kindle-fixed-layout-functionality.html).

#### Blank pages

```
<itemref idref="blank-page" properties="layout-blank"/>
```

This property can be used to indicated a page is blank. If set, the page will be rendered in landscape mode (spread) but not in portrait mode (single page).

#### Region magnification (deprecated)

```
<meta name="regionMagnification" content="true"/>
```

This property was used to indicate the Region Magnification feature should be used for the Fixed-Layout book. It is now set automatically if the required markup is found in XHTML documents.

The Region Magnification feature allows users to zoom text (pop-up view) and comics’ panels, as well as navigating panel by panel. Authors must however provide specific markup (i.e. using a `app-amzn-magnify` class and storing `targetId`, `sourceId` and `ordinal` attributes in a JSON object as part of a `data-app-amzn-magnify` value).

**Note:** authors may be using a JavaScript polyfill to re-use this markup in EPUB apps that support scripting. We recommend not trying to implement those features in your app unless JavaScript is strictly disabled for authoring purposes.

## Attributes

The iBooks Reading System is using a set of custom attributes to manage styling and features. 

### iBooks writing mode

```
__ibooks_writing_mode
```

Values can be the ones for the CSS property `writing-mode` i.e. `horizontal-tb`, `vertical-rl`, or `vertical-lr`.

This attribute is used to style pagination and scroll, the “page” dimensions, and vertical-alignment of some elements.

Authors’ usage of this attribute is unknown.

### iBooks theme

```
__ibooks_internal_theme
```

Values can be `BKWhiteStyleTheme`, `BKSepiaStyleTheme`, `BKGrayStyleTheme`, or `BKNightStyleTheme`.

This attribute is used to apply reading modes (`background-color`, `color` and `filter`). 

A handful of authors have been using them as a replacement to alternate stylesheets in iBooks.

### iBooks font-family override

```
__ibooks_font_override
```

Value can be `true`.

This attribute is used to indicate DOM elements for which the `font-family` must be changed when the user applies a typeface.

Authors’ usage of this attribute is unknown, but it may have set expectations and/or CSS hacks which aim is getting parts of the content not impacted by this user setting.

### iBooks text-align override

```
__ibooks_align_override
```

Value can be `true`.

This attribute is used to indicate DOM elements for which `text-align` must be changed when the user sets justification preferences.

Authors’ usage of this attribute is unknown, but it may have set expectations (e.g. elements with `text-align: right` won’t be impacted).

### iBooks respect image size (gaiji)

```
__ibooks_respect_image_size
```

Value can be `true`.

This attribute is used to style images which class have been explicitly set in the `ibooks:respect-image-size-class` meta.

Authors’ usage of this attribute is indirect since they set the class name for which this attribute must be added.

## Webkit’s CSS multi-column extensions

Apple extended the CSS multi-column specification with non-standard CSS properties to handle RTL scripts and vertical writing modes in iBooks. They were primarily designed for the `setPagination` API in the iOS UIWebView but works as expected in Safari/iOS webviews.

### Column axis

```
-webkit-column-axis
```

Value can be `auto`, `horizontal` or `vertical`.

This CSS property can be used on iOS/Safari to force the column axis whenever needed. It allows the Reading App to lay out columns on the `x-axis` (horizontal) when the document is in a `vertical-*` writing mode for instance, which permits a two-column spread view as expected in print – CSS multicol being automatically laid out on the `y-axis` in those writing modes. 

This property was removed from Blink in 2014.

It is noteworthy that `columns`, `column-width`, and `column-count` will be ignored when using this non-standard `-webkit-column-axis` property, and the `width` and `height` of the root (`html`) element will be used to lay out columns. Moreover, `box-sizing` may not work as expected, and impact `padding` and `column-gap`.

You can check [this demo in Safari](https://codepen.io/JayPanoz/pen/bYjEOE) to see the effect this CSS Property has.

### Column progression

```
-webkit-column-progression
```

Value can be `normal` or `reversed`.

This CSS property can be used on iOS/Safari to force the column progression whenever needed. It allows the Reading App to reverse the column progression for LTR documents in a RTL publication, and vice versa. 

This property was removed from Blink in 2014.

You can check [this demo in Safari](https://codepen.io/JayPanoz/pen/bYjEOE) and uncomment the CSS property in `:root` to see the effect it has.

## Non-standard CSS properties

EPUB authors may have used the following non-standard properties to achieve specific styling or get around Reading Systems’ overrides.

### Adobe text layout

```
adobe-text-layout
```

Values can be `optimizeSpeed` and (probably) `optimizeLegibility`, or `auto`.

Although this property and (especially) its values look closely related to `text-rendering` – so close that authors might have been using `text-rendering` instead –, it isn’t exactly the same.

At the time RMSDK 9.2 was released, this CSS property allowed authors to disable the typography enhancements, including hyphenation, by forcing the older text engine. It was thus primarily used to disable hyphenation, as some devices didn’t support `adobe-hyphenate` – and they could not disable hyphens for headings for instance.

Source: [MobileRead Forums](https://www.mobileread.com/forums/showpost.php?p=1513269&postcount=8) 

### Overflow scrolling

```
-webkit-overflow-scrolling
```

Values can be `auto` or `touch`.

The `-webkit-overflow-scrolling` CSS property controls whether or not touch devices use momentum-based scrolling for a given element.

Some authors may have been using this CSS property to improve the scrolling of overflowing elements, especially in fixed-layout.

Docs: [Safari Docs](https://developer.apple.com/library/content/documentation/AppleApplications/Reference/SafariCSSRef/Articles/StandardCSSProperties.html#//apple_ref/doc/uid/TP30001266-SW26)

### Non-breaking space mode

```
-webkit-nbsp-mode
```

Values can be `normal` or `space`.

The `-webkit-nbsp-mode` CSS property specifies the behavior of non-breaking spaces within an element.

This CSS property might pop up in files generated with the built-in MacOS’ (Cocoa) HTML generator. 

Please note that although it might fix an issue in some languages like English, it might also create other issues in other languages, where non-breaking space shouldn’t be treated as a normal space (e.g. punctuation in French).

Docs: [Safari Docs](https://developer.apple.com/library/content/documentation/AppleApplications/Reference/SafariCSSRef/Articles/StandardCSSProperties.html#//apple_ref/doc/uid/TP30001266--webkit-nbsp-mode)

### Text fill

```
-webkit-text-fill-color
```

The `-webkit-text-fill-color` property defines the foreground fill color of an element’s text content e.g. headings or links.

Authors may have used this property to force a color for text in night mode, especially when targeting iBooks.

### Text stroking

```
-webkit-text-stroke
-webkit-text-stroke-color
-webkit-text-stroke-width
```

The `-webkit-text-stroke-color` property specifies a stroke color for an element’s text, the `-webkit-text-stroke-width` property specifies the width of the stroke drawn at the edge of each glyph of an element’s text.

This can be used to achieve text with a stroked effect, or faux bolding, although this is pretty rare.

### Background color and gradients

```
-webkit-linear-gradient()
```

The `-webkit-linear-gradient()` function must be treated as an alias of `linear-gradient()`.

Some authors might have been using this to force a `background-color` in night mode. It would consequently be used in combination with `-webkit-text-fill-color` so that the text `color` is forced as well.

### Mobile Text Size Adjustment

```
-webkit-text-size-adjust
```

The `-webkit-text-size-adjust` property allows control over the text inflation algorithm used on some mobile devices, many mobile browsers indeed apply a text inflation algorithm to make the text larger and more readable. By using this property, authors can simply opt out or modify this behavior.

Some web authors and/or authoring software might be using this property out of habit, but it actually breaks iBooks’ `font-size` user setting for instance. This should not be an issue in Readium CSS.

Docs: [CSS Mobile Text Size Adjustment Module Level 1](https://drafts.csswg.org/css-size-adjust-1/)

### Text decoration styling

```
-webkit-text-decoration-line
-webkit-text-decoration-color
-webkit-text-decoration-style
```

The `-webkit-text-decoration-line` CSS property sets the kind of decoration that is used on text in an element, the `-webkit-text-decoration-color` CSS property sets the color of the decorative additions to text and the `-webkit-text-decoration-style` CSS property sets the style of the lines.

There are standard unprefixed properties for these but some authors may have been using the `-webkit-` ones only. 

MDN: https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Text_Decoration

### Hyphenation

```
adobe-hyphenate
```

Values can be `none`, `explicit`, or `auto`.

This must be treated as an alias of `hyphens` (with `explicit` as an alias of `manual`).

A very large part of authors have been using this CSS property to control hyphenation. It should not be an issue as they probably at least used `-webkit-hyphens` and `-epub-hyphens` in addition to this `adobe-hyphenate` CSS property.

Source: [MobileRead Forums](https://www.mobileread.com/forums/showpost.php?p=1569362&postcount=30)

### Hyphenate limit

```
-webkit-hyphenate-limit-before
-webkit-hyphenate-limit-after
-webkit-hyphenate-limit-lines
```

The `-webkit-hyphenate-limit-before` CSS property indicates the minimum number of characters in a hyphenated word before the hyphenation character, the `-webkit-hyphenate-limit-after` CSS property indicates the minimum number of characters in a hyphenated word after the hyphenation character, and the `-webkit-hyphenate-limit-lines` CSS property indicates the maximum number of successive hyphenated lines in an element.

Those properties are standardized in CSS Text Module Level 4, but `-webkit-hyphenate-limit-before` and `-webkit-hyphenate-limit-after` have been replaced with the `hyphenate-limit-chars` CSS property.

Docs: [CSS Text Module Level 4](https://www.w3.org/TR/css-text-4/#hyphenation)

## EPUB properties

Some authors might have used `-epub-` prefixed properties only, thinking they were enough since those CSS properties were standardized. Authors are now strongly encouraged to use unprefixed properties in EPUB 3.2. 

In the meantime, implementers may want to polyfill at least some of those properties, especially those related to vertical writing, as practical issues may arise due to the lack of file updates on the authoring side. [A mapping is available](https://w3c.github.io/publ-epub-revision/epub32/spec/epub-contentdocs.html#sec-css-prefixed) in the EPUB 3.2 spec if needed. 

A [PostCSS Plugin](https://github.com/JayPanoz/postcss-epub-interceptor) has been specifically created to unprefix those properties and change their value whenever needed – PostCSS can indeed be browerified. Implementers’ best option is running this process once and for all before distribution or file opening.