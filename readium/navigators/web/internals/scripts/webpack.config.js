const path = require("path")
const CopyPlugin = require("copy-webpack-plugin")

module.exports = {
  mode: "production",
  devtool: "source-map",
  entry: {
    "fixed-single-script": "./src/index-fixed-single.ts",
    "fixed-double-script": "./src/index-fixed-double.ts",
    "fixed-injectable-script": "./src/index-fixed-injectable.ts",
    "reflowable-injectable-script": "./src/index-reflowable-injectable.ts",
  },
  resolve: {
    // Add '.ts' and '.tsx' as resolvable extensions.
    extensions: ["", ".webpack.js", ".web.js", ".ts", ".tsx", ".js"],
  },
  module: {
    rules: [
      // All files with a '.ts' or '.tsx' extension will be handled by 'ts-loader'.
      {
        test: /\.tsx?$/,
        loader: "ts-loader",
      },
      {
        test: /\.m?js$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader",
          options: {
            presets: ["@babel/preset-env"],
          },
        },
      },
    ],
  },
  plugins: [
    new CopyPlugin({
      patterns: [
        { from: "public" },
        { from: "node_modules/@readium/css/css/dist", to: "readium-css" },
      ],
    }),
  ],
}
