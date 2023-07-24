#!/usr/bin/env node

import { App } from 'aws-cdk-lib';
import { Logos } from './construct/logos';

const app = new App();

new Logos(app, 'logos', {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION
});

app.synth();