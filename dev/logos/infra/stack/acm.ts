import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Certificate, CertificateValidation} from "aws-cdk-lib/aws-certificatemanager";
import {R53Stack} from "./r53";

export class AcmStack extends Stack {
    readonly certificate: Certificate;

    constructor(scope: App, id: string, r53Stack: R53Stack, props?: StackProps) {
        super(scope, id, props);

        const domain = r53Stack.stackZone.zoneName;

        this.certificate = new Certificate(this, `${id}-cert-${domain.replace(/\./g, '-')}`, {
            domainName: domain,
            validation: CertificateValidation.fromEmail(),
            subjectAlternativeNames: r53Stack.appZones.map(zone => zone.zoneName)
        })
    }
}

export function makeAcmStack(app: App, id: string, r53Stack: R53Stack, env: Environment): AcmStack {
    return new AcmStack(app, id, r53Stack, { env })
}
