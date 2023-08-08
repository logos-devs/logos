import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Certificate, CertificateValidation} from "aws-cdk-lib/aws-certificatemanager";

export class AcmStack extends Stack {
    readonly certificate: Certificate;

    private makeCert(id: string, domain: string): Certificate {
        return new Certificate(this, `${id}-${domain.replace(/\./g, '-')}`, {
            domainName: domain,
            validation: CertificateValidation.fromEmail()
        })
    }

    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);
        this.certificate = this.makeCert(id, "dev.digits.rip");
    }
}

export function makeAcmStack(app: App, id: string, env: Environment): AcmStack {
    return new AcmStack(app, id, { env })
}
