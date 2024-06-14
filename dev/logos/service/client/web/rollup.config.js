import commonjs from '@rollup/plugin-commonjs';
import terser from '@rollup/plugin-terser';
import includePaths from 'rollup-plugin-includepaths';
import json from '@rollup/plugin-json';
import nodeResolve from '@rollup/plugin-node-resolve';

export default {
    plugins: [
        commonjs(),
        nodeResolve(),
        includePaths({paths: ["./"]}),
        terser(),
        json()
    ],
    output: {
        format: 'esm',
        sourcemap: true,
        //chunkFileNames: '[name]-[hash].js',
    },
};