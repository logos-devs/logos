import {App, CfnOutput, Environment, SecretValue, Stack, StackProps} from "aws-cdk-lib";
import {UserPool, UserPoolClient, UserPoolDomain} from "aws-cdk-lib/aws-cognito";
import {ARecord, HostedZone, RecordTarget} from "aws-cdk-lib/aws-route53";
import {UserPoolDomainTarget} from "aws-cdk-lib/aws-route53-targets";
import {Secret} from "aws-cdk-lib/aws-secretsmanager";
import {LogosApp} from "../construct/logos";
import {AcmStack} from "./acm";


function dashToCamel(s: string): string {
    return s.replace(/(-\w)/g, (match) => match[1].toUpperCase());
}

export class CognitoStack extends Stack {
    constructor(scope: App, id: string, acmStack: AcmStack, props: StackProps, apps: LogosApp[]) {
        super(scope, id, props);

        const cognitoPublicHostMapOutput = {};
        const cognitoSecretHostMapOutput = {};

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
                generateSecret: true,
                oAuth: {
                    callbackUrls: [redirectUri],
                }
            });

            cognitoPublicHostMapOutput[fqdn] = {
                baseUrl: userPoolDomain.baseUrl(),
                loginUrl: userPoolDomain.signInUrl(userPoolClient, { redirectUri }),
                redirectUrl: redirectUri
            };

            const clientCredentialsSecret = new Secret(this, `${id}-client-credentials-${fqdnId}`, {
                secretObjectValue: {
                    clientId: SecretValue.unsafePlainText(userPoolClient.userPoolClientId),
                    clientSecret: userPoolClient.userPoolClientSecret,
                },
            });

            cognitoSecretHostMapOutput[app.getFQDN()] = {
                ...cognitoPublicHostMapOutput[fqdn],
                clientCredentialsSecretArn: clientCredentialsSecret.secretArn
            }
        });

        const publicHostMapOutputId = `${id}-public-host-map`;
        new CfnOutput(this, publicHostMapOutputId, {
            value: JSON.stringify(cognitoPublicHostMapOutput),
        }).overrideLogicalId(dashToCamel(publicHostMapOutputId));

        const secretHostMapOutputId = `${id}-secret-host-map`;
        new CfnOutput(this, secretHostMapOutputId, {
            value: JSON.stringify(cognitoSecretHostMapOutput),
        }).overrideLogicalId(dashToCamel(secretHostMapOutputId));
    }
}

export function makeCognitoStack(app: App, id: string, acmStack: AcmStack, env: Environment, apps: LogosApp[]): CognitoStack {
    return new CognitoStack(app, id, acmStack, { env }, apps);
}