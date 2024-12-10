package dev.logos.stack.k8s.module;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import dev.logos.app.register.registerModule;
import org.cdk8s.*;
import org.cdk8s.plus30.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import static java.util.Objects.requireNonNull;


@registerModule
public class K8sModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RpcServerChart {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    private @interface InitialRpcServerChart {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RpcServerName {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RpcServices {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RpcDeployments {
    }

    @Override
    protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), Key.get(String.class, RpcServerName.class));
        Multibinder.newSetBinder(binder(), PersistentVolumeClaimMount.class);
        Multibinder.newSetBinder(binder(), EmptyDirMount.class);
        Multibinder.newSetBinder(binder(), SecretVolumeMount.class);
    }

    @ProvidesIntoSet
    Chart provideIntoChartSet(@RpcServerChart Chart chart) {
        return chart;
    }

    @Provides
    @Singleton
    @InitialRpcServerChart
    Chart provideInitialChart(App app) {
        return new Chart(app, "stack-chart", ChartProps.builder().build());
    }

    @Provides
    @Singleton
    @RpcServerChart
    Chart provideChart(
            @RpcServices Set<ServiceProps.Builder> servicePropsBuilders,
            @RpcDeployments Set<DeploymentProps.Builder> deploymentPropsBuilders,
            @InitialRpcServerChart Chart chart
    ) {
        servicePropsBuilders.forEach(
                servicePropsBuilder ->
                        new org.cdk8s.plus30.Service(chart, "backend-service", servicePropsBuilder.build()));
        deploymentPropsBuilders.forEach(
                deploymentPropsBuilder -> new Deployment(chart, "backend-deployment", deploymentPropsBuilder.build()));
        return chart;
    }

    @Provides
    @Singleton
    App provideApp() {
        return new App();
    }

    @Provides
    @Singleton
    @RpcServices
    Set<ServiceProps.Builder> provideService(@InitialRpcServerChart Chart chart, @RpcServerName Optional<String> optionalRpcServerName) {
        return optionalRpcServerName.map(rpcServerName -> Set.of(ServiceProps.builder()
                                                                             .metadata(
                                                                                     ApiObjectMetadata.builder()
                                                                                                      .name(rpcServerName + "-service")
                                                                                                      .labels(Map.of("app", "backend"))
                                                                                                      .build())
                                                                             .type(ServiceType.NODE_PORT)
                                                                             .ports(List.of(ServicePort.builder()
                                                                                                       .port(8081)
                                                                                                       .protocol(Protocol.TCP)
                                                                                                       .build()))
                                                                             .selector(
                                                                                     Pods.select(
                                                                                             chart,
                                                                                             "backend-pods",
                                                                                             PodsSelectOptions.builder()
                                                                                                              .labels(Map.of("app", "backend"))
                                                                                                              .build())))).orElseGet(Set::of);
    }

