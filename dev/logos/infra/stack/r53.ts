import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {PublicHostedZone} from "aws-cdk-lib/aws-route53";

export class R53Stack extends Stack {
    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);

        new PublicHostedZone(this, 'logos-r53-hosted-zone', {
            zoneName: 'dev.logos.dev',
        });
    }
}

export function makeR53Stack(app: App, id: string, env: Environment): R53Stack {
    return new R53Stack(app, id, { env })
}