import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {PublicHostedZone} from "aws-cdk-lib/aws-route53";
import {LogosApp} from "../construct/logos";

export class R53Stack extends Stack {
    readonly id: string;
    readonly stackDomain: string;
    readonly stackZone: PublicHostedZone;
    readonly zones: Array<PublicHostedZone>;
    readonly appZones: Array<PublicHostedZone>;

    private zoneId(fqdn: string) {
        return `${this.id}-${fqdn.replace(/\./g, '-')}`;
    }

    constructor(scope: App, id: string, stackDomain: string, props: StackProps, apps: LogosApp[]) {
        super(scope, id, props);

        this.id = id;
        this.appZones = [];
        this.stackDomain = stackDomain;
        this.stackZone = new PublicHostedZone(this, this.zoneId(stackDomain), {
            caaAmazon: true,
            zoneName: stackDomain,
        });

        this.zones = [this.stackZone].concat(
            apps.map(app => {
                const
                    fqdn = app.getFQDN(),
                    zoneId = this.zoneId(fqdn),
                    zone = new PublicHostedZone(this, zoneId, {
                        zoneName: fqdn,
                    });

                this.appZones.push(zone);

                return zone;
            })
        );
    }
}

export function makeR53Stack(app: App, id: string, stackDomain: string, env: Environment, apps: LogosApp[]): R53Stack {
    return new R53Stack(app, id, stackDomain, { env }, apps)
}