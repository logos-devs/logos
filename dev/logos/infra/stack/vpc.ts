import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {
    InstanceClass,
    InstanceSize,
    InstanceType,
    LookupMachineImage,
    NatInstanceProviderV2,
    Peer,
    Port,
    Vpc
} from "aws-cdk-lib/aws-ec2";

export class VpcStack extends Stack {
    public readonly vpc: Vpc;

    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);

        const natGatewayProvider = new NatInstanceProviderV2({
            instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO),
            machineImage: new LookupMachineImage({
                name: 'fck-nat-amzn2-*-arm64-ebs',
                owners: ['568608671756'],
            })
        });

        this.vpc = new Vpc(this, id + '-vpc', {
            natGatewayProvider,
            restrictDefaultSecurityGroup: true
        });
        
        natGatewayProvider.securityGroup.addIngressRule(Peer.ipv4(this.vpc.vpcCidrBlock), Port.allTraffic());
    }
}

export function makeVpcStack(app: App, id: string, env: Environment): VpcStack {
    return new VpcStack(app, id, { env });
}
