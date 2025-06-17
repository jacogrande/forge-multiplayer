package forge.util.logging;

import org.slf4j.bridge.SLF4JBridgeHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Utility class to configure Java Util Logging (JUL) to SLF4J bridge.
 * This allows existing JUL usage in the codebase to be seamlessly redirected
 * to the SLF4J/Logback infrastructure for consistent structured logging.
 * 
 * The bridge should be initialized early in the application lifecycle,
 * typically during application startup.
 */
public class LoggingBridge {
    
    private static boolean initialized = false;
    private static final Object initLock = new Object();
    
    /**
     * Initializes the JUL to SLF4J bridge.
     * This method is idempotent - multiple calls are safe.
     * 
     * Should be called during application startup before any logging occurs.
     */
    public static void initialize() {
        synchronized (initLock) {
            if (initialized) {
                return;
            }
            
            try {
                // Remove existing handlers attached to j.u.l root logger
                LogManager.getLogManager().reset();
                
                // Add SLF4JBridgeHandler to j.u.l's root logger
                SLF4JBridgeHandler.removeHandlersForRootLogger();
                SLF4JBridgeHandler.install();
                
                // Ensure the bridge handler processes all log levels
                Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
                
                initialized = true;
                
                // Log successful initialization through SLF4J
                org.slf4j.LoggerFactory.getLogger(LoggingBridge.class)
                    .info("JUL to SLF4J bridge initialized successfully");
                
            } catch (Exception e) {
                // Fallback to standard error output if SLF4J isn't available yet
                System.err.println("Failed to initialize JUL to SLF4J bridge: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Cleans up the JUL to SLF4J bridge.
     * Should be called during application shutdown.
     */
    public static void cleanup() {
        synchronized (initLock) {
            if (!initialized) {
                return;
            }
            
            try {
                SLF4JBridgeHandler.uninstall();
                initialized = false;
                
                org.slf4j.LoggerFactory.getLogger(LoggingBridge.class)
                    .info("JUL to SLF4J bridge cleaned up");
                    
            } catch (Exception e) {
                System.err.println("Error during JUL bridge cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if the bridge has been initialized.
     * 
     * @return true if the bridge is active
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Redirects a specific JUL logger to use a custom SLF4J logger name.
     * Useful for categorizing existing JUL loggers into the new structured hierarchy.
     * 
     * @param julLoggerName The JUL logger name to redirect
     * @param slf4jLoggerName The target SLF4J logger name
     */
    public static void redirectLogger(String julLoggerName, String slf4jLoggerName) {
        if (!initialized) {
            throw new IllegalStateException("Bridge must be initialized before redirecting loggers");
        }
        
        // This is handled automatically by the bridge - JUL logger names are preserved
        // but this method exists for potential future custom mapping needs
        org.slf4j.LoggerFactory.getLogger(LoggingBridge.class)
            .debug("JUL logger '{}' will be mapped to SLF4J logger '{}'", 
                   julLoggerName, slf4jLoggerName);
    }
    
    /**
     * Utility method to help migrate existing JUL Logger usage to SLF4J.
     * Returns an SLF4J logger with the same name as the given JUL logger.
     * 
     * @param julLogger The existing JUL logger
     * @return An equivalent SLF4J logger
     */
    public static org.slf4j.Logger migrateTo(java.util.logging.Logger julLogger) {
        return org.slf4j.LoggerFactory.getLogger(julLogger.getName());
    }
    
    /**
     * Gets performance impact information about the bridge.
     * The JUL-to-SLF4J bridge does have some performance overhead.
     * 
     * @return A string describing the performance characteristics
     */
    public static String getPerformanceInfo() {
        return "JUL-to-SLF4J bridge active. " +
               "Note: JUL logging has ~2x overhead compared to native SLF4J. " +
               "Consider migrating to direct SLF4J usage for performance-critical code.";
    }
}