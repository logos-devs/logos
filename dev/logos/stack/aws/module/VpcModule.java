package dev.logos.stack.aws.module;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

/**
 * This module creates a Virtual Private Cloud (VPC) using AWS CDK. It sets up the VPC with
 * customizable NAT instances, configures VPC properties, and establishes necessary security
 * groups. The module uses Guice for dependency injection, allowing easy customization of
 * the VPC setup.
 * <p>
 * Use this module when you need to create a VPC with specific NAT instance configurations
 * or when you want to integrate VPC creation into a larger CDK application using Guice.
 * <p>
 * The {@code VpcStack} inner class handles the actual creation of VPC resources, while
 * the module's methods provide the necessary components for this stack.
 */
public class VpcModule extends AbstractModule {
    @Override
    protected void configure() {
        super.configure();
        Multibinder.newSetBinder(binder(), Stack.class).addBinding().to(VpcStack.class);
    }

    /**
     * Annotation for binding the stack construct ID.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VpcStackId {
    }

    /**
     * Annotation for binding the NAT instance machine image.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NatMachineImage {
    }

    /**
     * Annotation for binding the NAT instance type.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NatInstanceType {
    }

    /**
     * Provides the machine image builder for NAT instances.
     *
     * @return A LookupMachineImage.Builder configured for NAT instances
     */
    @Provides
    @Singleton
    @NatMachineImage
    LookupMachineImage.Builder provideNatInstanceMachineImage() {
        return LookupMachineImage.Builder.create()
                                         .name("fck-nat-al2023-*-arm64-ebs")
                                         .owners(List.of("568608671756"));
    }

    /**
     * Provides the instance type for NAT instances.
     *
     * @return The InstanceType for NAT instances (t4g.micro)
     */
    @Provides
    @Singleton
    @NatInstanceType
    InstanceType provideNatInstanceType() {
        return InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO);
    }

    /**
     * Provides the NAT instance provider builder.
     *
     * @param machineImageBuilder The machine image builder for NAT instances
     * @param instanceType        The instance type for NAT instances
     * @return A NatInstanceProviderV2.Builder configured with the provided machine image and instance type
     */
    @Provides
    @Singleton
    NatInstanceProviderV2.Builder provideNatInstanceProvider(
            final App scope,
            @NatMachineImage LookupMachineImage.Builder machineImageBuilder,
            @NatInstanceType InstanceType instanceType
    ) {
        return NatInstanceProviderV2.Builder.create()
                                            .instanceType(instanceType)
                                            .machineImage(
                                                    MachineImage.genericLinux(Map.of("us-east-2", "ami-01509685ef74c2fc1"))
                                            );
    }

    /**
     * Provides the VPC properties builder.
     *
     * @return A VpcProps.Builder with default settings
     */
    @Provides
    @Singleton
    VpcProps.Builder provideVpcProps() {
        return VpcProps.builder()
                       .vpcName("logos-vpc")
                       .availabilityZones(List.of("us-east-2a", "us-east-2b", "us-east-2c"))
                       .restrictDefaultSecurityGroup(true);
    }

    /**
     * Provides the Vpc instance.
     *
     * @param vpcStack The VpcStack instance
     * @return The Vpc instance from the VpcStack
     */
    @Provides
    @Singleton
    Vpc provideVpc(VpcStack vpcStack) {
        return vpcStack.getVpc();
    }

    @Provides
    @Singleton
    @VpcStackId
    String provideEksStackId(@RootConstructId String rootConstructId) {
        return "%s-vpc-stack".formatted(rootConstructId);
    }

    /**
     * The CDK Stack that creates the VPC and related resources.
     */
    @Singleton
    public static class VpcStack extends Stack {
        private final Vpc vpc;

        /**
         * Constructs a VpcStack.
         *
         * @param scope                      The parent construct
         * @param id                         The construct ID
         * @param props                      The stack properties
         * @param vpcPropsBuilder            The VPC properties builder
         * @param natInstanceProviderBuilder The NAT instance provider builder
         */
        @Inject
        public VpcStack(
                final App scope,
                final @VpcStackId String id,
                final StackProps props,
                VpcProps.Builder vpcPropsBuilder,
                NatInstanceProviderV2.Builder natInstanceProviderBuilder
        ) {
            super(scope, id, props);

            NatInstanceProviderV2 natInstanceProvider = natInstanceProviderBuilder.build();

            vpc = new Vpc(this, "%s-vpc".formatted(id),
                    vpcPropsBuilder.natGatewayProvider(natInstanceProvider).build()
            );

            for (ISubnet subnet : vpc.getPublicSubnets()) {
                Tags.of(subnet).add("kubernetes.io/role/elb", "1");
            }

            for (ISubnet subnet : vpc.getPrivateSubnets()) {
                Tags.of(subnet).add("kubernetes.io/role/internal-elb", "1");
            }

            natInstanceProvider.getSecurityGroup().addIngressRule(
                    Peer.ipv4(vpc.getVpcCidrBlock()),
                    Port.allTraffic()
            );
        }

        /**
         * Gets the VPC instance.
         *
         * @return The Vpc instance
         */
        public Vpc getVpc() {
            return vpc;
        }
    }
}