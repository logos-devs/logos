package dev.logos.stack.aws.cdk.stack;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import dev.logos.stack.aws.cdk.InfrastructureModule;
import dev.logos.stack.aws.cdk.InfrastructureModule.RootConstructId;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A Guice module for creating and configuring Amazon RDS (Relational Database Service) clusters using AWS CDK.
 * This module provides a flexible way to define and customize RDS clusters, specifically focusing on Aurora PostgreSQL.
 *
 * <p>By default, this module sets up an Aurora PostgreSQL cluster with customizable settings for port,
 * database name, credentials, backup properties, and instance configurations.</p>
 *
 * <p>The module includes an inner class {@code RdsStack} which extends {@code Stack} and creates the actual RDS resources.</p>
 *
 * <p>Example usage to customize the RDS configuration:</p>
 * <pre>
 * public class CustomRdsModule extends RdsModule {
 *     {@literal @}Provides
 *     {@literal @}Singleton
 *     {@literal @}Override
 *     BackupProps.Builder provideBackupPropsBuilder() {
 *         return BackupProps.builder()
 *                 .retention(Duration.days(30))
 *                 .preferredWindow("03:00-04:00");
 *     }
 *
 *     {@literal @}Provides
 *     {@literal @}Singleton
 *     {@literal @}WriterClusterInstanceProps
 *     {@literal @}Override
 *     ServerlessV2ClusterInstanceProps.Builder provideClusterInstancePropsBuilder() {
 *         return ServerlessV2ClusterInstanceProps.builder()
 *                 .autoMinorVersionUpgrade(true)
 *                 .publiclyAccessible(false);
 *     }
 * }
 * </pre>
 *
 * <p>Note: This module contains TODO comments indicating areas for potential future development or customization.</p>
 */
public class RdsModule extends AbstractModule {
    @Override
    protected void configure() {
        super.configure();
        Multibinder.newSetBinder(binder(), Stack.class).addBinding().to(RdsStack.class);
    }

    /**
     * Annotation for binding the stack construct ID.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RdsStackId {
    }

    /**
     * Annotation for binding the cluster port.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ClusterPort {
    }

    /**
     * Annotation for binding the database name.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DatabaseName {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WriterClusterInstanceProps {
    }

    /**
     * Annotation for binding the cluster subnet selection.
     */
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ClusterSubnetSelection {
    }

    /**
     * Provides the default port for the RDS cluster.
     *
     * @return The default port number (5432)
     */
    @Provides
    @ClusterPort
    int providePort() {
        return 5432;
    }

    /**
     * Provides the default database name.
     *
     * @return The default database name ("logos")
     */
    @Provides
    @DatabaseName
    String provideDefaultDatabaseName() {
        return "logos";
    }

    /**
     * Provides the default backup properties builder.
     *
     * @return A BackupProps.Builder with default settings
     */
    @Provides
    @Singleton
    BackupProps.Builder provideBackupPropsBuilder() {
        return BackupProps.builder()
                .retention(Duration.days(7))
                .preferredWindow("08:00-09:00");
    }

    /**
     * Provides the default database credentials.
     *
     * @return Credentials for the database cluster
     */
    @Provides
    @Singleton
    Credentials provideCredentials() {
        return Credentials.fromUsername("clusteradmin");
    }

    /**
     * Provides the Aurora PostgreSQL cluster engine properties builder.
     *
     * @return An AuroraPostgresClusterEngineProps.Builder with default settings
     */
    @Provides
    @Singleton
    AuroraPostgresClusterEngineProps.Builder provideAuroraPostgresClusterEnginePropsBuilder() {
        return AuroraPostgresClusterEngineProps.builder()
                .version(AuroraPostgresEngineVersion.VER_16_1);
    }

    /**
     * Provides the database cluster engine.
     *
     * @param auroraPostgresClusterEnginePropsBuilder The builder for Aurora PostgreSQL engine properties
     * @return An IClusterEngine for Aurora PostgreSQL
     */
    @Provides
    @Singleton
    IClusterEngine provideDatabaseClusterEngine(
            AuroraPostgresClusterEngineProps.Builder auroraPostgresClusterEnginePropsBuilder
    ) {
        return DatabaseClusterEngine.auroraPostgres(auroraPostgresClusterEnginePropsBuilder.build());
    }

    /**
     * Provides the default writer cluster instance properties builder.
     *
     * @return A ServerlessV2ClusterInstanceProps.Builder with default settings
     */
    @Provides
    @Singleton
    @WriterClusterInstanceProps
    ServerlessV2ClusterInstanceProps.Builder provideClusterInstancePropsBuilder() {
        return ServerlessV2ClusterInstanceProps.builder();
    }

