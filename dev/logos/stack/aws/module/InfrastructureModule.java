package dev.logos.stack.aws.module;

import com.google.inject.*;
import dev.logos.app.register.registerModule;
import dev.logos.stack.aws.AwsEnvironment;
import dev.logos.stack.aws.module.annotation.AwsAccountId;
import dev.logos.stack.aws.module.annotation.AwsRegion;
import software.amazon.awscdk.*;
import software.amazon.awscdk.cxapi.CloudAssembly;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.sts.StsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import static java.lang.System.err;

@registerModule
public class InfrastructureModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureModule.class);

    @Provides
    @Singleton
    @RootConstructId
    String provideEksStackId() {
        return "logos";
    }

    @Override
    protected void configure() {
        if (!AwsEnvironment.isEnabled()) {
            logger.atInfo().log("AWS infrastructure disabled; skipping CDK bindings");
            return;
        }

        install(new EcrModule());
        install(new VpcModule());
        install(new RdsModule());
        install(new EksModule());
    }

    @Provides
    @Singleton
    App provideApp() {
        ensureAwsEnabled("App");
        return new App(
                AppProps.builder()
                        .defaultStackSynthesizer(
                                DefaultStackSynthesizer.Builder.create().build())
                        .build()
        );
    }

    @Provides
    @AwsAccountId
    String provideAwsAccountId() {
        ensureAwsEnabled("AWS account id");
        try (StsClient stsClient = StsClient.builder().build()) {
            return stsClient.getCallerIdentity().account();
        }
    }

    @Provides
    @AwsRegion
    String provideAwsRegion() {
        ensureAwsEnabled("AWS region");
        return new DefaultAwsRegionProviderChain().getRegion().id();
    }

    @Provides
    @Singleton
    StackProps provideStackProps(@AwsAccountId String awsAccountId, @AwsRegion String awsRegion) {
        ensureAwsEnabled("StackProps");
        return StackProps.builder()
                         .env(Environment.builder()
                                         .account(awsAccountId)
                                         .region(awsRegion)
                                         .build())
                         .build();
    }

    @Provides
    CloudAssembly provideCloudAssembly(App app, Set<Stack> stacks) {
        ensureAwsEnabled("CloudAssembly");
        // Since CDK constructs its tree with references up from the children, we need to depend on the stacks even
        // though we don't need to do anything after they're constructed. The constructor of the stacks attaches them
        // to their parent constructs.
        for (Stack stack : stacks) {
            err.printf("CDK_STACK: %s%n", stack.getStackName());
        }

        return app.synth(StageSynthesisOptions.builder().validateOnSynthesis(true).build());
    }

    private void ensureAwsEnabled(String dependency) {
        if (!AwsEnvironment.isEnabled()) {
            throw new IllegalStateException("AWS infrastructure disabled; attempted to access " + dependency);
        }
    }
}
