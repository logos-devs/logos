import {EksBlueprint} from "@aws-quickstart/eks-blueprints";
import {App, Duration, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Peer, Port, SecurityGroup} from "aws-cdk-lib/aws-ec2";
import {
    AuroraPostgresEngineVersion,
    ClusterInstance,
    DatabaseCluster,
    DatabaseClusterEngine
} from "aws-cdk-lib/aws-rds";
import {CnameRecord, PrivateHostedZone} from "aws-cdk-lib/aws-route53";


const PORT = 5432;

export class RdsStack extends Stack {
    readonly databaseCluster: DatabaseCluster;

    constructor(scope: App, id: string, eksStack: EksBlueprint, props?: StackProps) {
        super(scope, id, props);

        const clusterInfo = eksStack.getClusterInfo();

        const securityGroupName = id + '-sg';
        const rdsSecurityGroup = new SecurityGroup(this, securityGroupName, {
            vpc: clusterInfo.cluster.vpc,
            securityGroupName
        });

        for (const subnet of clusterInfo.cluster.vpc.privateSubnets) {
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
            vpc: clusterInfo.cluster.vpc,
            vpcSubnets: { subnets: clusterInfo.cluster.vpc.privateSubnets },
            writer: ClusterInstance.serverlessV2("writer")
        });

        const privateDnsZone = new PrivateHostedZone(this, id + '-r53-zone', {
            vpc: clusterInfo.cluster.vpc,
            zoneName: 'logos.dev'
        });

        new CnameRecord(this, id + 'r53-zone-db-rw-record', {
            zone: privateDnsZone,
            recordName: 'db-rw',
            domainName: this.databaseCluster.clusterEndpoint.hostname
        });

        new CnameRecord(this, id + 'r53-db-ro-record', {
            zone: privateDnsZone,
            recordName: 'db-ro',
            domainName: this.databaseCluster.clusterReadEndpoint.hostname
        });
    }
}


export function makeRdsStack(app: App, id: string, eksStack: EksBlueprint, env: Environment): RdsStack {
    return new RdsStack(app, id, eksStack, { env })
}