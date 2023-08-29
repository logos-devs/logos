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

            const userPoolDomain = new UserPoolDomain(this, `${id}-domain-${loginDomain.replace(/\./g, "-")}`, {
                userPool,
                customDomain: {
                    domainName: loginDomain,
                    certificate: acmStack.loginCertificates[loginDomain]
                }
            })

            new ARecord(this, `${id}-custom-login-dns-record-${fqdnId}`, {
                zone: HostedZone.fromLookup(this, `${id}-zone-lookup-${fqdnId}`, {domainName: fqdn}),
                recordName: "login",
                target: RecordTarget.fromAlias(
                    new UserPoolDomainTarget(userPoolDomain)
                )
            });

            const redirectUri = `https://${fqdn}/login/complete`;
            const userPoolClient = new UserPoolClient(this, `${id}-user-pool-client-web-${fqdnId}`, {
                userPool,
                oAuth: {
                    callbackUrls: [redirectUri]
                }
            });

            const clientIdOutputId = `${id}-login-url-${fqdnId}`;
            const clientIdOutput = new CfnOutput(this, clientIdOutputId, {
                value: userPoolDomain.signInUrl(userPoolClient, { redirectUri }),
                description: "Sign-in URL"
            });
            clientIdOutput.overrideLogicalId(dashToCamel(clientIdOutputId));
        })
    }
}

export function makeCognitoStack(app: App, id: string, acmStack: AcmStack, env: Environment, apps: LogosApp[]): CognitoStack {
    return new CognitoStack(app, id, acmStack, { env }, apps);
}