package dev.logos.stack.aws.cdk;

import com.google.inject.*;
import dev.logos.stack.aws.cdk.stack.EcrModule;
import dev.logos.stack.aws.cdk.stack.EksModule;
import dev.logos.stack.aws.cdk.stack.RdsModule;
import dev.logos.stack.aws.cdk.stack.VpcModule;
import software.amazon.awscdk.*;
import software.amazon.awscdk.cxapi.CloudAssembly;
import software.constructs.Construct;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import static java.lang.System.err;

public class InfrastructureModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RootConstructId {}

    @Provides
    @Singleton
    @RootConstructId
    String provideEksStackId() {
        return "logos";
    }

//    class InfrastructureConstruct extends Construct {
//        @Inject
//        public InfrastructureConstruct(Construct scope, @RootConstructId String id) {
//            super(scope, id);
//        }
//    }

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

    public static void main(String[] args) {

        Injector injector = Guice.createInjector(new InfrastructureModule());

        // Ensure all stacks are initialized and log them
        Set<Stack> stacks = injector.getInstance(Key.get(new TypeLiteral<>() {}));
        for (Stack stack : stacks) {
            err.printf("CDK_STACK: %s%n", stack.getStackName());
        }

        App app = injector.getInstance(App.class);
        app.synth();
        CloudAssembly assembly = app.synth();
        System.out.println("Synthesis complete.");

        // Print information about the assembly
        System.out.println("Assembly directory: " + assembly.getDirectory());
    }
}