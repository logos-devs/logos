import { Stack, App, StackProps, Environment, RemovalPolicy } from "aws-cdk-lib";
import { Repository } from "aws-cdk-lib/aws-ecr";


class EcrStack extends Stack {
    private makeEcrRepo(id: string): Repository {
        return new Repository(this, id, {
            repositoryName: id,
            removalPolicy: RemovalPolicy.DESTROY
        });
    }

    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);

        this.makeEcrRepo(id + '-backend');
        this.makeEcrRepo(id + '-console');
        this.makeEcrRepo(id + '-storage');
    }
}

export function makeEcrStack(app: App, id: string, env: Environment): EcrStack {
    return new EcrStack(app, id, { env })
}