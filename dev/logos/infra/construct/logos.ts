import { makeEksCluster } from '../stack/eks';
import { makeDnsStack } from '../stack/dns';
import { makeStorageStack } from '../stack/storage';
import { makeEcrStack } from '../stack/ecr';
import { Construct } from 'constructs';
import { App, Environment } from 'aws-cdk-lib';


export class Logos extends Construct {
    constructor(app: App, id: string, env: Environment) {
        super(app, id);

        makeDnsStack(app, id + '-dns', env);
        makeEcrStack(app, id + '-ecr', env);
        const cluster_info = makeEksCluster(app, id + '-eks', env).getClusterInfo();
        makeStorageStack(app, id + '-storage', cluster_info, env);
    }
}