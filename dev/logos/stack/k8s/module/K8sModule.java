package dev.logos.stack.k8s.module;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import dev.logos.app.register.registerModule;
import org.cdk8s.*;
import org.cdk8s.plus30.*;

import dev.logos.config.infra.InfrastructureProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;


@registerModule
public class K8sModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RpcServerChart {
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
        Multibinder.newSetBinder(binder(), PersistentVolumeClaimMount.class);
        Multibinder.newSetBinder(binder(), ExistingVolumeClaimMount.class);
        Multibinder.newSetBinder(binder(), EmptyDirMount.class);
        Multibinder.newSetBinder(binder(), SecretVolumeMount.class);
        Multibinder.newSetBinder(binder(), RpcServer.class);
    }

    @Provides
    @Singleton
    @RpcServerChart
    Chart provideInitialChart(App app) {
        return new Chart(app, "stack-chart", ChartProps.builder().build());
    }

    @ProvidesIntoSet
    Chart provideChart(
            @RpcServices Set<ServiceProps.Builder> servicePropsBuilders,
            @RpcDeployments Set<DeploymentProps.Builder> deploymentPropsBuilders,
            @RpcServerChart Chart chart
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
    Set<ServiceProps.Builder> provideService(@RpcServerChart Chart chart, Set<RpcServer> rpcServers) {
        return rpcServers.stream()
                         .map(rpcServer ->
                                 ServiceProps.builder()
                                             .metadata(
                                                     ApiObjectMetadata.builder()
                                                                      .name(rpcServer.rpcServerName() + "-service")
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
                                                                              .build())))
                         .collect(Collectors.toSet());
    }

    @Provides
    @Singleton
    @RpcDeployments
    Set<DeploymentProps.Builder> provideDeployment(@RpcServerChart Chart chart, Set<RpcServer> rpcServers) {
        return rpcServers.stream().map(rpcServer -> {
            Map<String, Volume> volumes = new HashMap<>();
            List<VolumeMount> volumeMounts = new ArrayList<>();

            for (PersistentVolumeClaimMount persistentVolumeClaimMount : rpcServer.persistentVolumeClaimMounts()) {
                PersistentVolumeClaimProps props = persistentVolumeClaimMount.persistentVolumeClaimPropsBuilder().build();
                String pvcName = rpcServer.rpcServerName() + requireNonNull(props.getMetadata()).getName();
                PersistentVolumeClaim pvc = new PersistentVolumeClaim(chart, pvcName, props);
                Volume volume = Volume.fromPersistentVolumeClaim(chart, pvcName + "-mount", pvc);
                volumes.put(pvcName, volume);

                persistentVolumeClaimMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                    VolumeMount mount = volumeMountBuilder.volume(volume).build();
                    volumeMounts.add(mount);
                });
            }

            for (ExistingVolumeClaimMount existingVolumeClaimMount : rpcServer.existingVolumeClaimMounts()) {
                String pvcName = existingVolumeClaimMount.claimName();
                Volume volume = Volume.fromPersistentVolumeClaim(chart, pvcName + "-mount", PersistentVolumeClaim.fromClaimName(chart, pvcName, pvcName));
                volumes.put(pvcName, volume);

                existingVolumeClaimMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                    VolumeMount mount = volumeMountBuilder.volume(volume).build();
                    volumeMounts.add(mount);
                });
            }

            int emptyDirVolumeIndex = 0;
            for (EmptyDirMount emptyDirMount : rpcServer.emptyDirMounts()) {
                EmptyDirVolumeOptions options = emptyDirMount.emptyDirVolumeOptionsBuilder().build();

                String volumeName = rpcServer.rpcServerName() + "-empty-dir-volume-" + emptyDirVolumeIndex++;
                Volume volume = Volume.fromEmptyDir(chart, volumeName, volumeName, options);
                volumes.put(volumeName, volume);

                emptyDirMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                    VolumeMount mount = volumeMountBuilder.volume(volume).build();
                    volumeMounts.add(mount);
                });
            }

            for (SecretVolumeMount secretVolumeMount : rpcServer.secretVolumeMounts()) {
                Volume volume = Volume.fromSecret(chart,
                        secretVolumeMount.secretName() + "-volume",
                        Secret.fromSecretName(
                                chart,
                                secretVolumeMount.secretName() + "-ref",
                                secretVolumeMount.secretName()));
                volumes.put(secretVolumeMount.secretName(), volume);

                secretVolumeMount.volumeMountBuilders().forEach(volumeMountBuilder -> {
                    VolumeMount mount = volumeMountBuilder.volume(volume).build();
                    volumeMounts.add(mount);
                });
            }

            volumes.put("configmap-logos-apps",
                    Volume.fromConfigMap(chart, "config-volume",
                            ConfigMap.fromConfigMapName(chart,
                                    "config-volume-map",
                                    "logos-apps")));

            Map<String, EnvValue> containerEnv = new HashMap<>(rpcServer.rpcServerEnv());
            if ("minikube".equalsIgnoreCase(InfrastructureProvider.VALUE)) {
                containerEnv.put("STORAGE_PG_BACKEND_JDBC_URL",
                        EnvValue.fromValue("jdbc:postgresql://db-rw-service/logos?sslmode=disable"));
                containerEnv.put("STORAGE_PG_BACKEND_PASSWORD", EnvValue.fromValue("storage"));
            } else {
                containerEnv.put("STORAGE_PG_BACKEND_JDBC_URL",
                        EnvValue.fromValue("jdbc:postgresql://db-rw-service/logos?usessl=require&sslmode=verify-full&sslrootcert=/etc/ssl/certs/aws-rds-global-bundle.pem"));
            }
            containerEnv.put("STORAGE_PG_BACKEND_USER", EnvValue.fromValue("storage"));

            List<ContainerProps> containers = new ArrayList<>();
            containers.add(rpcServer.containerPropsBuilder()
                                    .name("backend")
                                    .image("logos-ecr-backend")
                                    .imagePullPolicy(ImagePullPolicy.ALWAYS)
                                    .envVariables(containerEnv)
                                    .volumeMounts(volumeMounts)
                                    .ports(List.of(ContainerPort.builder().number(8081).build()))
                                    .securityContext(ContainerSecurityContextProps.builder()
                                                                                  .ensureNonRoot(true)
                                                                                  .user(1000)
                                                                                  .group(1000)
                                                                                  .build())
                                    .liveness(
                                            Probe.fromTcpSocket(
                                                    TcpSocketProbeOptions.builder() // should be a grpc check instead
                                                                         .port(8081)
                                                                         .initialDelaySeconds(Duration.seconds(10))
                                                                         .periodSeconds(Duration.seconds(10))
                                                                         .build())).build());

            rpcServer.sidecars().forEach(
                    sidecar -> containers.add(sidecar.containerPropsBuilder()
                                                     .imagePullPolicy(ImagePullPolicy.ALWAYS)
                                                     .envVariables(containerEnv)
                                                     .volumeMounts(
                                                             Stream.concat(
                                                                     Stream.ofNullable(sidecar.containerPropsBuilder().build().getVolumeMounts())
                                                                           .flatMap(Collection::stream),
                                                                     sidecar.existingVolumeClaimMounts().stream().flatMap(existingVolumeClaimMount -> {
                                                                         String pvcName = rpcServer.rpcServerName() + existingVolumeClaimMount.claimName();
                                                                         return existingVolumeClaimMount.volumeMountBuilders().stream().map(
                                                                                 volumeMountBuilder -> volumeMountBuilder.volume(volumes.get(pvcName)).build()
                                                                         );
                                                                     })
                                                             ).collect(Collectors.toList())
                                                     ).build()));

            return rpcServer.deploymentPropsBuilder()
                            .metadata(
                                    ApiObjectMetadata.builder()
                                                     .name(rpcServer.rpcServerName() + "-deployment")
                                                     .labels(Map.of("app", "backend"))
                                                     .build())
                            .podMetadata(ApiObjectMetadata.builder()
                                                          .labels(Map.of("app", "backend"))
                                                          .build())
                            .replicas(1)
                            .securityContext(PodSecurityContextProps.builder().fsGroup(1000).build())
                            .serviceAccount(ServiceAccount.fromServiceAccountName(chart,
                                    "logos-eks-stack-backend-service-account",
                                    "logos-eks-stack-backend-service-account"))
                            .volumes(volumes.values().stream().toList())
                            .select(true)
                            .automountServiceAccountToken(true)
                            .containers(containers);
        }).collect(Collectors.toSet());
    }

    public record RpcServer(
            String rpcServerName,
            DeploymentProps.Builder deploymentPropsBuilder,
            ContainerProps.Builder containerPropsBuilder,
            List<Sidecar> sidecars,
            Map<String, EnvValue> rpcServerEnv,
            List<PersistentVolumeClaimMount> persistentVolumeClaimMounts,
            List<ExistingVolumeClaimMount> existingVolumeClaimMounts,
            List<EmptyDirMount> emptyDirMounts,
            List<SecretVolumeMount> secretVolumeMounts
    ) {
    }

    public record Sidecar(
            ContainerProps.Builder containerPropsBuilder,
            List<ExistingVolumeClaimMount> existingVolumeClaimMounts
    ) {
    }

    public record PersistentVolumeClaimMount(PersistentVolumeClaimProps.Builder persistentVolumeClaimPropsBuilder,
                                             List<VolumeMount.Builder> volumeMountBuilders) {
    }

    public record ExistingVolumeClaimMount(String claimName, List<VolumeMount.Builder> volumeMountBuilders) {
    }

    public record EmptyDirMount(EmptyDirVolumeOptions.Builder emptyDirVolumeOptionsBuilder,
                                List<VolumeMount.Builder> volumeMountBuilders) {
    }

    public record SecretVolumeMount(String secretName,
                                    List<VolumeMount.Builder> volumeMountBuilders) {
    }
}
