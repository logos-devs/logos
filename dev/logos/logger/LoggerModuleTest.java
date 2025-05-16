package dev.logos.logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import com.google.inject.Inject;

public class LoggerModuleTest {

    static class LogTarget {
        public final Logger logger;

        @Inject
        public LogTarget(LoggerFactory loggerFactory) {
            logger = loggerFactory.getLogger(LogTarget.class);
        }
    }

    @Test
    public void logger_is_bound_to_correct_class() {
        Injector injector = Guice.createInjector(new LoggerModule());
        LogTarget target = injector.getInstance(LogTarget.class);

        assertNotNull("Logger should be injected", target.logger);
        assertEquals(
                "Logger should be named after the injection target class",
                LogTarget.class.getName(),
                target.logger.getName()
        );
    }
}