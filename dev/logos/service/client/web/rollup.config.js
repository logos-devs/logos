import terser from '@rollup/plugin-terser';
import commonjs from '@rollup/plugin-commonjs';
import includePaths from 'rollup-plugin-includepaths';
import json from '@rollup/plugin-json';
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
        nodeResolve(),
        includePaths({paths: ["./"]}),
        terser(),
        json()
    ],
    output: {
        format: 'esm',
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