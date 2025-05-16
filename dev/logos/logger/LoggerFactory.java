package dev.logos.logger;

import org.slf4j.Logger;

public interface LoggerFactory {
    Logger getLogger(Class<?> clazz);
    Logger getLogger(String name);
}
