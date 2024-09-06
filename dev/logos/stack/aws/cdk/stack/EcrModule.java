package dev.logos.stack.aws.cdk.stack;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Guice module for creating ECR (Elastic Container Registry) repositories using AWS CDK.
 * This module provides a flexible way to define and customize ECR repositories.
 *
 * <p>By default, this module creates five ECR repositories: app-controller, backend, client, console, and envoy.
 * Additional repositories can be added, and properties of existing repositories can be customized
 * by extending this module or using Guice's multibinder functionality.</p>
 *
 * <p>Example usage to add a new repository with custom properties:</p>
 * <pre>
 * public class CustomEcrModule extends AbstractModule {
 *     {@literal @}Override
 *     protected void configure() {
 *         install(new EcrModule());  // Install the base EcrModule
 *
 *         // Add the new repository name
 *         Multibinder<String> repoNamesBinder = Multibinder.newSetBinder(binder(), String.class);
 *         repoNamesBinder.addBinding().toInstance("custom-repo");
 *
 *         // Add custom properties for the new repository
 *         MapBinder<String, RepositoryProps> repoBinder = MapBinder.newMapBinder(binder(), String.class, RepositoryProps.class);
 *         repoBinder.addBinding("custom-repo").toInstance(RepositoryProps.builder()
 *                 .repositoryName("custom-repo")
 *                 .imageScanOnPush(true)
 *                 .removalPolicy(RemovalPolicy.RETAIN)
 *                 .build());
 *     }
 * }
 * </pre>
 */
public class EcrModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EcrStackId {
    }

    @Override
    protected void configure() {
        // Set up multibinder for repository names
        Multibinder<String> repoNamesBinder = Multibinder.newSetBinder(binder(), String.class);
        repoNamesBinder.addBinding().toInstance("logos-ecr-app-controller");
        repoNamesBinder.addBinding().toInstance("logos-ecr-backend");
        repoNamesBinder.addBinding().toInstance("logos-ecr-client");
        repoNamesBinder.addBinding().toInstance("logos-ecr-console");
        repoNamesBinder.addBinding().toInstance("logos-ecr-envoy");

        Multibinder.newSetBinder(binder(), Stack.class).addBinding().to(EcrStack.class);
    }

    /**
     * Provides a map of repository names to their properties.
     * This method combines the bound properties with the default properties,
     * ensuring that each repository has a set of properties.
     *
     * @param repoNames The set of repository names
     * @return A map of repository names to their properties
     */
    @Provides
    @Singleton
    Map<String, RepositoryProps.Builder> provideRepositoryProps(
            Set<String> repoNames
    ) {
        Map<String, RepositoryProps.Builder> result = new HashMap<>();
        for (String repoName : repoNames) {
            result.put(repoName, RepositoryProps.builder().repositoryName(repoName));
        }
        return result;
    }

    @Provides
    @Singleton
    @EcrStackId
    String provideEksStackId(@RootConstructId String rootConstructId) {
        return "%s-ecr-stack".formatted(rootConstructId);
    }

    /**
     * The CDK Stack that creates the ECR repositories.
     */
    @Singleton
    public static class EcrStack extends Stack {
        /**
         * Constructs an EcrStack.
         *
         * @param scope             The parent construct
         * @param id                The construct ID
         * @param props             The stack properties
         * @param repoPropsBuilders The map of repository property builders
         * @param repoNames         The set of repository names
         */
        @Inject
        public EcrStack(
                final App scope,
                final @EcrStackId String id,
                final StackProps props,
                Set<String> repoNames,
                Map<String, RepositoryProps.Builder> repoPropsBuilders
        ) {
            super(scope, id, props);

            for (String repoName : repoNames) {
                new Repository(
                        this,
                        "%s-%s".formatted(id, repoName),
                        repoPropsBuilders.get(repoName).build()
                );
            }
        }
    }
}