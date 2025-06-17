package forge.gui.util;

import forge.util.logging.LoggingBridge;
import forge.gui.network.NetworkEventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the structured logging system for the Forge application.
 * Should be called early in the application lifecycle to ensure proper
 * logging configuration.
 * 
 * This class handles:
 * - JUL-to-SLF4J bridge initialization
 * - Logging system health checks
 * - Structured logging system setup
 */
public class LoggingInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingInitializer.class);
    private static boolean initialized = false;
    
    /**
     * Initializes the structured logging system.
     * This method is idempotent and can be safely called multiple times.
     * 
     * @return true if initialization succeeded, false otherwise
     */
    public static boolean initialize() {
        if (initialized) {
            logger.debug("Logging system already initialized");
            return true;
        }
        
        try {
            // Initialize JUL-to-SLF4J bridge
            LoggingBridge.initialize();
            
            // Verify structured logging components
            NetworkEventLogger testLogger = NetworkEventLogger.forComponent("System");
            testLogger.logEvent(NetworkEventLogger.EventType.SESSION, 
                              NetworkEventLogger.Severity.INFO,
                              "Structured logging system initialized successfully")
                     .withField("initializationTime", System.currentTimeMillis())
                     .withField("bridgeInitialized", LoggingBridge.isInitialized())
                     .log();
            
            initialized = true;
            logger.info("Structured logging system initialized successfully");
            
            // Log performance information
            logger.info("Logging bridge performance: {}", LoggingBridge.getPerformanceInfo());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize structured logging system", e);
            return false;
        }
    }
    
    /**
     * Shuts down the logging system cleanly.
     * Should be called during application shutdown.
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        try {
            NetworkEventLogger testLogger = NetworkEventLogger.forComponent("System");
            testLogger.logEvent(NetworkEventLogger.EventType.SESSION,
                              NetworkEventLogger.Severity.INFO,
                              "Structured logging system shutting down")
                     .withField("shutdownTime", System.currentTimeMillis())
                     .log();
            
            LoggingBridge.cleanup();
            initialized = false;
            
            logger.info("Structured logging system shutdown complete");
            
        } catch (Exception e) {
            System.err.println("Error during logging system shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if the logging system is properly initialized.
     * 
     * @return true if initialized and healthy
     */
    public static boolean isInitialized() {
        return initialized && LoggingBridge.isInitialized();
    }
    
    /**
     * Performs a health check of the logging system.
     * 
     * @return true if the logging system is healthy
     */
    public static boolean healthCheck() {
        if (!initialized) {
            return false;
        }
        
        try {
            // Test SLF4J logging
            logger.debug("Logging health check - SLF4J");
            
            // Test JUL bridging
            java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("HealthCheck");
            julLogger.info("Logging health check - JUL bridge");
            
            // Test structured logging
            NetworkEventLogger networkLogger = NetworkEventLogger.forComponent("HealthCheck");
            networkLogger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                                 NetworkEventLogger.Severity.DEBUG,
                                 "Logging health check - structured logging")
                         .withField("checkTime", System.currentTimeMillis())
                         .log();
            
            return true;
            
        } catch (Exception e) {
            logger.error("Logging system health check failed", e);
            return false;
        }
    }
    
    /**
     * Gets information about the current logging configuration.
     * 
     * @return A string describing the logging setup
     */
    public static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Structured Logging System Status:\n");
        info.append("- Initialized: ").append(initialized).append("\n");
        info.append("- JUL Bridge: ").append(LoggingBridge.isInitialized()).append("\n");
        info.append("- Total Events: ").append(NetworkEventLogger.getTotalEvents()).append("\n");
        info.append("- Total Errors: ").append(NetworkEventLogger.getTotalErrors()).append("\n");
        info.append("- Bridge Performance: ").append(LoggingBridge.getPerformanceInfo()).append("\n");
        
        return info.toString();
    }
    
    /**
     * Enables verbose logging for debugging purposes.
     * This will increase log output significantly.
     */
    public static void enableVerboseLogging() {
        logger.info("Enabling verbose logging mode");
        
        // Set system property that logback can check
        System.setProperty("forge.logging.verbose", "true");
        
        // You could also programmatically adjust logger levels here
        // This would require logback-specific code or using SLF4J 2.0+ features
    }
    
    /**
     * Disables verbose logging to reduce log output.
     */
    public static void disableVerboseLogging() {
        logger.info("Disabling verbose logging mode");
        System.setProperty("forge.logging.verbose", "false");
    }
    
    /**
     * Creates a shutdown hook that will properly clean up logging.
     * Call this during application initialization to ensure clean shutdown.
     */
    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutdown detected, cleaning up logging system");
            shutdown();
        }, "LoggingCleanup"));
    }
}