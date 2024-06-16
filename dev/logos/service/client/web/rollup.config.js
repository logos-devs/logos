import terser from '@rollup/plugin-terser';
import commonjs from '@rollup/plugin-commonjs';
import includePaths from 'rollup-plugin-includepaths';
import json from '@rollup/plugin-json';
import {makeHtmlAttributes} from '@rollup/plugin-html';
import html from '@rollup/plugin-html';
import replace from '@rollup/plugin-replace';
import nodeResolve from '@rollup/plugin-node-resolve';


const allowedWarnings = {
    'THIS_IS_UNDEFINED': [
        /.*/
    ],
    'CIRCULAR_DEPENDENCY': [
        /\/inversify@6.0.2\//,
    ],
    'EVAL': [
        /\/google-libphonenumber@3.2.34\//,
        /\/google-protobuf@3.21.2\//,
    ]
};

export default {
    plugins: [
        commonjs(), // needed by google-libphonenumber
        nodeResolve({
            browser: true
        }),
        includePaths({paths: ["./"]}),
        replace({
            preventAssignment: true,
            values: {
                'process.env.NODE_ENV': JSON.stringify('production')
            }
        }),
        //terser(),
        json(),
        html({
            title: "logos",
            publicPath: "/",
            async template({attributes, files, meta, publicPath, title}) {
                const scripts = (files.js || [])
                    .map(({fileName}) => {
                        const attrs = makeHtmlAttributes(attributes.script);
                        return `<script src="${publicPath}${fileName}"${attrs}></script>`;
                    })
                    .join('\n');

                const links = (files.css || [])
                    .map(({fileName}) => {
                        const attrs = makeHtmlAttributes(attributes.link);
                        return `<link href="${publicPath}${fileName}" rel="stylesheet"${attrs}>`;
                    })
                    .join('\n');

                const metas = meta
                    .map((input) => {
                        const attrs = makeHtmlAttributes(input);
                        return `<meta${attrs}>`;
                    })
                    .join('\n');

                return `
<!doctype html>
<html${makeHtmlAttributes(attributes.html)}>
  <head>
    <meta charset="utf-8">
    <title>summer.app</title>
    <link rel="preconnect" href="https://fonts.gstatic.com">
    <link href="https://fonts.googleapis.com/css2?family=Fira+Code&family=Roboto+Mono:wght@300;400;500&display=swap"
          rel="stylesheet">
    <link href="https://fonts.googleapis.com/css?family=Material+Icons&display=block" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Symbols+Outlined" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css?family=Roboto:300,400,500" rel="stylesheet">

    <meta charset="utf-8">
    <meta content="width=device-width, initial-scale=1" name="viewport">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="translucent">
    <meta name="referrer" content="no-referrer">
    <meta name="theme-color" content="#ffffff" media="(prefers-color-scheme: light)">
    <meta name="theme-color" content="#292929" media="(prefers-color-scheme: dark)">

    <style>
        html, body {
            height: 100%;
        }

        @media (prefers-color-scheme: dark) {
            body {
                /*background: #333;*/
                /*color: white;*/
                margin: 0;
                /*background: linear-gradient(180deg, #0f0c29, #302b63, #24243e, #8a5023);*/
                background-attachment: fixed !important;
                background-size: 100vw 100vh;
            }
        }

        @media (prefers-color-scheme: light) {
            body {
                background: white;
                color: #555;
            }
        }

        :root {
            --font-user-message: sans-serif;
        }

        html {
            margin: 0;
        }

        html, body {
            margin: 0;
        }

        body::-webkit-scrollbar {
            display: none;
        }
    </style>
    
    

    ${metas}
    <title>${title}</title>
    ${links}
  </head>
  <body>
    ${scripts}
  </body>
</html>`;
            }
        })
    ],
    output: {
        format: 'es',
        sourcemap: true,
    },
    onwarn(warning, warn) {
        const allowed = allowedWarnings[warning.code];

        if (allowed && (warning.loc === undefined || allowed.some((regex) => regex.test(warning.loc.file)))) {
            warn(warning);
            return;
        }
        if (warning.loc) {
            console.warn(warning.loc.file);
        }
        throw new Error(warning.code + ' : ' + warning.message);
    }
};