package dev.logos.stack.aws.module;

import com.google.inject.*;
import dev.logos.app.register.registerModule;
import software.amazon.awscdk.*;
import software.amazon.awscdk.cxapi.CloudAssembly;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import static java.lang.System.err;

@registerModule
public class InfrastructureModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AwsAccountId {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AwsRegion {
    }

    @Provides
    @Singleton
    @RootConstructId
    String provideEksStackId() {
        return "logos";
    }

    @Override
    protected void configure() {
        install(new EcrModule());
        install(new VpcModule());
        install(new RdsModule());
        install(new EksModule());
    }

    @Provides
    @Singleton
    App provideApp() {
        return new App(
            AppProps.builder()
                .defaultStackSynthesizer(DefaultStackSynthesizer.Builder.create().build())
                .build()
        );
    }

    @Provides
    @AwsAccountId
    String provideAwsAccountId() {
        return System.getenv("LOGOS_AWS_ACCOUNT_ID");
    }

    @Provides
    @AwsRegion
    String provideAwsRegion() {
        return System.getenv("LOGOS_AWS_REGION");
    }

    @Provides
    @Singleton
    StackProps provideStackProps(@AwsAccountId String awsAccountId, @AwsRegion String awsRegion) {
        return StackProps.builder()
                .env(Environment.builder()
                                .account(awsAccountId)
                                .region(awsRegion)
                        .build())
                .build();
    }

    @Provides
    CloudAssembly provideCloudAssembly(App app, Set<Stack> stacks) {
        // Since CDK constructs its tree with references up from the children, we need to depend on the stacks even
        // though we don't need to do anything after they're constructed. The constructor of the stacks attaches them
        // to their parent constructs.
        for (Stack stack : stacks) {
            err.printf("CDK_STACK: %s%n", stack.getStackName());
        }

        return app.synth();
    }
}