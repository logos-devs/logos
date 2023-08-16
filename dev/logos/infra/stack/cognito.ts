import {App, CfnOutput, Environment, Stack, StackProps} from "aws-cdk-lib";
import {UserPool, UserPoolClient, UserPoolDomain} from "aws-cdk-lib/aws-cognito";
import {ARecord, HostedZone, RecordTarget} from "aws-cdk-lib/aws-route53";
import {UserPoolDomainTarget} from "aws-cdk-lib/aws-route53-targets";
import {AcmStack} from "./acm";
import {LogosApp} from "../construct/logos";

function dashToCamel(s: string): string {
    return s.replace(/(-\w)/g, (match) => match[1].toUpperCase());
}

export class CognitoStack extends Stack {
    constructor(scope: App, id: string, acmStack: AcmStack, props: StackProps, apps: LogosApp[]) {
        super(scope, id, props);

        apps.map(app => {
            const
                fqdn = app.getFQDN(),
                fqdnId = fqdn.replace(/\./g, "-"),
                userPoolId = `${id}-user-pool-${fqdnId}`,
                userPool = new UserPool(this, userPoolId, {
                    userPoolName: userPoolId,
                    signInCaseSensitive: false,
                    selfSignUpEnabled: true,
                    signInAliases: {
                        email: true,
                        phone: true
                    },
                    keepOriginal: {
                        email: true,
                        phone: true
                    },
                    mfaSecondFactor: {
                        sms: true,
                        otp: true,
                    },
                }),
                loginDomain = `login.${fqdn}`;

            new ARecord(this, `${id}-custom-login-dns-record-${fqdnId}`, {
                zone: HostedZone.fromLookup(this, `${id}-zone-lookup-${fqdnId}`, {domainName: fqdn}),
                recordName: "login",
                target: RecordTarget.fromAlias(
                    new UserPoolDomainTarget(
                        new UserPoolDomain(this, `${id}-domain-${loginDomain.replace(/\./g, "-")}`, {
                            userPool,
                            customDomain: {
                                domainName: loginDomain,
                                certificate: acmStack.loginCertificates[loginDomain]
                            }
                        })
                    )
                )
            });

            const userPoolClient = new UserPoolClient(this, `${id}-user-pool-client-web-${fqdnId}`, {
                userPool
            });
            const clientIdOutputId = `${id}-user-pool-client-id-${fqdnId}`;
            const clientIdOutput = new CfnOutput(this, clientIdOutputId, {
                value: userPoolClient.userPoolClientId,
                description: "App client ID to use when linking to the Cognito hosted web login UI"
            });
            clientIdOutput.overrideLogicalId(dashToCamel(clientIdOutputId));
        })
    }
}

export function makeCognitoStack(app: App, id: string, acmStack: AcmStack, env: Environment, apps: LogosApp[]): CognitoStack {
    return new CognitoStack(app, id, acmStack, { env }, apps);
}