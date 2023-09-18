
'use strict'

if (process.env.NODE_ENV === 'production') {
  module.exports = require('./kotlin-navigator-web.cjs.production.min.js')
} else {
  module.exports = require('./kotlin-navigator-web.cjs.development.js')
}
