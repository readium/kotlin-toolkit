# Internationalization – A Typography Primer

[Implementers’ doc]

The aim of this document is to provide implementers with a typography baseline for internationalization.

## Abstract

Various scripts and/or languages can vary from the typography rules implementers may be accustomed to in Latin. There are important idiosyncrasies to take into account, as it will impact the text layout of documents, base and default styles, and user settings.

The best way to keep up with best practices and requirements is the [International text layout and typography index](https://w3c.github.io/typography/). In case of doubt, do not hesitate to check this index.

## Fonts

In some scripts, it is common to use different fonts for headings or emphasis, rather than bolding or italicisation. For instance, Chinese, Japanese and Korean fonts almost always lack italic or oblique faces, because those are not native typographic traditions.

Proper italic glyphs in Cyrillic text can look very different from normal variants, so synthesising italics can produce poor results. This is the reason why implementers should make sure they provide an italic style for the Cyrillic fonts they are offering to users.

Special OpenType features may need to be supported, especially for the Arabic script, which needs ligatures. 

Complex scripts have rules about how characters combine in syllabic structures, and scripts like Arabic may need controls to indicate where ligatures are wanted or not wanted. Authors may consequently use the zero-width joiner and zero-width non-joiner characters to force or disable ligatures, which can impact some features like search.

In CJK, fonts must support both horizontal and vertical writing modes, and extra attention must be paid as some characters will change depending on the writing mode of the document. It is for instance important to be able to convert between half-width and full-width presentation forms, and make sure the correct quotation marks are used – they are not the same depending on the writing mode.

## Text decoration

Underlines need to be broken in special ways for some scripts, and the height of underlines, strike-through and overlines may vary depending on the script.

For vertical text the placement needs to be to the right or left of the line of text, rather than under or over.

## Emphasis

Bold and italic are not always appropriate for expressing emphasis, and some scripts have their own unique ways of doing it.

In Japanese, a sans-serif font, or emphasis dots (horizontal writing) and sesame (vertical writing) may be used to express emphasis. 

In the Amharic Script (Ethiopic), underlines, larger text or a different color may be used to express emphasis. 

In Tibetan, marks and colors may be used to express emphasis.

## Line breaking

In some scripts like Chinese and Japanese, text is set solid, which means there is no space between words. These scripts tend to break a line in the middle of a word (with no hyphenation) – even in Korean, which has spaces between words.

It would be strange to offer a word-spacing setting to users in those languages, and letter-spacing may create extra justification issues you won’t be able to resolve, for lack of more precise controls.

It is common for certain characters to be forbidden at the start or end of a line, but which characters these are, and what rules are applied when depends on the script or language. In some cases, such as Japanese, there may be different rules according to the type of content or the user's preference.

## Hyphenation

Although CJK doesn’t use hyphenation, documents may contain other scripts which use hyphenation e.g. Latin in Japanese.

## Justification

Typographic conventions for full text justification depend on the writing system, the content language, and the calligraphic style of the text. Some scripts have complex methods the rendering engine doesn’t necessarily support.

Kashida elongation is not supported for the Arabic script for instance (`text-justify`), but authors may be using the tatweel joining character (see [i18n glossary doc](../docs/CSS26-i18n_glossary.md)) to elongate some words.

Chinese and Japanese are using `inter-character` justification for instance, while Latin uses `inter-word`. 

## Indents

In some scripts like CJK, indents are really important. Implementers should not try to provide users with such a setting, or apply a default designed for Latin.

## Vertical text

Implementers must not try to provide a setting allowing users to switch from the vertical to horizontal writing mode, and vice versa, unless they support alternate stylesheets and the publication provides one for each mode. Indeed, characters, punctuation, emphasis marks, quotes, and so on and so forth, depend on the writing mode used, and an automatic switch would provide users with a subpar experience. 

It's common for content authors to want to mix short horizontal runs of text, such as the tate-chu-yoko i.e. 2-digit numbers (`text-combine-upright`) or acronyms (`text-orientation`), in a vertical text.

Hanging punctuation at the end of lines can help improve justification for CJK in the vertical writing mode, but may be considered strange in the horizontal writing mode in some languages.

In Mongolian, it would be unexpected to have text written `horizontal-tb` if it is not using the Cyrillic script. The Mongolian script is indeed written `vertical-lr` and, unlike Chinese, Japanese, and Korean, can’t be written `horizontal-tb`.