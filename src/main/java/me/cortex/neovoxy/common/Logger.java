package me.cortex.neovoxy.common;

import org.slf4j.LoggerFactory;

/**
 * Centralized logging for NeoVoxy.
 * Wraps SLF4J logger with mod-specific prefix.
 */
public final class Logger {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("NeoVoxy");
    
    private Logger() {}
    
    public static void info(String message) {
        LOGGER.info(message);
    }
    
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }
    
    public static void warn(String message) {
        LOGGER.warn(message);
    }
    
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }
    
    public static void error(String message) {
        LOGGER.error(message);
    }
    
    public static void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }
    
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }
    
    public static void debug(String message) {
        LOGGER.debug(message);
    }
    
    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }
}
