import {App, Environment} from 'aws-cdk-lib';
import {
    AsgClusterProvider,
    AwsLoadBalancerControllerAddOn,
    BlueprintBuilder,
    ClusterAutoScalerAddOn,
    CoreDnsAddOn,
    EksBlueprint,
    ExternalDnsAddOn,
    ImportCertificateProvider,
    ImportHostedZoneProvider,
    KubeProxyAddOn,
    MetricsServerAddOn,
    NginxAddOn,
    VpcCniAddOn
} from "@aws-quickstart/eks-blueprints";
import {InstanceType} from 'aws-cdk-lib/aws-ec2';
import {UpdatePolicy} from 'aws-cdk-lib/aws-autoscaling';
import {KubernetesVersion, MachineImageType} from 'aws-cdk-lib/aws-eks';
import {R53Stack} from "./r53";
import {AcmStack} from "./acm";

export function makeEksStack(
    app: App,
    id: string,
    acmStack: AcmStack,
    r53Stack: R53Stack,
    env: Environment
): EksBlueprint {
    // TODO pull domains from app definitions
    let builder: BlueprintBuilder =
        EksBlueprint.builder()
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
            );

    for (const zone of r53Stack.zones) {
        builder = builder.resourceProvider(
            zone.zoneName,
            new ImportHostedZoneProvider(zone.hostedZoneId));
    }

    builder = builder.resourceProvider(`${id}-ingress-cert`,
        new ImportCertificateProvider(acmStack.certificate.certificateArn, `${id}-ingress-cert-import`))

    return builder.addOns(
            // TODO consider enabling WAF and Shield
            new AwsLoadBalancerControllerAddOn(),
            new ExternalDnsAddOn({
                hostedZoneResources: r53Stack.zones.map(
                    zone => zone.zoneName
                ),
                values: {
                    domainFilters: r53Stack.zones.map(
                        zone => zone.zoneName
                    )
                }
            }),
            new ClusterAutoScalerAddOn(),
            new CoreDnsAddOn("v1.9.3-eksbuild.2"),
            new KubeProxyAddOn("v1.26.2-eksbuild.1"),
            new MetricsServerAddOn(),
            new NginxAddOn({
                internetFacing: true,
                externalDnsHostname: 'dev.digits.rip',
                certificateResourceName: `${id}-ingress-cert`
            }),
            new VpcCniAddOn({
                version: "v1.12.5-eksbuild.2",
            }),
        )
        .useDefaultSecretEncryption(true)
        .build(app, id);
}
