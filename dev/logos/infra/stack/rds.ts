import {ClusterInfo} from "@aws-quickstart/eks-blueprints";
import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Peer, Port, SecurityGroup} from "aws-cdk-lib/aws-ec2";
import {
    AuroraPostgresEngineVersion,
    ClusterInstance,
    DatabaseCluster,
    DatabaseClusterEngine
} from "aws-cdk-lib/aws-rds";
import {CnameRecord, PrivateHostedZone} from "aws-cdk-lib/aws-route53";


const PORT = 5432;

class StorageStack extends Stack {
    constructor(scope: App, id: string, clusterInfo: ClusterInfo, props?: StackProps) {
        super(scope, id, props);

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
        const cluster = new DatabaseCluster(this, clusterIdentifier, {
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
            domainName: cluster.clusterEndpoint.hostname
        });

        new CnameRecord(this, id + 'r53-db-ro-record', {
            zone: privateDnsZone,
            recordName: 'db-ro',
            domainName: cluster.clusterReadEndpoint.hostname
        });
    }
}


export function makeStorageStack(app: App, id: string, clusterInfo: ClusterInfo, env: Environment): StorageStack {
    return new StorageStack(app, id, clusterInfo, { env })
}