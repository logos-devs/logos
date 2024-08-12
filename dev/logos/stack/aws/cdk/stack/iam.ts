import {EksBlueprint} from "@aws-quickstart/eks-blueprints";
import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {ServiceAccount} from "aws-cdk-lib/aws-eks";
import {PolicyStatement} from "aws-cdk-lib/aws-iam";
import {RdsStack} from "./rds";

export class IamStack extends Stack {
    readonly serviceAccount: ServiceAccount;

    constructor(scope: App, id: string, eksStack: EksBlueprint, rdsStack: RdsStack, props: StackProps) {
        super(scope, id, props);

    }
}

export function makeIamStack(app: App, id: string, eksStack: EksBlueprint, rdsStack: RdsStack, env: Environment): IamStack {
    return new IamStack(app, id, eksStack, rdsStack, {env})
}
