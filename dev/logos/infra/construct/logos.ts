import {App, Environment} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {makeEcrStack} from '../stack/ecr';
import {makeEksStack} from '../stack/eks';
import {makeR53Stack} from '../stack/r53';
import {makeRdsStack} from '../stack/rds';
import {makeIamStack} from "../stack/iam";

export class Logos extends Construct {
    constructor(app: App, id: string, env: Environment) {
        super(app, id);

        makeR53Stack(app, id + '-r53', env);
        makeEcrStack(app, id + '-ecr', env);

        const eksStack = makeEksStack(app, id + '-eks', env),
            rdsStack = makeRdsStack(app, id + '-rds', eksStack, env);

        makeIamStack(app, id + '-iam', eksStack, rdsStack, env);
    }
}