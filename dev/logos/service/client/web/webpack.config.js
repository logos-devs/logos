// TODO : move webpack and library scripts to //stack/web

const HtmlWebpackPlugin = require('html-webpack-plugin');
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin');
const path = require('path');

module.exports = (env, argv) => ({
  mode: env.production ? 'production' : 'development',
  entry: path.resolve(__dirname,
      '/dev/logos/service/client/web/index.ts'),
  experiments: {
    topLevelAwait: true
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.(png|jpe?g|gif|jp2|webp)$/,
        loader: 'file-loader',
        options: {
          name: '[name]_[contenthash].[ext]',
        },
      },
    ],
  },
  devtool: env.production ? "source-map" : "eval-source-map",
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, 'index.html'),
      publicPath: "/"
    }),
  ],
  output: {
    filename: '[name]_[contenthash].js',
    path: path.resolve(__dirname, 'dist')
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
    plugins: [
      new TsconfigPathsPlugin(
          {
            configFile: path.resolve(__dirname,
                "../../../../../tsconfig.json")
          }
      ),
    ],
  },
  devServer: {
    client: {
      webSocketURL: 'wss://dev.summer.app:443/ws',
    },
    host: '0.0.0.0',
    port: 8080,
    allowedHosts: ['all'],
    historyApiFallback: {
      index: '/index.html',
      disableDotRule: true,
    }
  },
  watchOptions: {
    followSymlinks: true,
    // TODO is this necessary?
    ignored: [
      '**/node_modules/**',
      '**/node_modules',
    ],
    poll: 2000,
  }
});
