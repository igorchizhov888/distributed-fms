console.log("config-overrides.js is being executed");
const webpack = require('webpack');

module.exports = function override(config) {
  const fallback = config.resolve.fallback || {};
  Object.assign(fallback, {
    "stream": require.resolve("stream-browserify"),
    "util": require.resolve("util/"),
    "zlib": require.resolve("browserify-zlib"),
    "http": require.resolve("stream-http"),
    "url": require.resolve("url/"),
    "os": require.resolve("os-browserify/browser"),
    "path": require.resolve("path-browserify"),
    "assert": require.resolve("assert/"),
    "fs": false,
    "net": false,
    "tls": false,
    "http2": false,
    "dns": false
  });
  config.resolve.fallback = fallback;

  config.plugins = (config.plugins || []).concat([
    new webpack.ProvidePlugin({
      process: 'process/browser',
      Buffer: ['buffer', 'Buffer'],
    }),
  ]);

  return config;
};