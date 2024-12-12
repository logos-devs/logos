package dev.logos.stack.aws.module;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import dev.logos.stack.aws.module.annotation.RpcServerDatabaseRoles;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.*;
import software.amazon.awscdk.cdk.lambdalayer.kubectl.v30.KubectlV30Layer;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.UpdatePolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.efs.FileSystem;
import software.amazon.awscdk.services.efs.*;
import software.amazon.awscdk.services.eks.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.rds.DatabaseCluster;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

public class EksModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CurrentRoleArn {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EksStackId {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RpcServerServiceAccount {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ClusterSubnetSelection {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WorkerNodeInstanceType {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AddonConfigs {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EfsStorageClassManifest {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DbRoServiceManifest {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DbRwServiceManifest {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WorkerNodePolicyActions {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EfsSecurityGroupBuilder {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EksFileSystemBuilder {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ServiceAccountOptionsBuilder {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ServiceAccountPolicyBuilder {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WorkerNodePolicyBuilder {
    }

    public static Map<String, Object> orderedMapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments: keyValues length must be even");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), Stack.class).addBinding().to(EksStack.class);
    }

    @Provides
    @Singleton
    @ClusterSubnetSelection
    List<SubnetSelection> provideClusterSubnetSelection() {
        return List.of(
                SubnetSelection.builder()
                               .subnetType(SubnetType.PUBLIC)
                               .build(),
                SubnetSelection.builder()
                               .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                               .build()
        );
    }

    @Provides
    @Singleton
    @WorkerNodeInstanceType
    InstanceType provideWorkerNodeInstanceType() {
        return InstanceType.of(InstanceClass.T3A, InstanceSize.LARGE);
    }

    @Provides
    @Singleton
    AlbControllerVersion provideAlbControllerVersion() {
        return AlbControllerVersion.V2_6_2;
    }

    @Provides
    @Singleton
    AlbControllerOptions.Builder provideAlbControllerOptionsBuilder(AlbControllerVersion albControllerVersion) {
        return AlbControllerOptions.builder().version(albControllerVersion);
    }

    @Provides
    @Singleton
    ClusterProps.Builder provideClusterProps(Vpc vpc,
                                             AlbControllerOptions.Builder albControllerOptionsBuilder,
                                             @ClusterSubnetSelection List<SubnetSelection> subnetSelection) {
        return ClusterProps.builder()
                           .version(KubernetesVersion.V1_30)
                           .albController(albControllerOptionsBuilder.build())
                           .defaultCapacity(0)
                           .vpc(vpc)
                           .vpcSubnets(subnetSelection);
    }

    @Provides
    @Singleton
    AutoScalingGroupCapacityOptions.Builder provideAutoScalingGroupPropsBuilder(
            @WorkerNodeInstanceType InstanceType instanceType
    ) {
        return AutoScalingGroupCapacityOptions.builder()
                                              .instanceType(instanceType)
                                              .machineImageType(MachineImageType.BOTTLEROCKET)
                                              .minCapacity(1)
                                              .maxCapacity(2)
                                              .updatePolicy(UpdatePolicy.rollingUpdate());
    }

    @Provides
    @Singleton
    @AddonConfigs
    List<Map<String, Object>> provideAddonConfigs() {
        return List.of(
                orderedMapOf("name", "vpc-cni", "version", "v1.18.3-eksbuild.2"),
                orderedMapOf("name", "coredns", "version", "v1.11.1-eksbuild.8"),
                orderedMapOf("name", "kube-proxy", "version", "v1.30.0-eksbuild.3"),
                orderedMapOf("name", "aws-efs-csi-driver", "version", "v2.0.7-eksbuild.1"));
    }

    @Provides
    @Singleton
    @EfsSecurityGroupBuilder
    SecurityGroupProps.Builder provideEfsSecurityGroupPropsBuilder(Vpc vpc) {
        return SecurityGroupProps.builder()
                                 .vpc(vpc)
                                 .description("Security group for EFS")
                                 .allowAllOutbound(true);
    }

    @Provides
    @Singleton
    @EksFileSystemBuilder
    FileSystemProps.Builder provideFileSystemPropsBuilder(Vpc vpc) {
        return FileSystemProps.builder()
                              .vpc(vpc)
                              .lifecyclePolicy(LifecyclePolicy.AFTER_7_DAYS)
                              .performanceMode(PerformanceMode.GENERAL_PURPOSE)
                              .throughputMode(ThroughputMode.ELASTIC)
                              .outOfInfrequentAccessPolicy(OutOfInfrequentAccessPolicy.AFTER_1_ACCESS)
                              .lifecyclePolicy(LifecyclePolicy.AFTER_14_DAYS)
                              // TODO switch to RETAIN upon release
                              .removalPolicy(RemovalPolicy.DESTROY)
                //.removalPolicy(software.amazon.awscdk.RemovalPolicy.RETAIN)
                ;
    }