    /**
     * Provides the default subnet selection for the cluster.
     *
     * @param vpc The VPC in which to place the cluster
     * @return A SubnetSelection for the private subnets of the VPC
     */
    @Provides
    @Singleton
    @ClusterSubnetSelection
    SubnetSelection provideSubnetSelection(Vpc vpc) {
        return SubnetSelection.builder()
                .subnets(vpc.getPrivateSubnets())
                .build();
    }

    /**
     * Provides the database cluster properties builder with default settings.
     *
     * @param vpc                   The VPC for the cluster
     * @param subnetSelection       The subnet selection for the cluster
     * @param port                  The port for the cluster
     * @param credentials           The credentials for the cluster
     * @param defaultDatabaseName   The default database name
     * @param databaseClusterEngine The database cluster engine
     * @param backupPropsBuilder    The backup properties builder
     * @return A DatabaseClusterProps.Builder with default settings
     */
    @Provides
    DatabaseClusterProps.Builder provideDatabaseClusterPropsBuilder(
            Vpc vpc,
            @ClusterSubnetSelection SubnetSelection subnetSelection,
            @ClusterPort int port,
            Credentials credentials,
            @DatabaseName String defaultDatabaseName,
            IClusterEngine databaseClusterEngine,
            BackupProps.Builder backupPropsBuilder
    ) {
        return DatabaseClusterProps.builder()
                .credentials(credentials)
                .vpc(vpc)
                .vpcSubnets(subnetSelection)
                .defaultDatabaseName(defaultDatabaseName)
                .backup(backupPropsBuilder.build())
                .port(port)
                .engine(databaseClusterEngine)
                .iamAuthentication(true)
                .storageEncrypted(true)
                .deletionProtection(false)
                .serverlessV2MaxCapacity(1)
                .serverlessV2MinCapacity(0.5);
    }

    @Provides
    @Singleton
    public DatabaseCluster provideDatabaseCluster(RdsStack rdsStack) {
        return rdsStack.getDatabaseCluster();
    }

    @Provides
    @Singleton
    @RdsStackId
    String provideEksStackId(@RootConstructId String rootConstructId) {
        return "%s-rds-stack".formatted(rootConstructId);
    }

    // TODO add binding for a list of ReaderClusterInstancePropsBuilders
    // TODO decouple from serverless to allow for other instance types.

    /**
     * The CDK Stack that creates the RDS cluster.
     */
    @Singleton
    public static class RdsStack extends Stack {
        private static final int PORT = 5432;
        private final DatabaseCluster databaseCluster;

        /**
         * Constructs an RdsStack.
         *
         * @param scope                             The parent construct
         * @param vpc                               The VPC for the RDS cluster
         * @param props                             The stack properties
         * @param databaseClusterPropsBuilder       The database cluster properties builder
         * @param writerClusterInstancePropsBuilder The writer cluster instance properties builder
         */
        @Inject
        public RdsStack(
                final App scope,
                final Vpc vpc,
                @RdsStackId final String id,
                final StackProps props,
                final DatabaseClusterProps.Builder databaseClusterPropsBuilder,
                final @WriterClusterInstanceProps ServerlessV2ClusterInstanceProps.Builder writerClusterInstancePropsBuilder
        ) {
            super(scope, id, props);

            String securityGroupName = id + "-sg";
            SecurityGroup rdsSecurityGroup = SecurityGroup.Builder.create(this, securityGroupName)
                    .vpc(vpc)
                    .securityGroupName(securityGroupName)
                    .build();

            vpc.getPrivateSubnets().forEach(
                    subnet -> rdsSecurityGroup.addIngressRule(
                            Peer.ipv4(subnet.getIpv4CidrBlock()),
                            Port.tcp(PORT),
                            "Allow traffic from source security group"
                    )
            );

            String clusterIdentifier = "%s-db-cluster".formatted(id);

            IClusterInstance writerClusterInstance = ClusterInstance.serverlessV2(
                    "%s-writer-instance".formatted(clusterIdentifier),
                    writerClusterInstancePropsBuilder.build()
            );

            this.databaseCluster = new DatabaseCluster(
                    this,
                    clusterIdentifier,
                    databaseClusterPropsBuilder
                            .clusterIdentifier(clusterIdentifier)
                            .writer(writerClusterInstance)
                            .securityGroups(List.of(rdsSecurityGroup))
                            .build()
            );
        }

        public DatabaseCluster getDatabaseCluster() {
            return databaseCluster;
        }
    }
}