module.exports = (ctx) => ({
  map: {
    inline: false
  },
  plugins: [
    require('postcss-import')({
      root: ctx.file.dirname
    }),
    require('postcss-custom-media')({}),
    require('postcss-custom-selectors')({}),
//    require('postcss-discard-comments')({}),
    require("stylelint")({
      "fix": true,
      "config": {
        "defaultSeverity": "warning",
        "rules": {
            "color-no-invalid-hex": true
          , "comment-no-empty": [ true, {
              "message": "Please remove empty comments."
          }]
          , "number-max-precision": 4
          , "unit-blacklist": [ ["pt"], {
              "message": "Sorry, this unit is not allowed. Please use another one."
            , "severity": "error"
          }]
          , "custom-property-pattern": [ "(RS|USER)__.+", {
              "message": "It looks like you’re using a CSS variable prefix which is not supported. It should either start with “--RS__” or “--USER__”."
            , "severity": "error"
          }]
          , "selector-max-empty-lines": 0
          , "color-hex-length": [ "long", {
              "message": "We recommend using long color HEX to prevent unexpected issues."
          }]
          , "font-family-name-quotes": [ "always-where-recommended", {
              "message": "If looks like there are spaces or digits in your “font-family”, please use quotes."
          }]
          , "function-url-quotes": "always"
          , "number-leading-zero": "always"
          , "number-no-trailing-zeros": true
          , "length-zero-no-unit": [ true, {
              "message": "The value of this property being 0, you don’t need an unit. Please remove it."
          }]
          , "unit-case": "lower"
          , "selector-attribute-quotes": "always"
          , "comment-whitespace-inside": "always"
          , "max-empty-lines": 1
          , "indentation": [ 2, {
              "message": "You should use 2 spaces to indent."
          }]
          , "no-duplicate-at-import-rules": true
          , "no-extra-semicolons": true
          , "no-invalid-double-slash-comments": [ true, {
              "message": "It looks like you’re using single-line JS comments. This is CSS, you can’t use that."
            , "severity": "error"
          }]
          , "max-nesting-depth": [ 0, {
              "message": "We’re using Vanilla CSS with PostCSS and our current configuration doesn’t allow nesting selectors as in LESS or SASS."
            , "severity": "error"
          }]
          , "string-quotes": "double"
          , "declaration-bang-space-before": [ "always", {
              "message": "Please put a space before !important as it will improve readability."
          }]
          , "block-closing-brace-newline-after": "always"
          , "block-opening-brace-newline-after": "always"
          , "selector-list-comma-newline-after": "always-multi-line"
        }
      }
    }),
    require('postcss-sorting')({
      'properties-order': [
          'object-fit'
        , 'position'
        , 'top'
        , 'right'
        , 'bottom'
        , 'left'
        , 'z-index'
        , 'display'
        , 'float'
        , 'columns'
        , 'column-width'
        , 'column-count'
        , 'column-gap'
        , 'column-fill'
        , 'width'
        , 'height'
        , 'max-width'
        , 'max-height'
        , 'min-width'
        , 'min-height'
        , 'padding'
        , 'padding-top'
        , 'padding-right'
        , 'padding-bottom'
        , 'padding-left'
        , 'margin'
        , 'margin-top'
        , 'margin-right'
        , 'margin-bottom'
        , 'margin-left'
        , 'margin-collapse'
        , 'margin-top-collapse'
        , 'margin-right-collapse'
        , 'margin-bottom-collapse'
        , 'margin-left-collapse'
        , 'overflow'
        , 'overflow-x'
        , 'overflow-y'
        , 'clip'
        , 'clear'
        , 'font'
        , 'font-family'
        , 'font-size'
        , 'font-smoothing'
        , 'font-style'
        , 'font-weight'
        , 'hyphens'
        , 'widows'
        , 'orphans'
        , 'src'
        , 'line-height'
        , 'letter-spacing'
        , 'word-spacing'
        , 'color'
        , 'text-align'
        , 'text-align-last'
        , 'text-decoration'
        , 'text-indent'
        , 'text-overflow'
        , 'text-rendering'
        , 'text-size-adjust'
        , 'text-shadow'
        , 'text-transform'
        , 'word-break'
        , 'word-wrap'
        , 'white-space'
        , 'vertical-align'
        , 'list-style'
        , 'list-style-type'
        , 'list-style-position'
        , 'list-style-image'
        , 'pointer-events'
        , 'cursor'
        , 'background'
        , 'background-attachment'
        , 'background-color'
        , 'background-image'
        , 'background-position'
        , 'background-repeat'
        , 'background-size'
        , 'border'
        , 'border-collapse'
        , 'border-top'
        , 'border-right'
        , 'border-bottom'
        , 'border-left'
        , 'border-color'
        , 'border-image'
        , 'border-top-color'
        , 'border-right-color'
        , 'border-bottom-color'
        , 'border-left-color'
        , 'border-spacing'
        , 'border-style'
        , 'border-top-style'
        , 'border-right-style'
        , 'border-bottom-style'
        , 'border-left-style'
        , 'border-width'
        , 'border-top-width'
        , 'border-right-width'
        , 'border-bottom-width'
        , 'border-left-width'
        , 'border-radius'
        , 'border-top-right-radius'
        , 'border-bottom-right-radius'
        , 'border-bottom-left-radius'
        , 'border-top-left-radius'
        , 'border-radius-topright'
        , 'border-radius-bottomright'
        , 'border-radius-bottomleft'
        , 'border-radius-topleft'
        , 'content'
        , 'quotes'
        , 'outline'
        , 'outline-offset'
        , 'opacity'
        , 'filter'
        , 'visibility'
        , 'size'
        , 'zoom'
        , 'transform'
        , 'box-align'
        , 'box-flex'
        , 'box-orient'
        , 'box-pack'
        , 'box-shadow'
        , 'box-sizing'
        , 'table-layout'
        , 'animation'
        , 'animation-delay'
        , 'animation-duration'
        , 'animation-iteration-count'
        , 'animation-name'
        , 'animation-play-state'
        , 'animation-timing-function'
        , 'animation-fill-mode'
        , 'transition'
        , 'transition-delay'
        , 'transition-duration'
        , 'transition-property'
        , 'transition-timing-function'
        , 'background-clip'
        , 'backface-visibility'
        , 'resize'
        , 'appearance'
        , 'user-select'
        , 'interpolation-mode'
        , 'direction'
        , 'marks'
        , 'page'
        , 'column-break-after'
        , 'page-break-after'
        , 'break-after'
        , 'column-break-before'
        , 'page-break-before'
        , 'break-before'
        , 'column-break-inside'
        , 'page-break-inside'
        , 'break-inside'
        , 'unicode-bidi'
        , 'speak'
      ],
      'unspecified-properties-position': 'bottomAlphabetical'
    })
  ]
})