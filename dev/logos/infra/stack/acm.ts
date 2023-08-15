import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {
    Certificate,
    CertificateValidation,
    DnsValidatedCertificate
} from "aws-cdk-lib/aws-certificatemanager";
import {R53Stack} from "./r53";

export class AcmStack extends Stack {
    readonly certificate: Certificate;
    readonly loginCertificates: Record<string, Certificate>;

    constructor(scope: App, id: string, r53Stack: R53Stack, props?: StackProps) {
        super(scope, id, props);

        const domain = r53Stack.stackZone.zoneName;

        this.certificate = new Certificate(this, `${id}-cert-${domain.replace(/\./g, '-')}`, {
            domainName: domain,
            validation: CertificateValidation.fromEmail(),
            subjectAlternativeNames: r53Stack.appZones.map(zone => zone.zoneName)
        })
        this.loginCertificates = {};

        r53Stack.appZones.forEach(zone => {
            const
                certDomain = `login.${zone.zoneName}`,
                certId = `${id}-login-cert-${certDomain.replace(/\./g, '-')}`;

            this.loginCertificates[certDomain] = new DnsValidatedCertificate(this, certId, {
                domainName: certDomain,
                hostedZone: zone,
                region: 'us-east-1', // Required for AWS-hosted login with custom domain
            });
        });
    }
}

export function makeAcmStack(app: App, id: string, r53Stack: R53Stack, env: Environment): AcmStack {
    return new AcmStack(app, id, r53Stack, { env })
}