    @Provides
    @Singleton
    @DbRoServiceManifest
    Map<String, Object> provideDbRoServiceManifest(DatabaseCluster dbCluster) {
        return orderedMapOf(
                "kind", "Service",
                "apiVersion", "v1",
                "metadata",
                orderedMapOf("name", "db-ro-service", "namespace", "default"),
                "spec",
                orderedMapOf("type", "ExternalName", "externalName", dbCluster.getClusterReadEndpoint().getHostname()));
    }

    @Provides
    @Singleton
    @DbRwServiceManifest
    Map<String, Object> provideDbRwServiceManifest(DatabaseCluster dbCluster) {
        return orderedMapOf(
                "kind", "Service",
                "apiVersion", "v1",
                "metadata",
                orderedMapOf("name", "db-rw-service", "namespace", "default"),
                "spec",
                orderedMapOf("type", "ExternalName", "externalName", dbCluster.getClusterEndpoint().getHostname()));
    }

    @Provides
    @Singleton
    @ServiceAccountOptionsBuilder
    ServiceAccountOptions.Builder provideServiceAccountOptionsBuilder() {
        return ServiceAccountOptions.builder().namespace("default");
    }

    @ProvidesIntoSet
    @RpcServerDatabaseRoles
    String provideStorage() {
        return "storage";
    }

    @Provides
    @Singleton
    @ServiceAccountPolicyBuilder
    PolicyStatement.Builder provideServiceAccountPolicyStatementBuilder(
            DatabaseCluster dbCluster,
            @RpcServerDatabaseRoles Set<String> databaseRoles
    ) {
        return PolicyStatement.Builder.create()
                                      .actions(Collections.singletonList("rds-db:connect"))
                                      .effect(Effect.ALLOW)
                                      .resources(databaseRoles.stream()
                                                              .map(role -> "arn:aws:rds-db:%s:%s:dbuser:%s/%s".formatted(
                                                                      dbCluster.getStack().getRegion(),
                                                                      dbCluster.getStack().getAccount(),
                                                                      dbCluster.getClusterResourceIdentifier(),
                                                                      role
                                                              )).toList());
    }

    @Provides
    @Singleton
    @WorkerNodePolicyActions
    List<String> provideWorkerNodePolicyActions() {
        return Arrays.asList(
                "route53:ChangeResourceRecordSets",
                "route53:CreateHealthCheck",
                "route53:CreateHostedZone",
                "route53:DeleteHealthCheck",
                "route53:DeleteHostedZone",
                "route53:GetHealthCheck",
                "route53:GetHostedZone",
                "route53:ListHostedZones",
                "route53:ListHostedZonesByName",
                "route53:ListResourceRecordSets",
                "route53:UpdateHealthCheck");
    }

    @Provides
    @Singleton
    @WorkerNodePolicyBuilder
    PolicyStatement.Builder provideWorkerNodePolicyStatementBuilder(@WorkerNodePolicyActions List<String> actions) {
        return PolicyStatement.Builder.create()
                                      .actions(actions)
                                      .effect(Effect.ALLOW)
                                      .resources(List.of("*"));
    }

    @Provides
    @Singleton
    @EksStackId
    String provideEksStackId(@RootConstructId String rootConstructId) {
        return "%s-eks-stack".formatted(rootConstructId);
    }

    @Provides
    @Singleton
    @CurrentRoleArn
    String provideCurrentRole() {
        try (
                IamClient iamClient = IamClient.builder().credentialsProvider(DefaultCredentialsProvider.create()).build();
                StsClient stsClient = StsClient.builder().credentialsProvider(DefaultCredentialsProvider.create()).build()
        ) {
            GetCallerIdentityResponse identityResponse = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
            String assumedRoleArn = identityResponse.arn();
            String sessionName = assumedRoleArn.substring(assumedRoleArn.lastIndexOf('/') + 1);
            ListRolesResponse rolesResponse = iamClient.listRoles(ListRolesRequest.builder()
                                                                                  .pathPrefix("/aws-reserved/sso.amazonaws.com/")
                                                                                  .build());

            for (software.amazon.awssdk.services.iam.model.Role role : rolesResponse.roles()) {
                if (assumedRoleArn.contains(role.roleName()) && assumedRoleArn.endsWith(sessionName)) {
                    return role.arn();
                }
            }

            throw new RuntimeException("Couldn't find the true ARN of your SSO role.");
        }
    }

