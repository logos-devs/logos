#!/usr/bin/env node

import {App} from 'aws-cdk-lib';
import * as AWS from 'aws-sdk';
import {Logos} from './construct/logos';

const app = new App();

new Logos(app, 'logos', {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: AWS.config.region
});

app.synth();