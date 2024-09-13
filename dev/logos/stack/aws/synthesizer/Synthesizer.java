package dev.logos.stack.aws.synthesizer;

import com.google.inject.Injector;
import dev.logos.module.ModuleLoader;
import org.slf4j.Logger;
import software.amazon.awscdk.cxapi.CloudAssembly;

import static org.slf4j.LoggerFactory.getLogger;

public class Synthesizer {
    private static final Logger logger = getLogger(Synthesizer.class);

    public static void main(String[] args) {
        Injector injector = ModuleLoader.createInjector();
        CloudAssembly assembly = injector.getInstance(CloudAssembly.class);

        logger.atInfo()
              .addKeyValue("outputDirectory", assembly.getDirectory())
              .log("Stack synthesis complete");
    }
}
