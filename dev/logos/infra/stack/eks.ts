import {
    AsgClusterProvider,
    AwsLoadBalancerControllerAddOn,
    BlueprintBuilder,
    ClusterAutoScalerAddOn,
    CoreDnsAddOn,
    DirectVpcProvider,
    EksBlueprint,
    ExternalDnsAddOn,
    GlobalResources,
    ImportCertificateProvider,
    ImportHostedZoneProvider,
    KubeProxyAddOn,
    MetricsServerAddOn,
    NginxAddOn,
    VpcCniAddOn
} from "@aws-quickstart/eks-blueprints";
import {App, Environment} from 'aws-cdk-lib';
import {UpdatePolicy} from 'aws-cdk-lib/aws-autoscaling';
import {InstanceType} from 'aws-cdk-lib/aws-ec2';
import {KubernetesVersion, MachineImageType} from 'aws-cdk-lib/aws-eks';
import {AcmStack} from "./acm";
import {R53Stack} from "./r53";
import {VpcStack} from "./vpc";


export function makeEksStack(
    app: App,
    id: string,
    vpcStack: VpcStack,
    acmStack: AcmStack,
    r53Stack: R53Stack,
    env: Environment
): EksBlueprint {
    // TODO pull domains from app definitions
    let builder: BlueprintBuilder =
        EksBlueprint.builder()
            .account(env.account)
            .region(env.region)
            .resourceProvider(GlobalResources.Vpc, new DirectVpcProvider(vpcStack.vpc))
            .clusterProvider(
               new AsgClusterProvider({
                   id: id + '-auto-scaling-group',
                   minSize: 1,
                   maxSize: 2,
                   instanceType: new InstanceType('t3.medium'),
                   machineImageType: MachineImageType.BOTTLEROCKET,
                   updatePolicy: UpdatePolicy.rollingUpdate(),
                   version: KubernetesVersion.V1_29,
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
            new ClusterAutoScalerAddOn({
                version: "auto"
            }),
            new CoreDnsAddOn("auto"),
            new KubeProxyAddOn("auto"),
            new MetricsServerAddOn(),
            new NginxAddOn({
                internetFacing: true,
                certificateResourceName: `${id}-ingress-cert`,
            }),
            new VpcCniAddOn({
                version: "auto",
            }),
        )
        .useDefaultSecretEncryption(true)
        .build(app, id);
}
