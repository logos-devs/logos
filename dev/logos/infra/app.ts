#!/usr/bin/env node

import * as cdk from 'aws-cdk-lib';
import * as blueprints from '@aws-quickstart/eks-blueprints';
//import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as asg from 'aws-cdk-lib/aws-autoscaling';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as r53 from 'aws-cdk-lib/aws-route53';
import * as r53Targets from 'aws-cdk-lib/aws-route53-targets';

const app = new cdk.App(),
    account = process.env.CDK_DEFAULT_ACCOUNT,
    region = process.env.CDK_DEFAULT_REGION;

const cluster_info = blueprints.EksBlueprint.builder()
    .account(account)
    .region(region)
    .clusterProvider(
        new blueprints.AsgClusterProvider({
            id: 'logos-eks',
            minSize: 1,
            maxSize: 2,
            instanceType: new ec2.InstanceType('t3.medium'),
            machineImageType: eks.MachineImageType.BOTTLEROCKET,
            updatePolicy: asg.UpdatePolicy.rollingUpdate(),
            version: eks.KubernetesVersion.V1_26,
        })
    )
    .addOns(
        new blueprints.addons.AwsLoadBalancerControllerAddOn(),
        new blueprints.addons.CertManagerAddOn({
            installCRDs: true,
            createNamespace: true,
        }),
        new blueprints.addons.ClusterAutoScalerAddOn(),
        new blueprints.addons.CoreDnsAddOn("v1.9.3-eksbuild.2"),
        new blueprints.addons.KubeProxyAddOn("v1.26.2-eksbuild.1"),
        new blueprints.addons.MetricsServerAddOn(),
        new blueprints.addons.NginxAddOn({
            externalDnsHostname: 'dev.logos.dev'
        }),
        new blueprints.addons.VpcCniAddOn({
            version: "v1.12.5-eksbuild.2",
        }),
    )
    .useDefaultSecretEncryption(true)
    .build(app, 'logos-eks')
    .getClusterInfo();


class LogosStack extends cdk.Stack {
    constructor(scope: cdk.App, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        // const repository = new ecr.Repository(this, 'logos-ecr', {
        //     imageScanOnPush: false,
        //     imageTagMutability: ecr.TagMutability.MUTABLE
        // });

        //this.makeEksNodeServiceAccount();
        this.makeRdsCluster();
    }

    private makeRdsCluster() {
        const securityGroupName = 'logos-rds-sg';
        const rdsSecurityGroup = new ec2.SecurityGroup(this, securityGroupName, {
            vpc: cluster_info.cluster.vpc,
            securityGroupName
        });

        for (const subnet of cluster_info.cluster.vpc.privateSubnets) {
            rdsSecurityGroup.addIngressRule(
                ec2.Peer.ipv4(subnet.ipv4CidrBlock),
                ec2.Port.tcp(5432),
                'Allow traffic from source security group');
        }

        const cluster = new rds.DatabaseCluster(this, 'logos-rds-cluster', {
            clusterIdentifier: 'dev-logos-dev-rds-cluster',
            credentials: { username: 'clusteradmin' },
            defaultDatabaseName: 'logos',
            engine: rds.DatabaseClusterEngine.auroraPostgres({ version: rds.AuroraPostgresEngineVersion.VER_15_2 }),
            serverlessV2MinCapacity: 0.5,
            serverlessV2MaxCapacity: 1,
            vpc: cluster_info.cluster.vpc,
            vpcSubnets: { subnets: cluster_info.cluster.vpc.privateSubnets },
            writer: rds.ClusterInstance.serverlessV2("writer"),
            securityGroups: [rdsSecurityGroup]
        });

        const privateDnsZone = new r53.PrivateHostedZone(this, 'logos-rds-zone', {
            vpc: cluster_info.cluster.vpc,
            zoneName: 'logos.dev'
        });

        new r53.CnameRecord(this, 'db-rw-record', {
            zone: privateDnsZone,
            recordName: 'db-rw',
            domainName: cluster.clusterEndpoint.hostname
        });

        new r53.CnameRecord(this, 'db-ro-record', {
            zone: privateDnsZone,
            recordName: 'db-ro',
            domainName: cluster.clusterReadEndpoint.hostname
        });

        // TODO role-based authentication for muh pods
    }

    private makeEksNodeServiceAccount() {
        const serviceRole = new iam.Role(this, 'ServiceRole', {
            assumedBy: new iam.FederatedPrincipal(
                cluster_info.cluster.openIdConnectProvider.openIdConnectProviderArn,
                { StringEquals: { 'sts:aud': 'sts.amazonaws.com' } },
                'sts:AssumeRoleWithWebIdentity'
            )
        });

        serviceRole.addToPolicy(new iam.PolicyStatement({
            // Define the policy here...
        }));

        const serviceAccountName = 'logos-eks-node-service-account';
        const sa = cluster_info.cluster.addServiceAccount(serviceAccountName, {
            name: serviceAccountName,
            namespace: 'default'
        });
        sa.role.node.addDependency(serviceRole);
    }
}

new LogosStack(app, 'logos-stack', {
    env: {
        account: account,
        region: region,
    }
});