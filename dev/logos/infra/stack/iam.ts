import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {EksBlueprint} from "@aws-quickstart/eks-blueprints";
import {PolicyStatement} from "aws-cdk-lib/aws-iam";
import {ServiceAccount} from "aws-cdk-lib/aws-eks";
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
    }
}

export function makeIamStack(app: App, id: string, eksStack, rdsStack, env: Environment): IamStack {
    return new IamStack(app, id, eksStack, rdsStack, { env })
}
