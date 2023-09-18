# Internationalization

[Implementers’ doc] [Authors’ doc] [WIP]

The aim of this document is to provide implementers with a baseline for internationalization.

## The current situation

Internationalization is an ongoing process, with browsers offering subpar interoperability (typography, writing modes, etc.), Operating Systems sometimes lacking fonts for some languages, and documentation providing little information on topics of interest to Reading Systems (a11y, settings, etc.).

This could well explain why the most popular Reading Apps tend to implement the least common denominator for all languages, especially when it comes to user settings, and to not support more complex languages implementers either know they can’t support well or are used on smaller markets the app/service is not targeting.

Occasionally, when an app does support a language, it can put some constraints on authors, as there is no other way to make sure the publication will be handled well otherwise e.g. specifying the `Hans` or `Hant` script for Chinese (`zh`).

We can’t overemphasize the importance of the internationalization process though, as the 24 languages we added extend support to 3,049,150,507 speakers, from the 1,150 people speaking Western Canadian Inuktitut, to the 1,200,000,000 speaking Chinese. Implementing right to left scripts will extend support to 411,000,000 native speakers, while vertical writing to at least 130,200,000 – excluding Chinese and Korean.

In total, we can cover the needs of 5,262,900,507 speakers. Credit where credit is due, this wouldn’t have been possible if Operating Systems and browsers didn’t tackle this process upstream, added fonts for those languages and improved support in rendering engines.

## Resources

