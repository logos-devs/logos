import {
    AsgClusterProvider,
    AwsLoadBalancerControllerAddOn,
    ClusterAutoScalerAddOn,
    CoreDnsAddOn,
    DirectVpcProvider,
    EfsCsiDriverAddOn,
    EksBlueprint,
    GlobalResources,
    KubeProxyAddOn,
    NginxAddOn,
    VpcCniAddOn,
} from "@aws-quickstart/eks-blueprints";
import {App, Environment} from 'aws-cdk-lib';
import {UpdatePolicy} from 'aws-cdk-lib/aws-autoscaling';
import {InstanceType, Peer, Port, SecurityGroup, SubnetType} from 'aws-cdk-lib/aws-ec2';
import {KubernetesVersion, MachineImageType, ServiceAccount} from 'aws-cdk-lib/aws-eks';
import {VpcStack} from "./vpc";
import {
    FileSystem,
    LifecyclePolicy,
    OutOfInfrequentAccessPolicy,
    PerformanceMode,
    ReplicationOverwriteProtection,
    ThroughputMode
} from 'aws-cdk-lib/aws-efs';
import {PolicyStatement} from "aws-cdk-lib/aws-iam";
import {RdsStack} from "./rds";


export function makeEksStack(
    app: App,
    id: string,
    vpcStack: VpcStack,
    rdsStack: RdsStack,
    env: Environment
): EksBlueprint {
    // TODO pull domains from app definitions
    const eksStack =
        EksBlueprint.builder()
            .account(env.account)
            .region(env.region)
            .version(KubernetesVersion.V1_30)
            .resourceProvider(GlobalResources.Vpc, new DirectVpcProvider(vpcStack.vpc))
            .useDefaultSecretEncryption(true)
            .clusterProvider(
                new AsgClusterProvider({
                    id: id + '-auto-scaling-group',
                    minSize: 1,
                    maxSize: 2,
                    instanceType: new InstanceType('t3a.large'),
                    machineImageType: MachineImageType.BOTTLEROCKET,
                    updatePolicy: UpdatePolicy.rollingUpdate(),
                })
            )
            .addOns(
                // TODO consider enabling WAF and Shield
                new AwsLoadBalancerControllerAddOn(),
                new ClusterAutoScalerAddOn(),
                new CoreDnsAddOn(),
                new KubeProxyAddOn(),
                new EfsCsiDriverAddOn({
                    replicaCount: 1
                }),
                new NginxAddOn({
                    internetFacing: true
                }),
                new VpcCniAddOn()
            ).build(app, id);

    const
        clusterInfo = eksStack.getClusterInfo(),
        cluster = clusterInfo.cluster,
        autoscalingGroups = clusterInfo.autoscalingGroups,
        eksClusterSg = cluster.clusterSecurityGroup,
        vpc = cluster.vpc;

    const efsSg = new SecurityGroup(eksStack, `${id}-efs-sg`, {
        vpc: vpc,
        description: 'Security group for EFS',
        allowAllOutbound: true,
    });

    efsSg.addIngressRule(
        Peer.securityGroupId(eksClusterSg.securityGroupId),
        Port.tcp(2049), // NFS port
        'Allow NFS traffic from EKS to EFS'
    );

    autoscalingGroups.forEach(asg => {
        const role = asg.role;

        role.addManagedPolicy({
            managedPolicyArn: 'arn:aws:iam::aws:policy/service-role/AmazonEFSCSIDriverPolicy'
        });

        role.addManagedPolicy({
            managedPolicyArn: 'arn:aws:iam::aws:policy/service-role/AmazonEFSCSIDriverPolicy'
        });

        role.addToPrincipalPolicy(
            new PolicyStatement({
                actions: [
                    "route53:GetHostedZone",
                    "route53:ListHostedZones",
                    "route53:ListHostedZonesByName",
                    "route53:ListResourceRecordSets",
                    "route53:CreateHostedZone",
                    "route53:DeleteHostedZone",
                    "route53:ChangeResourceRecordSets",
                    "route53:CreateHealthCheck",
                    "route53:GetHealthCheck",
                    "route53:DeleteHealthCheck",
                    "route53:UpdateHealthCheck",
                ],
                resources: ["*"]
            })
        );
    });

    const
        fileSystem = new FileSystem(eksStack, `${id}-cni-efs`, {
            vpc: vpc,
            lifecyclePolicy: LifecyclePolicy.AFTER_7_DAYS,
            performanceMode: PerformanceMode.GENERAL_PURPOSE,
            securityGroup: efsSg,
            throughputMode: ThroughputMode.ELASTIC,
            outOfInfrequentAccessPolicy: OutOfInfrequentAccessPolicy.AFTER_1_ACCESS,
            transitionToArchivePolicy: LifecyclePolicy.AFTER_14_DAYS,
            replicationOverwriteProtection: ReplicationOverwriteProtection.ENABLED,
            vpcSubnets: {
                subnets: vpc.privateSubnets
            }
        });

    cluster.addManifest(
        `${id}-cni-efs-storage-class`,
        {
            kind: "StorageClass",
            apiVersion: "storage.k8s.io/v1",
            metadata: {
                name: "efs-storage-class",
                annotations: {
                    "storageclass.kubernetes.io/is-default-class": "true"
                }
            },
            provisioner: "efs.csi.aws.com",
            parameters: {
                provisioningMode: "efs-ap",
                fileSystemId: fileSystem.fileSystemId,
                directoryPerms: "700",
            }
        }
    );

    cluster.addManifest(
        `${id}-db-ro-service`,
        {
            kind: "Service",
            apiVersion: "v1",
            metadata: {
                name: "db-ro-service",
                namespace: "default",
            },
            spec: {
                type: "ExternalName",
                externalName: rdsStack.databaseCluster.clusterReadEndpoint.hostname
            }
        }
    );


    cluster.addManifest(
        `${id}-db-rw-service`,
        {
            kind: "Service",
            apiVersion: "v1",
            metadata: {
                name: "db-rw-service",
                namespace: "default",
            },
            spec: {
                type: "ExternalName",
                externalName: rdsStack.databaseCluster.clusterEndpoint.hostname
            }
        }
    );

    const serviceAccountName = id + '-eks-service-account';
    const serviceAccount = new ServiceAccount(
        eksStack,
        serviceAccountName,
        {
            cluster: eksStack.getClusterInfo().cluster,
            name: serviceAccountName,
            namespace: 'default'
        }
    );

    serviceAccount.addToPrincipalPolicy(
        new PolicyStatement({
            actions: ["rds-db:connect"],
            resources: [
                `arn:aws:rds-db:${env.region}:${env.account}:dbuser:${rdsStack.databaseCluster.clusterResourceIdentifier}/storage`
            ]
        })
    );

    return eksStack;
}
