# Order in which to append Readium CSS reflowable stylesheets

[Implementers’ doc] [Authors’ info]

Readium CSS is leveraging the cascade in order to provide authors with defaults, paginate contents and apply user overrides and themes. As a consequence, there is a specific order in which reflowable stylesheets must be added. Indeed, conformance with [CSS Cascading and Inheritance Level 3](https://www.w3.org/TR/css3-cascade/) requires that we make authors’ stylesheets an integral part of our cascade.

## Dist stylesheets

Inject ReadiumCSS `dist` stylesheets into the `<head>` of (X)HTML documents. 

1. `ReadiumCSS-before.css`
2. Authors’ stylesheet(s) or `ReadiumCSS-default.css` for unstyled publications
3. `ReadiumCSS-after.css`

For RTL, you would then have to load the stylesheets in the `rtl` folder. Same for CJK. Check the [“Internationalization” doc](../docs/CSS16-internationalization.md) for guidance.

By default, we inject all stylesheets on load and rely on custom properties (a.k.a. CSS variables) set on `html` to apply user settings.

## Src modules

If you want to customize `dist` stylesheets, you’ll have to respect the following guidelines and rebuild `dist` stylesheets.

### Insert before the author’s stylesheets

The following modules must be inserted before the author’s stylesheets (`ReadiumCSS-before.css`), in this exact order: 

1. `ReadiumCSS-config.css`
2. `ReadiumCSS-base.css`
3. `ReadiumCSS-day_mode.css`
4. `ReadiumCSS-fonts.css`
5. `ReadiumCSS-html5patch.css`
6. `ReadiumCSS-safeguards.css` (or `ReadiumCSS-safeguards-vertical.css` for CJK – vertical writing mode)

### Append if there is no author’s styles

The following modules must be appended if there is no external stylesheet (`<link>`), internal stylesheet (`<style>`) or inline styles in the document (`style=" "`), in this exact order: 

1. `ReadiumCSS-default.css`

This default must be appended before all other modules in the next section.

### Append after the author’s stylesheets

The following modules must be appended after the author’s stylesheets (`ReadiumCSS-after.css`), ideally in this order: 

1. `ReadiumCSS-config.css`
2. `ReadiumCSS-pagination.css` (or `ReadiumCSS-pagination-vertical.css` for CJK – vertical writing mode)
3. `ReadiumCSS-scroll.css` (or `ReadiumCSS-scroll-vertical.css` for CJK – vertical writing mode)
4. `ReadiumCSS-highlights.css`
5. `ReadiumCSS-night_mode.css`
6. `ReadiumCSS-sepia_mode.css`
7. `ReadiumCSS-os_a11y.css`
8. User settings:
    1. `ReadiumCSS-colNumber_pref.css` (does not apply to CJK – vertical writing mode)
    2. `ReadiumCSS-pageMargins_pref.css` (or `ReadiumCSS-pageMargins-vertical_pref.css` for CJK – vertical writing mode)
    3. `ReadiumCSS-customColors_pref.css`
    4. `ReadiumCSS-textAlign_pref.css` (does not apply to CJK scripts)
    5. `ReadiumCSS-bodyHyphens_pref.css` (does not apply to CJK scripts)
    6. `ReadiumCSS-fontFamily_pref.css`
    7. `ReadiumCSS-a11yFont_pref.css` (does not apply to RTL and CJK scripts)
    8. `ReadiumCSS-fontSize_pref.css`
    9. `ReadiumCSS-lineHeight_pref.css`
    10. `ReadiumCSS-paraSpacing_pref.css` (or `ReadiumCSS-paraSpacing-vertical_pref.css` for CJK – vertical writing mode)
    11. `ReadiumCSS-paraIndent_pref.css` (does not apply to CJK scripts)
    12. `ReadiumCSS-wordSpacing_pref.css` (does not apply to RTL and CJK scripts)
    13. `ReadiumCSS-letterSpacing_pref.css` (does not apply to RTL and CJK scripts)
    14. `ReadiumCSS-arabicLigatures_pref.css` (RTL only)
9. `ReadiumCSS-fs_normalize.css`