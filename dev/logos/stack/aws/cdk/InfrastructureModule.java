package dev.logos.stack.aws.cdk;

import com.google.inject.*;
import dev.logos.app.register.registerModule;
import dev.logos.stack.aws.cdk.stack.*;
import software.amazon.awscdk.*;
import software.amazon.awscdk.cxapi.CloudAssembly;

import java.util.Set;

import static java.lang.System.err;

@registerModule
public class InfrastructureModule extends AbstractModule {
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
    @Singleton
    StackProps provideStackProps() {
        return StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("LOGOS_AWS_ACCOUNT_ID"))
                        .region(System.getenv("LOGOS_AWS_REGION"))
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

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new InfrastructureModule());
        CloudAssembly assembly = injector.getInstance(CloudAssembly.class);
        err.printf("Assembly directory: %s\n", assembly.getDirectory());
    }
}