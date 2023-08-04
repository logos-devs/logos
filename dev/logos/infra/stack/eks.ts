import {App, Environment} from 'aws-cdk-lib';
import {
    AsgClusterProvider,
    AwsLoadBalancerControllerAddOn,
    CertManagerAddOn,
    ClusterAutoScalerAddOn,
    CoreDnsAddOn,
    EksBlueprint,
    KubeProxyAddOn,
    MetricsServerAddOn,
    NginxAddOn,
    VpcCniAddOn
} from "@aws-quickstart/eks-blueprints";
import {InstanceType} from 'aws-cdk-lib/aws-ec2';
import {UpdatePolicy} from 'aws-cdk-lib/aws-autoscaling';
import {KubernetesVersion, MachineImageType} from 'aws-cdk-lib/aws-eks';

export function makeEksStack(app: App, id: string, env: Environment): EksBlueprint {
    return EksBlueprint.builder()
        .account(env.account)
        .region(env.region)
        .clusterProvider(
            new AsgClusterProvider({
                id: id + '-auto-scaling-group',
                minSize: 1,
                maxSize: 2,
                instanceType: new InstanceType('t3.medium'),
                machineImageType: MachineImageType.BOTTLEROCKET,
                updatePolicy: UpdatePolicy.rollingUpdate(),
                version: KubernetesVersion.V1_26,
            })
        )
        .addOns(
            new AwsLoadBalancerControllerAddOn(),
            new CertManagerAddOn({
                installCRDs: true,
                createNamespace: true,
            }),
            new ClusterAutoScalerAddOn(),
            new CoreDnsAddOn("v1.9.3-eksbuild.2"),
            new KubeProxyAddOn("v1.26.2-eksbuild.1"),
            new MetricsServerAddOn(),
            new NginxAddOn({
                internetFacing: true,
                externalDnsHostname: 'dev.logos.dev'
            }),
            new VpcCniAddOn({
                version: "v1.12.5-eksbuild.2",
            }),
        )
        .useDefaultSecretEncryption(true)
        .build(app, id);
}