    @Singleton
    public static class EksStack extends Stack {
        public static final String RPC_SERVICE_ACCOUNT_ROLE_ARN_OUTPUT = "LogosRpcServiceAccountRoleArn";
        private final Cluster cluster;

        @Inject
        public EksStack(
                App app,
                StackProps props,
                @EksStackId String id,
                @CurrentRoleArn String currentRoleArn,
                ClusterProps.Builder clusterPropsBuilder,
                AutoScalingGroupCapacityOptions.Builder autoScalingGroupCapacityOptionsBuilder,
                @AddonConfigs List<Map<String, Object>> addonConfigs,
                @EfsSecurityGroupBuilder SecurityGroupProps.Builder efsSecurityGroupPropsBuilder,
                @EksFileSystemBuilder FileSystemProps.Builder fileSystemPropsBuilder,
                @DbRoServiceManifest Map<String, Object> dbRoServiceManifest,
                @DbRwServiceManifest Map<String, Object> dbRwServiceManifest,
                @ServiceAccountOptionsBuilder ServiceAccountOptions.Builder serviceAccountOptionsBuilder,
                @ServiceAccountPolicyBuilder PolicyStatement.Builder serviceAccountPolicyStatementBuilder,
                @WorkerNodePolicyBuilder PolicyStatement.Builder workerNodePolicyStatementBuilder

        ) {
            super(app, id, props);

            String clusterId = id + "-cluster";
            this.cluster = new Cluster(
                    this,
                    clusterId,
                    clusterPropsBuilder.clusterName(clusterId).kubectlLayer(
                                               new KubectlV30Layer(this, id + "-kubectl-layer"))
                                       .endpointAccess(EndpointAccess.PUBLIC_AND_PRIVATE)
                                       .authenticationMode(AuthenticationMode.API_AND_CONFIG_MAP)
                                       .mastersRole(Role.fromRoleArn(this, id + "-deployment-role", currentRoleArn))
                                       .build()
            );

            for (Map<String, Object> addon : addonConfigs) {
                Addon.Builder.create(this, id + "-" + addon.get("name") + "-addon")
                             .cluster(cluster)
                             .addonName((String) addon.get("name"))
                             .addonVersion((String) addon.get("version"))
                             .build();
            }

            AutoScalingGroup asg = cluster.addAutoScalingGroupCapacity(id + "-autoscalinggroup",
                                                                       autoScalingGroupCapacityOptionsBuilder.build());

            SecurityGroup efsSg = new SecurityGroup(this, id + "-efs-sg", efsSecurityGroupPropsBuilder.build());
            efsSg.addIngressRule(Peer.securityGroupId(cluster.getClusterSecurityGroupId()), Port.tcp(2049));

            FileSystem fileSystem = new FileSystem(this, id + "-efs", fileSystemPropsBuilder.securityGroup(efsSg).build());

            cluster.addManifest(id + "-app-storage-class-manifest", orderedMapOf(
                    "kind", "StorageClass",
                    "apiVersion", "storage.k8s.io/v1",
                    "metadata", orderedMapOf(
                            "name", "app-storage-class",
                            "annotations",
                            orderedMapOf("storageclass.kubernetes.io/is-default-class", "true")),
                    "provisioner", "efs.csi.aws.com",
                    "parameters", orderedMapOf(
                            "provisioningMode", "efs-ap",
                            "fileSystemId", fileSystem.getFileSystemId(),
                            "uid", "0",
                            "gid", "0",
                            "directoryPerms", "700")));

            cluster.addManifest(id + "-db-ro-service-manifest", dbRoServiceManifest);
            cluster.addManifest(id + "-db-rw-service-manifest", dbRwServiceManifest);

            ServiceAccount rpcServerServiceAccount = cluster.addServiceAccount(
                    id + "-backend-service-account",
                    serviceAccountOptionsBuilder.name(id + "-backend-service-account")
                                                .build());
            rpcServerServiceAccount.addToPrincipalPolicy(serviceAccountPolicyStatementBuilder.build());

            new CfnOutput(this, id + "-backend-service-account-role-arn",
                          CfnOutputProps.builder()
                                        .exportName(RPC_SERVICE_ACCOUNT_ROLE_ARN_OUTPUT)
                                        .value(rpcServerServiceAccount.getRole().getRoleArn())
                                        .build());

            IRole role = asg.getRole();
            role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonEFSCSIDriverPolicy"));
            role.addToPrincipalPolicy(workerNodePolicyStatementBuilder.build());
        }

        public Cluster getCluster() {
            return cluster;
        }
    }
}