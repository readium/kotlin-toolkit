# User Overrides’ Classification

[Implementers’ doc] [Authors’ info]

In order to help you manage user settings, we defined 4 types of overrides: 

1. chrome (UI);
2. chrome advanced;
3. user overrides;
4. user overrides advanced.

Those types can help you decide which settings should be applied to every book, which should be implemented based on your UX approach or the first to be displayed in the customization panel, etc.

## Chrome

The following settings are required and should be applied by any means necessary:

- reading modes;
- paged/scrolled view.

## Chrome advanced

The following settings are optional but should be applied by any means necessary, without any flag, if they are provided to users:

- number of columns;
- page margins;
- custom reading modes (background and text color);
- filters (darken and invert for night mode).

## User overrides

The following settings are required and should be applied with their required flag by any means necessary:

- font-family (requires `:--font-overide` flag);
- font-size (requires `:--advanced-settings` flag).

## User overrides advanced

The following settings are optional but should be applied by any means necessary, with the `:--advanced-setting` flag, if they are provided to users:

- line-height;
- text-align and hyphens (those two should probably work together);
- paragraph spacing;
- paragraph indent;
- letter-spacing;
- word-spacing;
- arabic ligatures;
- accessibility normalization;
- type scale.

Please note this classification might change and additional items be added in the future.