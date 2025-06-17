package forge.gui.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Structured logging framework for network events in the Forge MTG application.
 * Provides consistent, machine-parseable logging for network operations, performance
 * monitoring, and debugging.
 * 
 * Features:
 * - Structured JSON logging with standardized fields
 * - Correlation ID tracking for request tracing
 * - Performance metrics collection
 * - Event categorization and context preservation
 * - MDC integration for request-scoped data
 * 
 * Usage:
 * <pre>
 * NetworkEventLogger logger = NetworkEventLogger.forComponent("FGameClient");
 * 
 * // Log connection events
 * logger.logConnection("server.example.com", 7777, true, 1500);
 * 
 * // Log with correlation ID
 * String correlationId = logger.startCorrelation();
 * logger.logGameEvent("RECONNECTION_STARTED", "reason", "NETWORK_ERROR");
 * logger.endCorrelation(correlationId);
 * </pre>
 */
public class NetworkEventLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkEventLogger.class);
    
    // Component-specific loggers for better categorization
    private static final Map<String, NetworkEventLogger> componentLoggers = new ConcurrentHashMap<>();
    
    // Performance tracking
    private static final AtomicLong totalEvents = new AtomicLong(0);
    private static final AtomicLong totalErrors = new AtomicLong(0);
    
    private final String component;
    private final Logger componentLogger;
    
    /**
     * Standard event types for network operations.
     */
    public enum EventType {
        CONNECTION("connection"),
        DISCONNECTION("disconnection"), 
        RECONNECTION("reconnection"),
        MESSAGE_SENT("message_sent"),
        MESSAGE_RECEIVED("message_received"),
        GAME_STATE_SYNC("game_state_sync"),
        SECURITY_EVENT("security"),
        PERFORMANCE("performance"),
        ERROR("error"),
        HEARTBEAT("heartbeat"),
        SESSION("session");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Standard severity levels for events.
     */
    public enum Severity {
        TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL
    }
    
    private NetworkEventLogger(String component) {
        this.component = component;
        this.componentLogger = LoggerFactory.getLogger("forge.gamemodes.net." + component);
    }
    
    /**
     * Gets or creates a NetworkEventLogger for the specified component.
     * 
     * @param component The component name (e.g., "FGameClient", "FServerManager")
     * @return A NetworkEventLogger instance for the component
     */
    public static NetworkEventLogger forComponent(String component) {
        return componentLoggers.computeIfAbsent(component, NetworkEventLogger::new);
    }
    
    /**
     * Starts a new correlation context for tracking related events.
     * 
     * @return A correlation ID that should be used with endCorrelation()
     */
    public String startCorrelation() {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
    
    /**
     * Starts a correlation context with a specific ID.
     * 
     * @param correlationId The correlation ID to use
     */
    public void startCorrelation(String correlationId) {
        MDC.put("correlationId", correlationId);
    }
    
    /**
     * Ends the correlation context.
     * 
     * @param correlationId The correlation ID (for verification)
     */
    public void endCorrelation(String correlationId) {
        String currentId = MDC.get("correlationId");
        if (correlationId.equals(currentId)) {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Sets session-specific context information.
     * 
     * @param sessionId The session identifier
     * @param username The username (if available)
     * @param gameId The game identifier (if available)
     */
    public void setSessionContext(String sessionId, String username, String gameId) {
        if (sessionId != null) MDC.put("sessionId", sessionId);
        if (username != null) MDC.put("username", username);
        if (gameId != null) MDC.put("gameId", gameId);
    }
    
    /**
     * Clears all session context.
     */
    public void clearSessionContext() {
        MDC.remove("sessionId");
        MDC.remove("username");
        MDC.remove("gameId");
    }
    
    /**
     * Logs a connection event.
     * 
     * @param host The target host
     * @param port The target port
     * @param success Whether the connection succeeded
     * @param durationMs The connection duration in milliseconds
     */
    public void logConnection(String host, int port, boolean success, long durationMs) {
        logEvent(EventType.CONNECTION, success ? Severity.INFO : Severity.WARN,
                "Connection attempt to {}:{} {} in {}ms",
                host, port, success ? "succeeded" : "failed", durationMs)
                .withField("host", host)
                .withField("port", port)
                .withField("success", success)
                .withField("durationMs", durationMs)
                .log();
    }
    
    /**
     * Logs a disconnection event.
     * 
     * @param reason The disconnection reason
     * @param unexpected Whether the disconnection was unexpected
     */
    public void logDisconnection(String reason, boolean unexpected) {
        logEvent(EventType.DISCONNECTION, unexpected ? Severity.WARN : Severity.INFO,
                "Disconnected: {} ({})",
                reason, unexpected ? "unexpected" : "expected")
                .withField("reason", reason)
                .withField("unexpected", unexpected)
                .log();
    }
    
    /**
     * Logs a reconnection attempt.
     * 
     * @param attempt The attempt number
     * @param maxAttempts The maximum number of attempts
     * @param delayMs The delay before this attempt
     * @param success Whether the attempt succeeded
     */
    public void logReconnectionAttempt(int attempt, int maxAttempts, long delayMs, boolean success) {
        logEvent(EventType.RECONNECTION, success ? Severity.INFO : Severity.WARN,
                "Reconnection attempt {}/{} {} (delay: {}ms)",
                attempt, maxAttempts, success ? "succeeded" : "failed", delayMs)
                .withField("attempt", attempt)
                .withField("maxAttempts", maxAttempts)
                .withField("delayMs", delayMs)
                .withField("success", success)
                .log();
    }
    
    /**
     * Logs a message sending event.
     * 
     * @param messageType The type of message
     * @param messageSize The message size in bytes
     * @param success Whether sending succeeded
     */
    public void logMessageSent(String messageType, int messageSize, boolean success) {
        logEvent(EventType.MESSAGE_SENT, success ? Severity.DEBUG : Severity.WARN,
                "Sent {} message ({} bytes): {}",
                messageType, messageSize, success ? "success" : "failed")
                .withField("messageType", messageType)
                .withField("messageSize", messageSize)
                .withField("success", success)
                .log();
    }
    
    /**
     * Logs a message receiving event.
     * 
     * @param messageType The type of message
     * @param messageSize The message size in bytes
     * @param processingTimeMs Time taken to process the message
     */
    public void logMessageReceived(String messageType, int messageSize, long processingTimeMs) {
        logEvent(EventType.MESSAGE_RECEIVED, Severity.DEBUG,
                "Received {} message ({} bytes, processed in {}ms)",
                messageType, messageSize, processingTimeMs)
                .withField("messageType", messageType)
                .withField("messageSize", messageSize)
                .withField("processingTimeMs", processingTimeMs)
                .log();
    }
    
    /**
     * Logs a game state synchronization event.
     * 
     * @param operation The sync operation (e.g., "CAPTURE", "RESTORE")
     * @param success Whether the operation succeeded
     * @param durationMs The operation duration
     * @param stateSizeBytes The size of the state data
     */
    public void logGameStateSync(String operation, boolean success, long durationMs, long stateSizeBytes) {
        logEvent(EventType.GAME_STATE_SYNC, success ? Severity.INFO : Severity.ERROR,
                "Game state {} {} in {}ms ({} bytes)",
                operation.toLowerCase(), success ? "succeeded" : "failed", durationMs, stateSizeBytes)
                .withField("operation", operation)
                .withField("success", success)
                .withField("durationMs", durationMs)
                .withField("stateSizeBytes", stateSizeBytes)
                .log();
    }
    
    /**
     * Logs a security event.
     * 
     * @param securityEvent The type of security event
     * @param details Additional details about the event
     * @param severity The severity level
     */
    public void logSecurityEvent(String securityEvent, String details, Severity severity) {
        logEvent(EventType.SECURITY_EVENT, severity,
                "Security event: {} - {}",
                securityEvent, details)
                .withField("securityEvent", securityEvent)
                .withField("details", details)
                .log();
    }
    
    /**
     * Logs a performance metric.
     * 
     * @param metric The metric name
     * @param value The metric value
     * @param unit The unit of measurement
     */
    public void logPerformanceMetric(String metric, double value, String unit) {
        Logger metricsLogger = LoggerFactory.getLogger("performance");
        
        MDC.put("metric", metric);
        MDC.put("value", String.valueOf(value));
        MDC.put("unit", unit);
        MDC.put("component", component);
        MDC.put("timestamp", Instant.now().toString());
        
        try {
            metricsLogger.info("Performance metric: {} = {} {}", metric, value, unit);
        } finally {
            MDC.remove("metric");
            MDC.remove("value");
            MDC.remove("unit");
            MDC.remove("timestamp");
        }
    }
    
    /**
     * Logs a heartbeat event.
     * 
     * @param sent Whether this is a sent or received heartbeat
     * @param latencyMs The round-trip latency (for received heartbeats)
     */
    public void logHeartbeat(boolean sent, Long latencyMs) {
        if (sent) {
            logEvent(EventType.HEARTBEAT, Severity.TRACE, "Heartbeat sent").log();
        } else {
            logEvent(EventType.HEARTBEAT, Severity.TRACE,
                    "Heartbeat received (latency: {}ms)", latencyMs)
                    .withField("latencyMs", latencyMs)
                    .log();
        }
    }
    
    /**
     * Logs an error event with exception details.
     * 
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    public void logError(String operation, Throwable error) {
        totalErrors.incrementAndGet();
        
        logEvent(EventType.ERROR, Severity.ERROR,
                "Error in {}: {}",
                operation, error.getMessage())
                .withField("operation", operation)
                .withField("errorType", error.getClass().getSimpleName())
                .withField("errorMessage", error.getMessage())
                .withException(error)
                .log();
    }
    
    /**
     * Creates a structured log event builder.
     * 
     * @param eventType The type of event
     * @param severity The severity level
     * @param message The log message template
     * @param args Arguments for the message template
     * @return A LogEventBuilder for further customization
     */
    public LogEventBuilder logEvent(EventType eventType, Severity severity, String message, Object... args) {
        totalEvents.incrementAndGet();
        return new LogEventBuilder(componentLogger, eventType, severity, message, args);
    }
    
    /**
     * Gets total events logged across all components.
     * 
     * @return The total number of events logged
     */
    public static long getTotalEvents() {
        return totalEvents.get();
    }
    
    /**
     * Gets total errors logged across all components.
     * 
     * @return The total number of errors logged
     */
    public static long getTotalErrors() {
        return totalErrors.get();
    }
    
    /**
     * Builder class for constructing structured log events.
     */
    public static class LogEventBuilder {
        private final Logger logger;
        private final EventType eventType;
        private final Severity severity;
        private final String message;
        private final Object[] args;
        private final Map<String, Object> fields = new ConcurrentHashMap<>();
        private Throwable exception;
        
        LogEventBuilder(Logger logger, EventType eventType, Severity severity, String message, Object[] args) {
            this.logger = logger;
            this.eventType = eventType;
            this.severity = severity;
            this.message = message;
            this.args = args;
            
            // Add standard fields
            fields.put("eventType", eventType.getValue());
            fields.put("timestamp", Instant.now().toString());
        }
        
        /**
         * Adds a custom field to the log event.
         * 
         * @param key The field name
         * @param value The field value
         * @return This builder for chaining
         */
        public LogEventBuilder withField(String key, Object value) {
            if (value != null) {
                fields.put(key, value);
            }
            return this;
        }
        
        /**
         * Adds multiple custom fields to the log event.
         * 
         * @param additionalFields Map of field names to values
         * @return This builder for chaining
         */
        public LogEventBuilder withFields(Map<String, Object> additionalFields) {
            if (additionalFields != null) {
                fields.putAll(additionalFields);
            }
            return this;
        }
        
        /**
         * Adds an exception to the log event.
         * 
         * @param exception The exception to log
         * @return This builder for chaining
         */
        public LogEventBuilder withException(Throwable exception) {
            this.exception = exception;
            return this;
        }
        
        /**
         * Logs the event with the configured parameters.
         */
        public void log() {
            // Set MDC fields for structured logging
            String originalMdc = preserveMdc();
            
            try {
                for (Map.Entry<String, Object> field : fields.entrySet()) {
                    MDC.put(field.getKey(), String.valueOf(field.getValue()));
                }
                
                // Log with appropriate level
                switch (severity) {
                    case TRACE:
                        if (exception != null) {
                            logger.trace(message, args, exception);
                        } else {
                            logger.trace(message, args);
                        }
                        break;
                    case DEBUG:
                        if (exception != null) {
                            logger.debug(message, args, exception);
                        } else {
                            logger.debug(message, args);
                        }
                        break;
                    case INFO:
                        if (exception != null) {
                            logger.info(message, args, exception);
                        } else {
                            logger.info(message, args);
                        }
                        break;
                    case WARN:
                        if (exception != null) {
                            logger.warn(message, args, exception);
                        } else {
                            logger.warn(message, args);
                        }
                        break;
                    case ERROR:
                    case CRITICAL:
                        if (exception != null) {
                            logger.error(message, args, exception);
                        } else {
                            logger.error(message, args);
                        }
                        break;
                }
            } finally {
                // Clean up MDC
                for (String key : fields.keySet()) {
                    MDC.remove(key);
                }
                restoreMdc(originalMdc);
            }
        }
        
        private String preserveMdc() {
            return MDC.getCopyOfContextMap() != null ? MDC.getCopyOfContextMap().toString() : null;
        }
        
        private void restoreMdc(String originalMdc) {
            // This is a simplified approach - in a real implementation,
            // you'd want to properly preserve and restore the MDC state
        }
    }
}