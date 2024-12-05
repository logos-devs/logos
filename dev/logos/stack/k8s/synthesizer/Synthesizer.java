package dev.logos.stack.k8s.synthesizer;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import dev.logos.module.ModuleLoader;
import org.cdk8s.App;
import org.cdk8s.Chart;

import java.nio.file.Paths;
import java.util.Set;

import static java.lang.System.err;

public class Synthesizer {
    public static void main(String[] args) {
        Injector injector = ModuleLoader.createInjector();
        App app = injector.getInstance(App.class);

        // manually trigger the creation of charts due to cdk8s artifact tree challenges with Guice's IoC.
        for (Chart chart : injector.getInstance(Key.get(new TypeLiteral<Set<Chart>>() {
        }))) {
            err.printf("CDK8S_CHART: %s%n", chart);
        }

        err.printf("CDK8S_APP outdir: %s%n", Paths.get(app.getOutdir()).toAbsolutePath());
        app.synth();
    }
}