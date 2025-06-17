package forge.error;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base abstract class for all network-related errors in the Forge multiplayer system.
 * Provides comprehensive error classification, severity levels, and recovery strategies.
 * 
 * This hierarchy supports the error handling requirements for LAN multiplayer functionality,
 * including connection management, serialization, security, game state synchronization,
 * and protocol-level errors.
 */
public abstract class NetworkError extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    // Error tracking
    private static final AtomicLong ERROR_COUNTER = new AtomicLong(0);
    
    private final long errorId;
    private final Severity severity;
    private final Type type;
    private final Instant timestamp;
    private final Map<String, Object> context;
    private final boolean recoverable;
    private final String errorCode;
    
    /**
     * Enumeration of error severity levels.
     */
    public enum Severity {
        /**
         * Informational message, no action required.
         */
        INFO(0, "Information"),
        
        /**
         * Warning condition that may require attention.
         */
        WARN(1, "Warning"),
        
        /**
         * Error condition that impacts functionality.
         */
        ERROR(2, "Error"),
        
        /**
         * Critical error that may cause system failure.
         */
        CRITICAL(3, "Critical");
        
        private final int level;
        private final String displayName;
        
        Severity(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isMoreSevereThan(Severity other) {
            return this.level > other.level;
        }
    }
    
    /**
     * Enumeration of error types for classification.
     */
    public enum Type {
        /**
         * Network connection-related errors.
         */
        CONNECTION("CON", "Connection Error"),
        
        /**
         * Data serialization/deserialization errors.
         */
        SERIALIZATION("SER", "Serialization Error"),
        
        /**
         * Security and authentication errors.
         */
        SECURITY("SEC", "Security Error"),
        
        /**
         * Game state synchronization errors.
         */
        GAME_STATE("GST", "Game State Error"),
        
        /**
         * Network protocol violations.
         */
        PROTOCOL("PRO", "Protocol Error"),
        
        /**
         * Operation timeout errors.
         */
        TIMEOUT("TIM", "Timeout Error"),
        
        /**
         * Authentication and authorization failures.
         */
        AUTHENTICATION("AUT", "Authentication Error"),
        
        /**
         * Application-level errors.
         */
        APPLICATION("APP", "Application Error");
        
        private final String code;
        private final String displayName;
        
        Type(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Creates a new NetworkError with the specified parameters.
     * 
     * @param message Human-readable error message
     * @param severity Error severity level
     * @param type Error type classification
     * @param recoverable Whether the error can be recovered from
     */
    protected NetworkError(String message, Severity severity, Type type, boolean recoverable) {
        this(message, severity, type, recoverable, null, null);
    }
    
    /**
     * Creates a new NetworkError with the specified parameters and cause.
     * 
     * @param message Human-readable error message
     * @param severity Error severity level
     * @param type Error type classification
     * @param recoverable Whether the error can be recovered from
     * @param cause The underlying cause of this error
     */
    protected NetworkError(String message, Severity severity, Type type, boolean recoverable, Throwable cause) {
        this(message, severity, type, recoverable, cause, null);
    }
    
    /**
     * Creates a new NetworkError with the specified parameters, cause, and context.
     * 
     * @param message Human-readable error message
     * @param severity Error severity level
     * @param type Error type classification
     * @param recoverable Whether the error can be recovered from
     * @param cause The underlying cause of this error
     * @param context Additional context information
     */
    protected NetworkError(String message, Severity severity, Type type, boolean recoverable, 
                         Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorId = ERROR_COUNTER.incrementAndGet();
        this.severity = severity;
        this.type = type;
        this.timestamp = Instant.now();
        this.recoverable = recoverable;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        this.errorCode = generateErrorCode();
    }
    
    /**
     * Generates a unique error code based on type and ID.
     * Format: TYPE-YYYYMMDD-ID
     */
    private String generateErrorCode() {
        java.util.Date date = java.util.Date.from(timestamp);
        return String.format("%s-%tY%<tm%<td-%06d", 
                           type.getCode(), 
                           date, 
                           errorId);
    }
    
    /**
     * Gets the unique error ID.
     * 
     * @return Unique error identifier
     */
    public long getErrorId() {
        return errorId;
    }
    
    /**
     * Gets the error severity level.
     * 
     * @return Error severity
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * Gets the error type classification.
     * 
     * @return Error type
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Gets the timestamp when this error occurred.
     * 
     * @return Error timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the error context information.
     * 
     * @return Unmodifiable map of context data
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
    
    /**
     * Adds context information to this error.
     * 
     * @param key Context key
     * @param value Context value
     * @return This error instance for chaining
     */
    public NetworkError withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Determines if this error can be recovered from.
     * 
     * @return true if recovery is possible
     */
    public boolean isRecoverable() {
        return recoverable;
    }
    
    /**
     * Gets the unique error code.
     * 
     * @return Error code string
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets a context value by key.
     * 
     * @param key Context key
     * @return Optional containing the value if present
     */
    public Optional<Object> getContextValue(String key) {
        return Optional.ofNullable(context.get(key));
    }
    
    /**
     * Gets a typed context value by key.
     * 
     * @param key Context key
     * @param type Expected type class
     * @param <T> Type parameter
     * @return Optional containing the typed value if present and correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getContextValue(String key, Class<T> type) {
        Object value = context.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Returns a user-friendly error message suitable for display.
     * Subclasses should override this to provide specific user guidance.
     * 
     * @return User-friendly error message
     */
    public String getUserMessage() {
        return String.format("[%s] %s", severity.getDisplayName(), getMessage());
    }
    
    /**
     * Returns a technical error message with full details for logging.
     * 
     * @return Technical error details
     */
    public String getTechnicalMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetworkError[").append(errorCode).append("] ");
        sb.append("Type: ").append(type.getDisplayName()).append(", ");
        sb.append("Severity: ").append(severity).append(", ");
        sb.append("Recoverable: ").append(recoverable).append(", ");
        sb.append("Message: ").append(getMessage());
        
        if (!context.isEmpty()) {
            sb.append(", Context: ").append(context);
        }
        
        if (getCause() != null) {
            sb.append(", Cause: ").append(getCause().getClass().getSimpleName());
            sb.append(" - ").append(getCause().getMessage());
        }
        
        return sb.toString();
    }
    
    /**
     * Determines the recommended recovery strategy for this error.
     * Subclasses should override to provide specific recovery guidance.
     * 
     * @return Recommended recovery strategy
     */
    public abstract RecoveryStrategy getRecommendedRecoveryStrategy();
    
    /**
     * Enumeration of possible recovery strategies.
     */
    public enum RecoveryStrategy {
        /**
         * No recovery possible, user intervention required.
         */
        NONE,
        
        /**
         * Retry the operation with exponential backoff.
         */
        RETRY,
        
        /**
         * Reconnect to the server.
         */
        RECONNECT,
        
        /**
         * Resynchronize game state.
         */
        RESYNC,
        
        /**
         * Fall back to alternative functionality.
         */
        FALLBACK,
        
        /**
         * Require user intervention.
         */
        USER_INTERVENTION
    }
    
    @Override
    public String toString() {
        return getTechnicalMessage();
    }
}