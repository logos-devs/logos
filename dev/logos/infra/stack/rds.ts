import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Peer, Port, SecurityGroup} from "aws-cdk-lib/aws-ec2";
import {
    AuroraPostgresEngineVersion,
    ClusterInstance,
    DatabaseCluster,
    DatabaseClusterEngine
} from "aws-cdk-lib/aws-rds";
import {CnameRecord, PrivateHostedZone} from "aws-cdk-lib/aws-route53";
import {EksBlueprint} from "@aws-quickstart/eks-blueprints";


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
            clusterIdentifier: clusterIdentifier,
            credentials: { username: 'clusteradmin' },
            defaultDatabaseName: 'logos',
            engine: DatabaseClusterEngine.auroraPostgres({ version: AuroraPostgresEngineVersion.VER_15_2 }),
            iamAuthentication: true,
            serverlessV2MinCapacity: 0.5,
            serverlessV2MaxCapacity: 1,
            storageEncrypted: true,
            vpc: clusterInfo.cluster.vpc,
            vpcSubnets: { subnets: clusterInfo.cluster.vpc.privateSubnets },
            writer: ClusterInstance.serverlessV2("writer"),
            securityGroups: [rdsSecurityGroup],
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