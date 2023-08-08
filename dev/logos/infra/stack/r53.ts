import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {PublicHostedZone} from "aws-cdk-lib/aws-route53";

export class R53Stack extends Stack {
    readonly zones: Array<PublicHostedZone>;

    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);

        this.zones = [
            "dev.digits.rip",
            "dev.logos.dev",
            "dev.rep.dev"
        ].map(domain => new PublicHostedZone(this, id + domain.replace(/\./g, '-'), {
                zoneName: domain,
            })
        );
    }
}

export function makeR53Stack(app: App, id: string, env: Environment): R53Stack {
    return new R53Stack(app, id, { env })
}