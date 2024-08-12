import {App, Environment} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {makeCognitoStack} from "../stack/cognito";
import {makeEcrStack} from '../stack/ecr';
import {makeEksStack} from '../stack/eks';
import {makeRdsStack} from '../stack/rds';
import {makeVpcStack} from "../stack/vpc";


export class Deployment {
    static readonly Development = new Deployment("dev");
    static readonly Staging = new Deployment("stage");
    static readonly Production = new Deployment("");

    readonly host?: string;

    constructor(host: string) {
        this.host = host;
    }

    public getFQDN(domain: string) {
        if (this.host) {
            return `${this.host}.${domain}`;
        } else {
            return domain;
        }
    }
}

export class LogosApp {
    readonly name: string;
    readonly host?: string;
    readonly domain: string;
    readonly deployment: Deployment;

    constructor(name: string, domain: string, deployment: Deployment) {
        this.name = name;
        this.domain = domain;
        this.deployment = deployment;
    }

    public getFQDN() {
        return this.deployment.getFQDN(this.domain);
    }
}

export class Logos extends Construct {
    constructor(app: App, id: string, env: Environment) {
        super(app, id);

        makeEcrStack(app, `${id}-ecr`, env);

        const
            vpcStack = makeVpcStack(app, `${id}-vpc`, env),
            rdsStack = makeRdsStack(app, `${id}-rds`, vpcStack, env);

        makeEksStack(app, `${id}-eks`, vpcStack, rdsStack, env);

        //makeCognitoStack(app, `${id}-cognito`, acmStack, iamStack, env, apps);
    }
}