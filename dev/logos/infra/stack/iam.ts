import {EksBlueprint} from "@aws-quickstart/eks-blueprints";
import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {ServiceAccount} from "aws-cdk-lib/aws-eks";
import {PolicyStatement} from "aws-cdk-lib/aws-iam";
import {RdsStack} from "./rds";

export class IamStack extends Stack {
    constructor(scope: App, id: string, eksStack: EksBlueprint, rdsStack: RdsStack, props?: StackProps) {
        super(scope, id, props);

        const serviceAccountName = id + '-eks-service-account';
        const serviceAccount = new ServiceAccount(
            this,
            serviceAccountName,
            {
                cluster: eksStack.getClusterInfo().cluster,
                name: serviceAccountName,
                namespace: 'default'
            }
        );

        serviceAccount.addToPrincipalPolicy(
            new PolicyStatement({
                actions: ["rds-db:connect"],
                resources: [
                    `arn:aws:rds-db:${props.env.region}:${props.env.account}:dbuser:${rdsStack.databaseCluster.clusterResourceIdentifier}/storage`
                ]
            })
        )

        serviceAccount.addToPrincipalPolicy(
            new PolicyStatement({
                actions: ["secretsmanager:GetSecretValue"],
                resources: [
                    `arn:aws:secretsmanager:${props.env.region}:${props.env.account}:secret:dev/external/openai-??????`
                ]
            })
        )
    }
}

export function makeIamStack(app: App, id: string, eksStack, rdsStack, env: Environment): IamStack {
    return new IamStack(app, id, eksStack, rdsStack, { env })
}
