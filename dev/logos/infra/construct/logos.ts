import {makeEksCluster} from '../stack/eks';
import {makeEcrStack} from '../stack/ecr';
import {makeR53Stack} from '../stack/r53';
import {makeStorageStack} from '../stack/rds';
import {Construct} from 'constructs';
import {App, Environment} from 'aws-cdk-lib';


export class Logos extends Construct {
    constructor(app: App, id: string, env: Environment) {
        super(app, id);

        makeR53Stack(app, id + '-r53', env);
        makeEcrStack(app, id + '-ecr', env);
        const cluster_info = makeEksCluster(app, id + '-eks', env).getClusterInfo();
        makeStorageStack(app, id + '-rds', cluster_info, env);
    }
}