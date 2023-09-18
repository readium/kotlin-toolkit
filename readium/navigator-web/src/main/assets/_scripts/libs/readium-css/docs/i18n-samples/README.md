# ReadiumCSS i18n samples

This folder contains a set of small samples whose goal is to help implementers test and improve the internationalization support of their app.

The primary focus are text (typography, fonts) and rendition (`page-progression-direction`, `dir`, and `writing-mode`). However they can also be used to: 

- test the UI of the app (toc, run-in headings, language-specific user settings, etc.);
- metadata parsing (`dc:title`, multiple `dc:language` items, and alternate script).

## Classification

The `latin.epub` file serves as a base, it is a control which allows implementers to check if there is no rendition issue to fix before testing all other samples.

### Left to Right 

#### Indic

- Bengali
- Gujarati
- Hindi
- Kannada
- Malayalam
- Oriya
- Punjabi
- Sinhalese
- Tamil
- Telugu

#### Other Languages

- Amharic
- Armenian
- Cherokee
- Inuktitut
- Khmer
- Lao
- Thai
- Tibetan

### Right to left

- Arabic
- Hebrew
- Persian/Farsi

### CJK

#### Horizontal writing

- Chinese
- Japanese
- Korean

#### Vertical writing

- Japanese (`vertical-rl`)
- Mongolian (`vertical-lr`)

### Edge Cases

The most complex i18n issue to handle at the rendition level is managing publications in which some documents are in another language, and either `direction` or `writing-mode` differs from the publication.

Consequently, two samples are provided to test those two edge cases:

- mixed-directions (`dir`);
- mixed-writing-modes (`writing-mode`).

Both files contain:

- 1 `<dc:title>` item for the publication (in Arabic or Japanese);
- 1 `alternate script` for the title (in English);
- 2 `<dc:language>` (Arabic || Japanese && English);
- 1 `page-progression-direction` attribute on the `<spine>` and whose value is `rtl`;
- 1 table of content (`nav`) in the primary language (Arabic or Japanese);
- 1 title page in the primary language (Arabic or Japanese);
- 1 document in the primary language (Arabic or Japanese), with the following: 
    1. Mixed directions: a `dir="rtl"` attribute on `html`;
    2. Mixed writing modes: a `writing-mode: vertical-rl` style on `html`.
- 1 document in the secondary language (English), with the following:
    1. Mixed directions: a `dir="ltr"` attribute on `html`;
    2. Mixed writing modes: a `writing-mode: horizontal-tb` style on `html`.

Those two edge cases raise interoperability issues in the EPUB ecosystem. As of January 2018, expected results are:

1. Mixed directions: rendition based on the `page-progression-direction`, with every document forced on a `rtl` direction;
2. Mixed writing modes: rendition based on the `page-progression-direction`, with every document forced on a `vertical-rl` writing mode.

#### Poorlyfill for reverse column-progression

Webkit has a specific `-webkit-column-progression` CSS property whose value can be `normal` or `reverse`. This is non-standard and only supported in Webkit – it was indeed removed from Blink in 2014.

This property is used in the `setPagination` API available in the old `UIWebView` (iOS), so that left-to-right documents in a right-to-left publication can follow the natural `page-progression-direction` set on the spine (`rtl`).

There is a trick to emulate this CSS property, but it hasn’t been tested extensively. The logic is the following:

1. if the publication is EPUB3;
2. it has a `page-progression-direction="rtl"` (`spine` item);
3. the primary language (`<dc:language>` item) of the publication is Arabic, Hebrew, or Persian (there may be additional scripts/languages to take into account);
4. the document has:
    1. an explicit `xml:lang` or `lang` attribute set on either `html` or `body`, which differs from the publication;
    2. lacks a `dir` attribute or has an explicit `dir="ltr"` attribute set on either `html` or `body`;
    3. an explicit `direction="ltr"` CSS property is used by the author if no `dir` attribute can be found.
5. the `dir="rtl"` attribute is set for `html`;
6. the `dir="ltr"` attribute can be set for `body` in order to reverse the column progression.

Columns, set on `html` will consequently follow the `rtl` direction while contents `body` will follow the `lrt` direction so the first “page” for instance will be on the right, the second one on the left, etc. in a spread view.

This solution won’t work for Trident/EdgeHTML engines though, and will fail in IE11/Edge. This looks like the correct interpretation of the [CSS Writing Modes Level 3](https://www.w3.org/TR/css-writing-modes-3/#principal-flow):

> As a special case for handling HTML documents, if the `:root` element has a `<body>` child element, the principal writing mode is instead taken from the values of `writing-mode` and `direction` on the first such child element instead of taken from the root element.

What this means is that the `dir` attribute (or the `direction` CSS property) set for `body` will override the one set for `html`. Unlike most other CSS properties, which don’t impact the parent element, the `dir` attribute (or the `direction` CSS property) should propagate.

#### Poorlyfill for column-axis

Webkit has a specific `-webkit-column-axis` CSS property whose value can be `auto`, `horizontal` or `vertical`. This is non-standard and only supported in Webkit – it was indeed removed from Blink in 2014.

This property is used in the `setPagination` API available in the old `UIWebView` (iOS), so that documents with a `vertical-*` writing mode can be laid out in columns on the `x-axis`. Column axis indeed automatically follow the axis of the `writing-mode` set.

Unfortunately, there is currently no way to emulate this CSS property, and `html` will even acquire the `writing-mode` set for `body`.

## Reporting issues

An [i18n-specific issue](https://github.com/readium/readium-css/issues/26) has been opened to deal with issues, documentation and support. Please feel free to raise any global issue you may encounter.