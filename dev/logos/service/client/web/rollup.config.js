import terser from '@rollup/plugin-terser';
import commonjs from '@rollup/plugin-commonjs';
import includePaths from 'rollup-plugin-includepaths';
import json from '@rollup/plugin-json';
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
        terser(),
        json(),
        html()
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