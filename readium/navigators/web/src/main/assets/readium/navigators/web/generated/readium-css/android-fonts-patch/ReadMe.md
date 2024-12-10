# Android Fonts Patch

This patch is intended to fix a Fixed-Layout issue on Android, and only on this platform. **It doesn’t apply to reflowable EPUBs or any other platform.**

See https://github.com/readium/readium-css/issues/149

## What it does

It declares open source fonts alternatives (Nimbus Roman and Nimbus Sans) as the font-families that generic font-family `serif` and `sans-serif` are usually resolving to on other platforms:

- `Nimbus Roman` as `Times` and `Times New Roman`;
- `Nimbus Sans` as `Helvetica` and `Arial`.

Indeed, these fonts are fully metric compatible while Droid Serif and Roboto are not, which creates issues with text that is absolutely positioned in Fixed-Layout e.g. overlapping or overflowing text, etc.

Unfortunately, it is not possible to assign these fonts to generic font-family `serif` and `sans-serif`, we have to target the font-family names they are resolving to.

## When to load it?

You want to target Fixed-Layout EPUBs on Android devices, in which there is text (not a manga or a *bande dessinée* made of images) but no font embedded in the package. Ideally, you would check whether `Times`, `Times New Roman`, `Arial` or `Helvetica` can be found in stylesheets (`.css`).

Logic is up to implementers.