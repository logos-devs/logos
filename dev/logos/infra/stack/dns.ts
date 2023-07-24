import { App, Environment, Stack, StackProps } from "aws-cdk-lib";
import { PublicHostedZone } from "aws-cdk-lib/aws-route53";

class DnsStack extends Stack {
    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);

        new PublicHostedZone(this, 'logos-r53-hosted-zone', {
            zoneName: 'dev.logos.dev',
        });
    }
}

export function makeDnsStack(app: App, id: string, env: Environment): DnsStack {
    return new DnsStack(app, id, { env })
}