- [W3C Internationalization Working Group Home Page](https://www.w3.org/International/core/Overview)
- [Creating HTML Pages in Arabic, Hebrew and Other Right-to-left Scripts (tutorial)](https://www.w3.org/International/tutorials/bidi-xhtml/index)
- [Styling vertical Chinese, Japanese, Korean and Mongolian text](https://www.w3.org/International/articles/vertical-text/index)
- [International text layout and typography index](https://w3c.github.io/typography/)
- [Requirements for Japanese Text Layout](https://www.w3.org/TR/jlreq/)
- [Requirements for Chinese Text Layout](http://w3c.github.io/clreq/)
- [Requirements for Hangul Text Layout](https://w3c.github.io/klreq/)
- [Requirements for the Arabic Script](http://w3c.github.io/alreq/)
- [Requirements for Hebrew Text Layout](https://w3c.github.io/hlreq/)
- [Requirements for Indic Text Layout](https://www.w3.org/TR/ilreq/)
- [EBPAJ File creation guide](http://ebpaj.jp/counsel/guide)

## What implementers need

Supporting the maximum amount of languages and scripts is a complex process. 

As a consequence, work for internationalization should be tackled early, as the changes and adaptations needed will have a significant impact on an existing implementation. It indeed impacts the entire implementation, and not only CSS.

### Retrieving the significant information in the OPF

Implementers will need a way to retrieve `page-progression-direction` and the primary language (`<dc:language>`) of the publication.

#### Page-progression-direction

This attribute is set on the `<spine>` item, and the value `rtl` should be considered important information for the whole process.

This value signals the publication is either an RTL script, or is using the `vertical-rl` writing mode, which is the reason why we must find the primary language of the publication next.

The value is important to store, as it will be the one used for the `dir` attribute to append if it is missing in a document.

#### Primary Language

**It is very important to note the primary language must be checked in all cases, and not only when the `page-progression-direction` is set or has an `rtl` value.**

Indeed, this piece will be even more critical in the following steps, as it will trigger the list of fonts to load for the publication, the user settings to provide, and the `xml:lang` attribute to append if it is missing in a document.

#### Possible issues

The OPF file should not be considered a single source of truth for the publication, since issues may arise relatively quickly. We can’t call the process “heuristics” *per se,* it’s more of a chain of educated guesses.

##### Multiple language items

There exists an increasing corpus of EPUB files with multiple `<dc:language>` items. Some authoring tools, for instance, list all languages a publication contains.

In this case, `page-progression-direction` should serve as a hint, if present. For instance:

1. the first `<dc:language>` item is English;
2. the second `<dc:language>` item is Japanese;
3. the `page-progression-direction` is `rtl`;
4. the primary language is Japanese.

Obviously, this can quickly become an issue if both languages share the same `page-progression-direction`…

1. the first `<dc:language>` item is English;
2. the second `<dc:language>` item is Japanese;
3. the `page-progression-direction` is missing;
4. we can’t guess the primary language from the OPF.

In such an edge case, to achieve the best interoperability possible, the first `<dc:language>` element must be considered the primary language, unless you can pre-process all documents in a publication to determine it beforehand.

##### Missing page-progression-direction

For some reason the `page-progression-direction` may be missing in the OPF, which can be true if the publication is EPUB2 for instance – which supports the `direction` CSS property and, in theory, could support RTL scripts.

The following guidance is informal:

1. if the `page-progression-direction` is missing;
2. if there is only one `<dc:language>` item which clearly signals the `page-progression-direction`:
    - `ar`, `fa`, and `he`;
    - `zh-Hant`, or `zh-TW`.
3. then you can assume the `page-progression-direction` is `rtl`.

The decision to handle this edge case is up to each implementer, especially as it can be considered a patch of an authoring failure.

### Triggering the correct page-progression-direction

Once the `page-progression-direction` is defined as `rtl`, it must be reversed in the app:

- the previous resource (document) is on the right;
- the next resource (document) is on the left.

Navigating the publication should follow this pattern.

### Appending information into documents

Missing attributes in each document is far from an uncommon or edge case. 

Since the `page-progression-direction` or `<dc:language>` are already set in the OPF, some authors might think they will automatically apply to all the resources in the EPUB file, and explicitly set it only when it differs from those global values. More importantly, some Reading Apps are automatically managing this, and should authors only check their files in those apps, it could lead them to believe it just works.

#### Language

The language is important as it will enable hyphenation and use the proper rules specific to each language if a dictionnary is available, change the default typeface for some languages, and even apply language-specific styles for layout (e.g. pagination, defaults for unstyled publications, etc.).

The following process must be implemented:

1. if `xml:lang` can’t be found on `html`;
2. check if `xml:lang` can be found on `body`, copy and set it to `html`, and stop there if it is;
3. if it can’t be found on `body`, use the primary language retrieved from the OPF file and set it to `html` and `body`.

#### Direction

The `dir` attribute is critical too, as it will reverse the column direction for RTL scripts.

**The following must only apply if the primary language is `ar`, `fa`, and `he`. It MUST NOT apply to CJK.**

The following process must be implemented:

1. if the `dir` attribute can’t be found on `html`;
2. check if `dir` can be found on `body`:
    1. if it is the same value as the one retrieved from the OPF file, copy it;
    2. if it differs from the one retrieved from the OPF file, change the value;
3. set the `dir` attribute with the correct value on `html`.

### Loading the correct styles

Readium CSS provides implementers with specific stylesheets for RTL scripts and CJK (including the Mongolian script), those styles must be loaded accordingly. Otherwise, styles for LTR scripts in the horizontal writing mode will apply.

Guidance can be found in the [Injection and Pagination document](../docs/CSS03-injection_and_pagination.md).

### Modifying the UI of the app

Ideally, several parts of the app should be customizable depending on the publication. Another option is implementing the least common denominator for all languages.

#### All Languages

The list of fonts the app offers to users should be specific to the primary language of the publication, and `writing-mode` if it applies – Japanese currently.

This means fonts for Latin language can’t be reused for Indic, RTL scripts, CJK, etc.

#### RTL

Several parts of the UI must follow the direction (`rtl`) of the primary language:

- the running header (title of the publication or chapter);
- the toc and its entries;
- user settings e.g. text align;
- implementers might want to localize the interface based on the language set at the system level, or at least fall back to English.

Moreover, some user settings should be removed if used (`letter-` and `word-spacing`) and another one added (arabic ligatures in `ar` and `fa`).

#### CJK

Although the UI can keep an `ltr` direction with a `horizontal-tb` writing mode, some extra attention should be paid:

- make sure the “UI font” can display the characters needed in those languages;
- implementers might want to allow authors to set a `vertical-*` writing mode for the navigation document (`nav.xhtml`); 
- implementers might want to localize the interface based on the language set at the system level, or at least fall back to English.

Moreover, some user settings should be removed if used (`text-align`, `hyphens`, `letter-` and `word-spacing`, number of columns in vertical writing, etc.). See [User Prefs for further details](../docs/CSS12-user_prefs.md#user-settings-can-be-language-specific).

### Internationalize features

Implementers should make sure features like search, highlighting, etc. can work well with bidirectional text and unicode – CJK, especially as some characters change from horizontal to vertical writing modes.

Another issue to take into account is that input methods might not allow users to use some features easily, in which case extended research should be made to check realistic options.

### Fonts

Implementers should make sure they offer at least two options: the publisher’s font and the default. If they want to extend the list of fonts they provide for some languages, they should follow guidance in the [User Preferences doc](../docs/CSS12-user_prefs.md#user-settings-can-be-language-specific).

## How authors can help

Authors can help in different ways, from authoring files to giving valuable information we can use to improve the entire Readium CSS project.

### Authoring

In some cases, EPUB files themselves will help us improve support tremendously.

The metadata in the OPF file will help us trigger the expected behavior. For instance: 

- declaring only one `<dc:language>` will help us determine the primary language of the publication;
- if this is not possible, add the primary language as the first `<dc:language>` item in the OPF;
- declaring the script e.g. `zh-Hant` or `mn-Mong` will help us trigger the correct `writing-mode`;
- using the EBPAJ guide meta will help us improve support across platforms.

Setting attributes in each document of the publication will also help us make educated choices and could eventually allow us to handle some edge cases like mixed directions or mixed writing modes. Our aim is obviously not to put the burden on authors there, but collaboration is the only solution to get around those edge cases the EPUB specification doesn’t discuss.

What you can do:

- embed fonts when necessary, so that users have at least two options to choose from in settings and/or proper support of the language;
- set the `xml:lang` attribute on `html`;
- set the `dir` attribute on `html`;
- avoid using the `direction` CSS property, [as it is now recommended](https://developer.mozilla.org/en-US/docs/Web/CSS/direction).

### Reporting issues

First and foremost, please be assured that we’re as annoyed as you are if there is an issue. It doesn’t mean we don’t care, far from it… it just means we lacked information or misunderstood some requirements when designing Readium CSS. Our goal is to achieve the best support possible, and we know we can’t fulfill it without your expertise.

If you encounter any issue specific to a language or script, do not hesitate to [open a new issue in the github repository](https://github.com/readium/readium-css/issues). A reduced sample will definitely help. [Check our guidelines for further details](https://github.com/readium/readium-css/blob/master/contributing.md#reporting-bugs).

The most information we have, the better the fix will be. Please feel free to go into extensive details and provide links, documentation, examples, etc. we can check to get a solid grasp of the issue we have to fix.

### Sending feedback

We’re not saying you should implement a support of script yourself if we don’t support it at all or very well, but it is noteworthy that we can’t necessarily get all the details right, especially when we don’t know how to speak and write the language you wish were supported or improved.

If our font-stacks are not good enough, a solider one will help. If we don’t have any font-stack for a language, a basic one will do. Additionally, you can point us to libre/open-source fonts which you think should be recommended to implementers.

If we’ve got something wrong in terms of typography, an example – picture, codepen demo, etc. – will help us see how we got there. Please bear in mind we might be limited by what web browsers can do though, and some issues could be consequently deemed “out of scope.” We will at least do our best to report the issue to whom it may concern whenever possible.

[Check our contribution guidelines for further details](https://github.com/readium/readium-css/blob/master/contributing.md#how-can-i-contribute).

## Overarching issues

Implementers should be aware there are overarching issues for which we haven’t reached consensus, or couldn’t discuss yet.

The most important issue, by very far, is that checking the `writing-mode` at runtime can blow performance in extreme ways. It can indeed take 15 seconds to render some complex XHTML files in `vertical-*`. Needless to say, this would obviously be worse in terms of UX. And this is the reason why we try to guess the `writing-mode` from the OPF file.

Longer terms issues include: 

- polyfilling `-epub-properties` for web apps;
- mixed directions (LTR document in a RTL publication) and mixed writing modes (`horizontal-tb` document in a `vertical-rl` publication);
- support for alternate stylesheets, which is critical if the implementer wants to offer a horizontal/vertical-writing user setting;
- support for `rendition: align-x-center`;
- support for `ibooks:respect-image-size-class` (gaiji) and `ibooks:scroll-axis` metadata items (see [EPUB Compat doc](../docs/CSS21-epub_compat.md#gaiji-and-image-sizing));
- `rendition: flow` of `scrolled-doc`.

## Out of scope

There are some typography and layout issues which are not the responsibility of apps’ implementers but rendering engines’. Those issues include: 

- line-adjustment and justification (RTL and CJK);
- run-in headings (`display: run-in`), which is popular in CJK;
- `ruby` and its styling;
- `bidi`;
- Kashida Elongation (Arabic);
- joining forms (Arabic);
- single-letter styling (Arabic).

If those issues arise, please report them to whom it may concern (e.g. Chromium, Firefox, Microsoft, Webkit, etc.). The entire web platform will indeed benefit. You can additionally report the issue to us so that we can document it for other implementers.

## Glossary

Check the [i18n Glossary doc](../docs/CSS26-i18n_glossary.md) for a glossary of terms you may encounter during bug reports, issues, or feedbacks.