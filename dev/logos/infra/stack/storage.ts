import { ClusterInfo } from "@aws-quickstart/eks-blueprints";
import { Stack, App, StackProps, Environment } from "aws-cdk-lib";
import { SecurityGroup, Peer, Port } from "aws-cdk-lib/aws-ec2";
import { Role, FederatedPrincipal } from "aws-cdk-lib/aws-iam";
import { DatabaseCluster, DatabaseClusterEngine, AuroraPostgresEngineVersion, ClusterInstance } from "aws-cdk-lib/aws-rds";
import { PrivateHostedZone, CnameRecord } from "aws-cdk-lib/aws-route53";


const PORT = 5432;

class StorageStack extends Stack {
    constructor(scope: App, id: string, clusterInfo: ClusterInfo, props?: StackProps) {
        super(scope, id, props);

        const securityGroupName = 'logos-rds-sg';
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

        const cluster = new DatabaseCluster(this, 'logos-rds-cluster', {
            clusterIdentifier: 'dev-logos-dev-rds-cluster',
            credentials: { username: 'clusteradmin' },
            defaultDatabaseName: 'logos',
            engine: DatabaseClusterEngine.auroraPostgres({ version: AuroraPostgresEngineVersion.VER_15_2 }),
            serverlessV2MinCapacity: 0.5,
            serverlessV2MaxCapacity: 1,
            vpc: clusterInfo.cluster.vpc,
            vpcSubnets: { subnets: clusterInfo.cluster.vpc.privateSubnets },
            writer: ClusterInstance.serverlessV2("writer"),
            securityGroups: [rdsSecurityGroup]
        });

        const privateDnsZone = new PrivateHostedZone(this, 'logos-rds-zone', {
            vpc: clusterInfo.cluster.vpc,
            zoneName: 'logos.dev'
        });

        new CnameRecord(this, 'db-rw-record', {
            zone: privateDnsZone,
            recordName: 'db-rw',
            domainName: cluster.clusterEndpoint.hostname
        });

        new CnameRecord(this, 'db-ro-record', {
            zone: privateDnsZone,
            recordName: 'db-ro',
            domainName: cluster.clusterReadEndpoint.hostname
        });

        // const serviceRole = new Role(this, 'ServiceRole', {
        //     assumedBy: new FederatedPrincipal(
        //         clusterInfo.cluster.openIdConnectProvider.openIdConnectProviderArn,
        //         { StringEquals: { 'sts:aud': 'sts.amazonaws.com' } },
        //         'sts:AssumeRoleWithWebIdentity'
        //     )
        // });

        // // serviceRole.addToPolicy(new PolicyStatement({
        // //     // Define the policy here...
        // // }));

        // const serviceAccountName = 'logos-eks-node-service-account';
        // const sa = clusterInfo.cluster.addServiceAccount(serviceAccountName, {
        //     name: serviceAccountName,
        //     namespace: 'default'
        // });
        // sa.role.node.addDependency(serviceRole);
    }
}


export function makeStorageStack(app: App, id: string, clusterInfo: ClusterInfo, env: Environment): StorageStack {
    return new StorageStack(app, id, clusterInfo, { env })
}