    @Provides
    @Singleton
    @RpcDeployments
    Set<DeploymentProps.Builder> provideDeployment(
            @InitialRpcServerChart Chart chart,
            @RpcServerName Optional<String> optionalRpcServerName,
            Set<PersistentVolumeClaimMount> persistentVolumeClaimMounts,
            Set<EmptyDirMount> emptyDirMounts,
            Set<SecretVolumeMount> secretVolumeMounts
    ) {
        return optionalRpcServerName.map(
                rpcServerName -> {
                    List<Volume> volumes = new ArrayList<>();
                    List<VolumeMount> volumeMounts = new ArrayList<>();

                    for (PersistentVolumeClaimMount persistentVolumeClaimMount : persistentVolumeClaimMounts) {
                        PersistentVolumeClaimProps props = persistentVolumeClaimMount.persistentVolumeClaimPropsBuilder().build();
                        String pvcName = rpcServerName + requireNonNull(props.getMetadata()).getName();
                        PersistentVolumeClaim pvc = new PersistentVolumeClaim(chart, pvcName, props);
                        Volume volume = Volume.fromPersistentVolumeClaim(chart, pvcName + "-mount", pvc);
                        volumes.add(volume);

                        persistentVolumeClaimMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                            VolumeMount mount = volumeMountBuilder.volume(volume).build();
                            volumeMounts.add(mount);
                        });
                    }

                    int emptyDirVolumeIndex = 0;
                    for (EmptyDirMount emptyDirMount : emptyDirMounts) {
                        EmptyDirVolumeOptions options = emptyDirMount.emptyDirVolumeOptionsBuilder().build();

                        String volumeName = rpcServerName + "-empty-dir-volume-" + emptyDirVolumeIndex++;
                        Volume volume = Volume.fromEmptyDir(chart, volumeName, volumeName, options);
                        volumes.add(volume);

                        emptyDirMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                            VolumeMount mount = volumeMountBuilder.volume(volume).build();
                            volumeMounts.add(mount);
                        });
                    }

                    for (SecretVolumeMount secretVolumeMount : secretVolumeMounts) {
                        Volume volume = Volume.fromSecret(chart,
                                                          secretVolumeMount.secretName() + "-volume",
                                                          Secret.fromSecretName(
                                                                  chart,
                                                                  secretVolumeMount.secretName() + "-ref",
                                                                  secretVolumeMount.secretName()));
                        volumes.add(volume);

                        secretVolumeMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                            VolumeMount mount = volumeMountBuilder.volume(volume).build();
                            volumeMounts.add(mount);
                        });
                    }

                    volumes.add(Volume.fromConfigMap(chart, "config-volume",
                                                     ConfigMap.fromConfigMapName(chart, "config-volume-map", "logos-apps")));

                    return Set.of(DeploymentProps.builder()
                                                 .metadata(
                                                         ApiObjectMetadata.builder()
                                                                          .name(rpcServerName + "-deployment")
                                                                          .labels(Map.of("app", "backend"))
                                                                          .build())
                                                 .podMetadata(ApiObjectMetadata.builder()
                                                                               .labels(Map.of("app", "backend"))
                                                                               .build())
                                                 .replicas(1)
                                                 .serviceAccount(ServiceAccount.fromServiceAccountName(chart,
                                                                                                       "logos-eks-stack-backend-service-account",
                                                                                                       "logos-eks-stack-backend-service-account"))
                                                 .volumes(volumes)
                                                 .select(true)
                                                 .containers(List.of(
                                                         ContainerProps.builder()
                                                                       .name("backend")
                                                                       .image("logos-ecr-backend")
                                                                       .imagePullPolicy(ImagePullPolicy.ALWAYS)
                                                                       .envVariables(
                                                                               Map.of(
                                                                                       "STORAGE_PG_BACKEND_JDBC_URL",
                                                                                       EnvValue.fromValue("jdbc:postgresql://db-rw-service/logos?usessl=require&sslmode=verify-full&sslrootcert=/etc/ssl/certs/aws-rds-global-bundle.pem"),
                                                                                       "STORAGE_PG_BACKEND_USER",
                                                                                       EnvValue.fromValue("storage")
                                                                               ))
                                                                       .resources(
                                                                               ContainerResources.builder()
                                                                                                 .cpu(CpuResources.builder()
                                                                                                                  .request(Cpu.millis(200))
                                                                                                                  .build())
                                                                                                 .memory(MemoryResources.builder()
                                                                                                                        .request(Size.mebibytes(512))
                                                                                                                        .limit(Size.gibibytes(2))
                                                                                                                        .build())
                                                                                                 .build())
                                                                       .volumeMounts(volumeMounts)
                                                                       .ports(List.of(ContainerPort.builder().number(8081).build()))
                                                                       .securityContext(ContainerSecurityContextProps.builder().ensureNonRoot(false).build())
                                                                       .liveness(Probe.fromTcpSocket(TcpSocketProbeOptions.builder() // should be a grpc check instead
                                                                                                                          .port(8081)
                                                                                                                          .initialDelaySeconds(Duration.seconds(10))
                                                                                                                          .periodSeconds(Duration.seconds(10))
                                                                                                                          .build()))
                                                                       .build()
                                                 )));
                }
        ).orElseGet(Set::of);
    }

    public record PersistentVolumeClaimMount(PersistentVolumeClaimProps.Builder persistentVolumeClaimPropsBuilder,
                                             List<VolumeMount.Builder> volumeMountBuilders) {
    }

    public record EmptyDirMount(EmptyDirVolumeOptions.Builder emptyDirVolumeOptionsBuilder,
                                List<VolumeMount.Builder> volumeMountBuilders) {
    }

    public record SecretVolumeMount(String secretName,
                                    List<VolumeMount.Builder> volumeMountBuilders) {
    }
}