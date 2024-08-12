import {App, CfnOutput, Duration, Environment, SecretValue, Stack, StackProps} from "aws-cdk-lib";
import {UserPool, UserPoolClient, UserPoolDomain} from "aws-cdk-lib/aws-cognito";
import {PolicyStatement} from "aws-cdk-lib/aws-iam";
import {ARecord, HostedZone, RecordTarget} from "aws-cdk-lib/aws-route53";
import {UserPoolDomainTarget} from "aws-cdk-lib/aws-route53-targets";
import {Secret} from "aws-cdk-lib/aws-secretsmanager";
import {LogosApp} from "../construct/logos";
import {IamStack} from "./iam";
import {AcmStack} from "./acm";


function dashToCamel(s: string): string {
    return s.replace(/(-\w)/g, (match) => match[1].toUpperCase());
}

type HostPublicParams = { baseUrl: string, loginUrl: string, redirectUrl: string };
type HostSecretParams = { clientCredentialsSecretArn: string } & HostPublicParams;

export class CognitoStack extends Stack {
    constructor(scope: App, id: string, acmStack: AcmStack, iamStack: IamStack, props: StackProps, apps: LogosApp[]) {
        super(scope, id, props);

        const
            cognitoPublicHostMapOutput: { [key: string]: HostPublicParams } = {},
            cognitoSecretHostMapOutput: { [key: string]: HostSecretParams } = {},
            cognitoHostsOutput: string[] = [];

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
                },
                accessTokenValidity: Duration.hours(8),
            });

            cognitoPublicHostMapOutput[fqdn] = {
                baseUrl: userPoolDomain.baseUrl(),
                loginUrl: userPoolDomain.signInUrl(userPoolClient, {redirectUri}),
                redirectUrl: redirectUri
            };

            const clientCredentialsSecret: Secret = new Secret(this, `${id}-client-credentials-${fqdnId}`, {
                secretObjectValue: {
                    clientId: SecretValue.unsafePlainText(userPoolClient.userPoolClientId),
                    clientSecret: userPoolClient.userPoolClientSecret,
                },
            });

            cognitoSecretHostMapOutput[app.getFQDN()] = {
                ...cognitoPublicHostMapOutput[fqdn],
                clientCredentialsSecretArn: clientCredentialsSecret.secretArn
            }

            iamStack.serviceAccount.addToPrincipalPolicy(
                new PolicyStatement({
                    actions: ["secretsmanager:GetSecretValue"],
                    resources: [clientCredentialsSecret.secretArn]
                })
            )

            cognitoHostsOutput.push(fqdn);
        });

        const publicHostMapOutputId = `${id}-public-host-map`;
        new CfnOutput(this, publicHostMapOutputId, {
            value: JSON.stringify(cognitoPublicHostMapOutput),
        }).overrideLogicalId(dashToCamel(publicHostMapOutputId));

        const secretHostMapOutputId = `${id}-server-config`;
        new CfnOutput(this, secretHostMapOutputId, {
            value: JSON.stringify(cognitoSecretHostMapOutput),
        }).overrideLogicalId(dashToCamel(secretHostMapOutputId));

        const hostsOutputId = `${id}-hosts`;
        new CfnOutput(this, hostsOutputId, {
            value: JSON.stringify(cognitoHostsOutput),
        }).overrideLogicalId(dashToCamel(hostsOutputId));
    }
}

export function makeCognitoStack(app: App, id: string, acmStack: AcmStack, iamStack: IamStack, env: Environment, apps: LogosApp[]): CognitoStack {
    return new CognitoStack(app, id, acmStack, iamStack, {env}, apps);
}