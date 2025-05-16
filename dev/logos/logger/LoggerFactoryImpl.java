package dev.logos.logger;

import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;

class LoggerFactoryImpl implements LoggerFactory {
    @Override
    public Logger getLogger(@Assisted Class<?> clazz) {
        return org.slf4j.LoggerFactory.getLogger(clazz);
    }

    @Override
    public Logger getLogger(@Assisted String name) {
        return org.slf4j.LoggerFactory.getLogger(name);
    }
}