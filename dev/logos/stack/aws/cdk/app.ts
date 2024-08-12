#!/usr/bin/env node

import 'source-map-support/register';
import {App} from 'aws-cdk-lib';
import {Logos} from './construct/logos';

const app = new App();

new Logos(app, 'logos', {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: 'us-east-2'
});

app.synth();