import {App, Environment} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {makeAcmStack} from "../stack/acm";
import {makeCognitoStack} from "../stack/cognito";
import {makeEcrStack} from '../stack/ecr';
import {makeEksStack} from '../stack/eks';
import {makeIamStack} from "../stack/iam";
import {makeR53Stack} from '../stack/r53';
import {makeRdsStack} from '../stack/rds';
import {makeS3Stack} from "../stack/s3";


export class Deployment {
    static readonly Development = new Deployment("dev");
    static readonly Staging = new Deployment("stage");
    static readonly Production = new Deployment("");

    readonly host: string;

    constructor(host: string) {
        this.host = host;
    }

    public getFQDN(domain: string) {
        if (this.host) {
            return `${this.host}.${domain}`;
        }
        else {
            return domain;
        }
    }
}

export class LogosApp {
    readonly name: string;
    readonly host: string;
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

        const
            deployment = Deployment.Development,
            apps = [
                new LogosApp("digits", "digits.rip", deployment),
                new LogosApp("rep", "rep.dev", deployment),
                new LogosApp("summer", "summer.app", deployment),
            ],
            r53Stack = makeR53Stack(app, `${id}-r53`, deployment.getFQDN("logos.dev"), env, apps),
            acmStack = makeAcmStack(app, `${id}-acm`, r53Stack, env);

        makeEcrStack(app, `${id}-ecr`, env);

        const
            eksStack = makeEksStack(app, `${id}-eks`, acmStack, r53Stack, env),
            rdsStack = makeRdsStack(app, `${id}-rds`, eksStack, env),
            iamStack = makeIamStack(app, `${id}-iam`, eksStack, rdsStack, env);

        makeCognitoStack(app, `${id}-cognito`, acmStack, iamStack, env, apps);
        makeS3Stack(app, `${id}-s3`, env);
    }
}