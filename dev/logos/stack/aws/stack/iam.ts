import {EksBlueprint} from "@aws-quickstart/eks-blueprints";
import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {ServiceAccount} from "aws-cdk-lib/aws-eks";
import {PolicyStatement} from "aws-cdk-lib/aws-iam";
import {RdsStack} from "./rds";

export class IamStack extends Stack {
    readonly serviceAccount: ServiceAccount;

    constructor(scope: App, id: string, eksStack: EksBlueprint, rdsStack: RdsStack, props: StackProps) {
        super(scope, id, props);

        const serviceAccountName = id + '-eks-service-account';
        this.serviceAccount = new ServiceAccount(
            this,
            serviceAccountName,
            {
                cluster: eksStack.getClusterInfo().cluster,
                name: serviceAccountName,
                namespace: 'default'
            }
        );

        const env = props.env!;

        this.serviceAccount.addToPrincipalPolicy(
            new PolicyStatement({
                actions: ["rds-db:connect"],
                resources: [
                    `arn:aws:rds-db:${env.region}:${env.account}:dbuser:${rdsStack.databaseCluster.clusterResourceIdentifier}/storage`
                ]
            })
        )

        // TODO this has no business being done here, rather than as a consequence of an app declaring a dependency on this API
        this.serviceAccount.addToPrincipalPolicy(
            new PolicyStatement({
                actions: ["secretsmanager:GetSecretValue"],
                resources: [
                    `arn:aws:secretsmanager:${env.region}:${env.account}:secret:dev/external/openai-??????`
                ]
            })
        )

        this.serviceAccount.addToPrincipalPolicy(
            new PolicyStatement({
                actions: ["secretsmanager:GetSecretValue"],
                resources: [
                    `arn:aws:secretsmanager:${env.region}:${env.account}:secret:dev/external/twilio-??????`
                ]
            })
        )
    }
}

export function makeIamStack(app: App, id: string, eksStack: EksBlueprint, rdsStack: RdsStack, env: Environment): IamStack {
    return new IamStack(app, id, eksStack, rdsStack, {env})
}