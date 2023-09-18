# Typefaces and font-stacks

[Implementers’ doc] [Authors’ info]

In order to get the best performance and language support possible, we tried to design off-the-rack font-stacks using pre-installed system fonts.

Those font-stacks cover:

- MacOS > 10.9;
- iOS > 9;
- Windows > 7;
- Android > 4.4.4.

The unicode ranges to be found in this document are specifically scoped to their language and are provided in case you want to use other typefaces: it can help you check support and extended coverage, and subset if needed.

## Test files

Test files can be retrieved from [the Readium CSS’ i18n-samples OPDS feed](https://raw.githubusercontent.com/readium/readium-css/develop/docs/i18n-samples/root.atom).

## Fundamentals

UA defaults if no `font-family` is set usually are: 

- Times (New Roman)
- Droid Sans/Roboto on Android

You could force Android using a serif font by declaring 

```
font-family: “Times New Roman”;
```

But it will be resolved to Droid Serif.

[For generic families](https://www.granneman.com/webdev/coding/css/fonts-and-formatting/web-browser-font-defaults/) (sans-serif, serif, monospace), you’ll usually get: 

- Arial/Helvetica/Roboto for sans-serif;
- Times/Times New Roman/Droid Serif for serif;
- Courier/Courier New/Droid Mono for monospace. 

Historically, monospaced fonts have had a font-size of `13.33px`. You’d better be cautious about that since a margin of `1em` will indeed be `13.33px` for `pre`.

Finally, rendering engines fall back on a glyph basis. If a glyph is not available in the preferred font, it will check if it is in the second one, and so on and so forth, for each glyph.

## UI 

If you want a [system font stack](https://css-tricks.com/snippets/css/system-font-stack/), it already exists. 

```
font-family: -apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantrell, "Helvetica Neue", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
```

This is a modernized version taking popular Linux distros and emoji into account.

## Latin

### Old Style (serif)

```
font-family: "Iowan Old Style", "Sitka Text", Palatino, "Book Antiqua", serif;
```

Serif will resolve to Droid Serif on Android.

Open Type features available for those fonts are:

| Typeface        | Small-caps | Oldstyle nums | Linear nums | Proportional nums | Tabular nums |
|-----------------|:----------:|:-------------:|:-----------:|:-----------------:|:------------:|
| Iowan Old Style |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Sitka Text      |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Droid Serif     |      ✕     |       ✕       |      ✕      |         ✕         |       ✕      |

### Modern (serif)

```
font-family: Athelas, Constantia, Georgia, serif;
```

Serif will resolve to Droid Serif on Android.

Open Type features available for those fonts are:

| Typeface    | Small-caps | Oldstyle nums | Linear nums | Proportional nums | Tabular nums |
|-------------|:----------:|:-------------:|:-----------:|:-----------------:|:------------:|
| Athelas     |      ✕     |       ✕       |      ✕      |         ✕         |       ✕      |
| Constantia  |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Droid Serif |      ✕     |       ✕       |      ✕      |         ✕         |       ✕      |

### Neutral (sans)

Same as UI font-stack or the following simplified version:

```
font-family: -apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", Roboto, Noto, "Helvetica Neue", Arial, sans-serif;
```

Open Type features available for those fonts are:

| Typeface       | Small-caps | Oldstyle nums | Linear nums | Proportional nums | Tabular nums |
|----------------|:----------:|:-------------:|:-----------:|:-----------------:|:------------:|
| San Fransisco  |      ✓     |       ✕       |      ✕      |         ✓         |       ✓      |
| Segoe UI       |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Roboto         |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Helvetica Neue |      ✕     |       ✕       |      ✕      |         ✓         |       ✓      |

### Humanist (sans)

```
font-family: Seravek, Calibri, Roboto, Arial, sans-serif;
```

Sans-serif will resolve to Droid Sans or Roboto on Android.

Open Type features available for those fonts are:

| Typeface | Small-caps | Oldstyle nums | Linear nums | Proportional nums | Tabular nums |
|----------|:----------:|:-------------:|:-----------:|:-----------------:|:------------:|
| Seravek  |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Calibri  |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |
| Roboto   |      ✓     |       ✓       |      ✓      |         ✓         |       ✓      |

### Monospace

```
font-family: "Andale Mono", Consolas, monospace;
```

Monospace will resolve to Droid Sans Mono on Android. 

Apple is shipping a monospaced San Fransisco in High Sierra but it is still unclear how you can access it in CSS.

### Unicode ranges

Unicode ranges are: 

```
/* Basic Latin */
unicode-range: U+0020-007F;

/* Latin-1 supplement */
unicode-range: U+0080-00FF;

/* Latin extended A */
unicode-range: U+0100-017F;

/* Latin extended B */
unicode-range: U+0180-024F;
```

In addition, you might want to cover Greek and Cyrillic.

```
/* Greek and coptic */
unicode-range: U+0370-03FF;

/* Cyrillic */
unicode-range: U+0400-04FF;

/* Cyrillic Supplement */
unicode-range: U+0500-052F;
```

## Amharic (am)

```
font-family: Kefa, Nyala, Roboto, Noto, "Noto Sans Ethiopic", serif;
```

**Warning:** no iOS.

```
unicode-range: U+1200-137F, U+1380-139F;
```

## Arabic (ar)

```
font-family: "Geeza Pro", "Arabic Typesetting", Roboto, Noto, "Noto Naskh Arabic", "Times New Roman", serif;
```

```
unicode-range: U+0600-06FF, U+0750-077F, U+08A0-08FF;
```

## Bengali/Bangla (bn)

```
font-family: "Kohinoor Bangla", "Bangla Sangam MN", Vrinda, Roboto, Noto, "Noto Sans Bengali", sans-serif;
```

```
unicode-range: U+0980-09FF;
```

## Tibetan (bo)

```
font-family: Kailasa, "Microsoft Himalaya", Roboto, Noto, "Noto Sans Tibetan", sans-serif;
```

```
unicode-range: U+0F00-0FFF;
```

## Cherokee (chr)

```
font-family: "Plantagenet Cherokee", Roboto, Noto, "Noto Sans Cherokee";
```

**Warning:** no iOS.

```
unicode-range: U+13A0-13FF;
```

## Persian (fa)

```
font-family "Geeza Pro", "Arabic Typesetting", Roboto, Noto, "Noto Naskh Arabic", "Times New Roman", serif;
```

```
unicode-range: U+0600-06FF, U+0750-077F, U+08A0-08FF;
```

## Gujarati (gu)

```
font-family: "Gujarati Sangam MN", "Nirmala UI", Shruti, Roboto, Noto, "Noto Sans Gujarati", sans-serif;
```

```
unicode-range: U+0A80-0AFF;
```

## Hebrew (he)

```
font-family: "New Peninim MT", "Arial Hebrew", Gisha, "Times New Roman", Roboto, Noto, "Noto Sans Hebrew" sans-serif;
```

```
unicode-range: U+0590-05FF;
```

## Hindi (hi)

```
font-family: "Kohinoor Devanagari", "Devanagari Sangam MN", Kokila, "Nirmala UI", Roboto, Noto, "Noto Sans Devanagari", sans-serif;
```

```
unicode-range: U+0900-097F, U+A8E0-A8FF;
```

## Armenian (hy)

```
font-family: Mshtakan, Sylfaen, Roboto, Noto, "Noto Serif Armenian", serif;
```

**Warning:** no iOS.

```
unicode-range: U+0530-058F;
```

## Inuktitut (iu)

```
font-family: "Euphemia UCAS", Euphemia, Roboto, Noto, "Noto Sans Canadian Aboriginal", sans-serif;
```

```
unicode-range: U+1400-167F;
```

## Japanese (ja)

```
font-family: "游ゴシック体", YuGothic, "ヒラギノ丸ゴ", "Hiragino Sans", "Yu Gothic UI", "Meiryo UI", "MS Gothic", Roboto, Noto, "Noto Sans CJK JP", sans-serif;
```

```
unicode-range: U+3000-303F, U+3040-309F, U+30A0-30FF, U+FF00-FFEF, U+4E00-9FAF;
```

## Khmer (km)

```
font-family: "Khmer Sangam MN", "Leelawadee UI", "Khmer UI", Roboto, Noto, "Noto Sans Khmer", sans-serif;
```

```
unicode-range: U+1780-17FF, U+19E0-19FF;
```

## Kannada (kn)

```
font-family: "Kannada Sangam MN", "Nirmala UI", Tunga, Roboto, Noto, "Noto Sans Kannada", sans-serif;
```

```
unicode-range: U+0C80-0CFF;
```

## Korean (ko)

```
font-family: "Nanum Gothic", "Apple SD Gothic Neo", "Malgun Gothic", Roboto, Noto, "Noto Sans CJK KR", sans-serif;
```

```
unicode-range: U+1100-11FF, U+3130-318F, U+A960-A97F, U+AC00-D7AF, U+D7B0-D7FF;
```

## Lao (lo)

```
font-family: "Lao Sangam MN", "Leelawadee UI", "Lao UI", Roboto, Noto, "Noto Sans Lao", sans-serif;
```

```
unicode-range: U+0E80-0EFF;
```

## Malayalam (ml)

```
font-family: "Malayalam Sangam MN", "Nirmala UI", Kartika, Roboto, Noto, "Noto Sans Malayalam", sans-serif;
```

```
unicode-range: U+0D00-0D7F;
```

## Oriya (or)

```
font-family: "Oriya Sangam MN", "Nirmala UI", Kalinga, Roboto, Noto, "Noto Sans Oriya", sans-serif;
```

```
unicode-range: U+0B00-0B7F;
```

## Punjabi (pa)

```
font-family: "Gurmukhi MN", "Nirmala UI", Kartika, Roboto, Noto, "Noto Sans Gurmukhi", sans-serif;
```

```
unicode-range: U+0A00-0A7F;
```

## Sinhalese (si)

```
font-family: "Sinhala Sangam MN", "Nirmala UI", "Iskoola Pota", Roboto, Noto, "Noto Sans Sinhala", sans-serif;
```

```
unicode-range: U+0D80-0DFF;
```

## Tamil (ta)

```
font-family: "Tamil Sangam MN", "Nirmala UI", Latha, Roboto, Noto, "Noto Sans Tamil", sans-serif;
```

```
unicode-range: U+0B80-0BFF;
```

## Telugu (te)

```
font-family: "Kohinoor Telugu", "Telugu Sangam MN", "Nirmala UI", Gautami, Roboto, Noto, "Noto Sans Telugu", sans-serif;
```

```
unicode-range: U+0C00-0C7F;
```

## Thai (th)

```
font-family: "Thonburi", "Leelawadee UI", "Cordia New", Roboto, Noto, "Noto Sans Thai", sans-serif;
```

```
unicode-range: U+0E00-0E7F;
```

## Chinese (zh-CN)

```
font-family: "方体", "PingFang SC", "黑体", "Heiti SC", "Microsoft JhengHei UI", "Microsoft JhengHei", Roboto, Noto, "Noto Sans CJK SC", sans-serif;
```

```
unicode-range: U+3000-303F, U+3040-309F, U+30A0-30FF, U+FF00-FFEF, U+4E00-9FAF;
```

## Chinese – Hong Kong (zh-HK)

```
font-family: "方體", "PingFang HK", "方體", "PingFang TC", "黑體", "Heiti TC", "Microsoft JhengHei UI", "Microsoft JhengHei", Roboto, Noto, "Noto Sans CJK TC", sans-serif;
```

## Chinese – Taiwan (zh-TW)

```
font-family: "方體", "PingFang TC", "黑體", "Heiti TC", "Microsoft JhengHei UI", "Microsoft JhengHei", Roboto, Noto, "Noto Sans CJK TC", sans-serif;
```

## Dyslexic

This is a tricky topic, and it needs quite some expertise, so those won’t be recommendations *per se* but mere insights, since extended research is currently being done in this field. As a consequence, implementers willing to provide the best experience possible should try to keep up-to-date. 

### Typefaces don’t work in a vacuum

[From research conducted by Charles Bigelow](http://bigelowandholmes.typepad.com/bigelow-holmes/2014/11/typography-dyslexia.html) – who designed Lucida and Wingdings with Kris Holmes –, on a corpus of existing scientific research and books: 

> In the scientific literature, I found no evidence that special dyslexia fonts confer statistically significant improvements in reading speed compared to standard, run-of-the-mill fonts. Some studies found that for certain subsets of reading errors, special dyslexia fonts do reduce error rates for dyslexic readers, yet for other subsets of errors, special dyslexic fonts were no better, or in some cases worse; hence, the findings on reading errors are mixed.

Typography is indeed a system and other factors have to be taken into account:

> […] the sizing, spacing, and arrangement of type, but not typeface design *per se* – a few scientific papers found that certain variations in typography offer statistically significant benefits to dyslexic readers.

If needed, the sources of papers and studies available online at the time of this research are [listed in this companion article](http://bigelowandholmes.typepad.com/bigelow-holmes/2014/11/web-links-urls-for-typography-dyslexia-post.html). 

### General tips

It is important to emphasize that the user settings (and their values) implementers will offer users is as equally – if not more – important as the typefaces themselves. 

[A list of general tips:](http://adrianroselli.com/2015/03/typefaces-for-dyslexia.html) 

- avoid justified text (`text-align` setting);
- use generous line spacing / leading (`line-height` setting);
- use generous letter spacing (`letter-spacing` setting);
- avoid italics (`--USER__a11yNormalize` flag);
- generally use sans serif faces (`font-family` setting);
- use larger text (`font-size` setting);
- use good contrast (`background-` and `color` settings, reading modes, themes);
- use clear, concise writing.

[The WCAG 2.1 spec](https://www.w3.org/TR/WCAG21/), especially [the “Adapting text” section](https://www.w3.org/TR/WCAG21/#adapting-text), can serve as a starting point to choose the values you might at least offer to users for those settings.

We’ve been trying to define a baseline for a11y-related user settings, and you’ll find further information and extra features you might want to implement in the “Baseline for a11y-related user settings” document (e.g. alternated colors for lines of text or words, etc.). 

### On typefaces themselves

Although typefaces specifically designed for dyslexia exist, users might prefer system fonts they are used to. [The following system typefaces are almost always listed as good for dyslexia](http://dyslexiahelp.umich.edu/sites/default/files/good_fonts_for_dyslexia_study.pdf): 

- Arial/Helvetica
- Trebuchet MS
- Verdana

Those fonts are available on both Windows and MacOS/iOS so it would be a good idea to have at least 2 of them available in the typeface user setting. We are recommending extra options in the [Open Source and Libre Fonts](../docs/CSS10-libre_fonts.md#recommended-fonts-for-accessibility) though, in case you want to provide users with more specific fonts.

More importantly, preference is a key factor, and implementers might want to give access to user-installed fonts if the platform allows. When doing research, some participants indeed emphasized they installed a specific font for dyslexia, and they want to be able to use it in the reading app as well.

### Conclusion

It’s not just about fonts, it’s about typography and user settings allowing its customization. 

At the very minimum, you might want to provide: 

- 2–3 system fonts listed as good for dyslexia;
- a font specifically designed for dyslexia;
- a reliable `font-size` setting (i.e. that will work for all reflowable EPUB files);
- a `line-height` setting;
- a `text-align` setting;
- a `letter-spacing` setting.

It is noteworthy that everything extra will have to be implemented with great care: too many settings might create a cognitive overload for some users, so UX research should be conducted accordingly.

## Math

This is the font-stack used by Webkit, it tries to leverage fonts users might have installed via LaTeX.

```
font-family: "Latin Modern Math", "STIX Two Math", "XITS Math", "STIX Math", "Libertinus Math", "TeX Gyre Termes Math", "TeX Gyre Bonum Math", "TeX Gyre Schola", "DejaVu Math TeX Gyre", "TeX Gyre Pagella Math", "Asana Math", "Cambria Math", "Lucida Bright Math", "Minion Math", STIXGeneral, STIXSizeOneSym, Symbol, "Times New Roman", serif;
```

## EBPAJ patch for Japanese

The EBPAJ template only references fonts from MS Windows so Readium CSS has to reference fonts from other platforms and override authors’ stylesheets. What this patch does is to keep the default value used in EBPAJ templates and provide fallbacks for all platforms.

Implementers might want to load this patch only if they find one of the following metadata items in the OPF package:

- version 1: `<dc:description id="ebpaj-guide">ebpaj-guide-1.0</dc:description>`
- version 1.1: `<meta property="ebpaj:guide-version">1.1</meta>`

Since `@font-face` must be used to align with EBPAJ’s specific implementation (rendering engines have to go through 9–11 `local` sources in the worst-case scenario), implementers should expect a performance debt.