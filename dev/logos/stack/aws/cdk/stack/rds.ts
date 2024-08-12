import {App, Duration, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Peer, Port, SecurityGroup} from "aws-cdk-lib/aws-ec2";
import {
    AuroraPostgresEngineVersion,
    ClusterInstance,
    DatabaseCluster,
    DatabaseClusterEngine
} from "aws-cdk-lib/aws-rds";
import {VpcStack} from "./vpc";


const PORT = 5432;

export class RdsStack extends Stack {
    readonly databaseCluster: DatabaseCluster;

    constructor(scope: App, id: string, vpcStack: VpcStack, props?: StackProps) {
        super(scope, id, props);

        const securityGroupName = id + '-sg';
        const rdsSecurityGroup = new SecurityGroup(this, securityGroupName, {
            vpc: vpcStack.vpc,
            securityGroupName
        });

        for (const subnet of vpcStack.vpc.privateSubnets) {
            rdsSecurityGroup.addIngressRule(
                Peer.ipv4(subnet.ipv4CidrBlock),
                Port.tcp(PORT),
                'Allow traffic from source security group');
        }

        const clusterIdentifier = id + '-db-cluster';
        this.databaseCluster = new DatabaseCluster(this, clusterIdentifier, {
            backup: {
                retention: Duration.days(7),
                preferredWindow: '08:00-09:00'
            },
            clusterIdentifier: clusterIdentifier,
            credentials: { username: 'clusteradmin' },
            defaultDatabaseName: 'logos',
            deletionProtection: false,
            engine: DatabaseClusterEngine.auroraPostgres({ version: AuroraPostgresEngineVersion.VER_16_1 }),
            iamAuthentication: true,
            securityGroups: [rdsSecurityGroup],
            serverlessV2MaxCapacity: 1,
            serverlessV2MinCapacity: 0.5,
            storageEncrypted: true,
            vpc: vpcStack.vpc,
            vpcSubnets: {subnets: vpcStack.vpc.privateSubnets},
            writer: ClusterInstance.serverlessV2("writer")
        });
    }
}


export function makeRdsStack(app: App, id: string, vpcStack: VpcStack, env: Environment): RdsStack {
    return new RdsStack(app, id, vpcStack, {env})
}