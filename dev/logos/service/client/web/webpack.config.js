// TODO : move webpack and library scripts to //stack/web

const HtmlWebpackPlugin = require('html-webpack-plugin');
const path = require('path');

module.exports = (env, argv) => ({
    mode: env.production ? 'production' : 'development',
    entry: path.resolve(__dirname, 'index.js'),
    experiments: {
        topLevelAwait: true
    },
    module: {
        rules: [
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