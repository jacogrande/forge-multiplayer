package forge.error;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Result of an error recovery attempt.
 * Contains information about the success or failure of the recovery operation.
 */
public class RecoveryResult {
    
    /**
     * Type of recovery result.
     */
    public enum Type {
        /**
         * Recovery was successful.
         */
        SUCCESS,
        
        /**
         * Recovery failed, but can be retried.
         */
        RETRY,
        
        /**
         * Recovery failed permanently.
         */
        FAILURE,
        
        /**
         * Recovery was partially successful.
         */
        PARTIAL_SUCCESS,
        
        /**
         * Recovery is not applicable for this error.
         */
        NOT_APPLICABLE
    }
    
    private final Type type;
    private final String message;
    private final long durationMs;
    private final Throwable cause;
    private final Map<String, Object> metadata;
    private final Instant timestamp;
    
    /**
     * Creates a successful recovery result.
     * 
     * @param message Success message
     * @param durationMs Recovery duration in milliseconds
     * @return Success result
     */
    public static RecoveryResult success(String message, long durationMs) {
        return new RecoveryResult(Type.SUCCESS, message, durationMs, null, null);
    }
    
    /**
     * Creates a successful recovery result with metadata.
     * 
     * @param message Success message
     * @param durationMs Recovery duration in milliseconds
     * @param metadata Additional metadata
     * @return Success result
     */
    public static RecoveryResult success(String message, long durationMs, Map<String, Object> metadata) {
        return new RecoveryResult(Type.SUCCESS, message, durationMs, null, metadata);
    }
    
    /**
     * Creates a retry recovery result.
     * 
     * @param message Retry message
     * @param durationMs Recovery attempt duration in milliseconds
     * @return Retry result
     */
    public static RecoveryResult retry(String message, long durationMs) {
        return new RecoveryResult(Type.RETRY, message, durationMs, null, null);
    }
    
    /**
     * Creates a retry recovery result with cause.
     * 
     * @param message Retry message
     * @param durationMs Recovery attempt duration in milliseconds
     * @param cause Cause of the retry
     * @return Retry result
     */
    public static RecoveryResult retry(String message, long durationMs, Throwable cause) {
        return new RecoveryResult(Type.RETRY, message, durationMs, cause, null);
    }
    
    /**
     * Creates a failure recovery result.
     * 
     * @param message Failure message
     * @param durationMs Recovery attempt duration in milliseconds
     * @return Failure result
     */
    public static RecoveryResult failure(String message, long durationMs) {
        return new RecoveryResult(Type.FAILURE, message, durationMs, null, null);
    }
    
    /**
     * Creates a failure recovery result with cause.
     * 
     * @param message Failure message
     * @param durationMs Recovery attempt duration in milliseconds
     * @param cause Cause of the failure
     * @return Failure result
     */
    public static RecoveryResult failure(String message, long durationMs, Throwable cause) {
        return new RecoveryResult(Type.FAILURE, message, durationMs, cause, null);
    }
    
    /**
     * Creates a partial success recovery result.
     * 
     * @param message Partial success message
     * @param durationMs Recovery duration in milliseconds
     * @param metadata Additional metadata about what was recovered
     * @return Partial success result
     */
    public static RecoveryResult partialSuccess(String message, long durationMs, Map<String, Object> metadata) {
        return new RecoveryResult(Type.PARTIAL_SUCCESS, message, durationMs, null, metadata);
    }
    
    /**
     * Creates a not applicable recovery result.
     * 
     * @param message Explanation message
     * @return Not applicable result
     */
    public static RecoveryResult notApplicable(String message) {
        return new RecoveryResult(Type.NOT_APPLICABLE, message, 0, null, null);
    }
    
    /**
     * Creates a new RecoveryResult.
     * 
     * @param type Result type
     * @param message Result message
     * @param durationMs Operation duration in milliseconds
     * @param cause Underlying cause (for failures)
     * @param metadata Additional metadata
     */
    private RecoveryResult(Type type, String message, long durationMs, Throwable cause, Map<String, Object> metadata) {
        this.type = type;
        this.message = message;
        this.durationMs = durationMs;
        this.cause = cause;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    /**
     * Gets the result type.
     * 
     * @return Result type
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Gets the result message.
     * 
     * @return Result message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the operation duration.
     * 
     * @return Duration in milliseconds
     */
    public long getDurationMs() {
        return durationMs;
    }
    
    /**
     * Gets the underlying cause for failures.
     * 
     * @return Optional containing the cause if present
     */
    public Optional<Throwable> getCause() {
        return Optional.ofNullable(cause);
    }
    
    /**
     * Gets the result metadata.
     * 
     * @return Unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    
    /**
     * Gets the timestamp when this result was created.
     * 
     * @return Result timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Determines if this result indicates success.
     * 
     * @return true if the result is successful
     */
    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }
    
    /**
     * Determines if this result indicates failure.
     * 
     * @return true if the result is a failure
     */
    public boolean isFailure() {
        return type == Type.FAILURE;
    }
    
    /**
     * Determines if this result indicates that retry is possible.
     * 
     * @return true if retry is recommended
     */
    public boolean canRetry() {
        return type == Type.RETRY;
    }
    
    /**
     * Determines if this result indicates partial success.
     * 
     * @return true if the result is partial success
     */
    public boolean isPartialSuccess() {
        return type == Type.PARTIAL_SUCCESS;
    }
    
    /**
     * Gets a metadata value.
     * 
     * @param key Metadata key
     * @return Optional containing the value if present
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }
    
    /**
     * Gets a typed metadata value.
     * 
     * @param key Metadata key
     * @param type Expected type
     * @param <T> Type parameter
     * @return Optional containing the typed value if present and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Creates a new result with additional metadata.
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return New result with additional metadata
     */
    public RecoveryResult withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new RecoveryResult(type, message, durationMs, cause, newMetadata);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RecoveryResult{");
        sb.append("type=").append(type);
        sb.append(", message='").append(message).append('\'');
        sb.append(", durationMs=").append(durationMs);
        if (cause != null) {
            sb.append(", cause=").append(cause.getClass().getSimpleName());
        }
        if (!metadata.isEmpty()) {
            sb.append(", metadata=").append(metadata);
        }
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}