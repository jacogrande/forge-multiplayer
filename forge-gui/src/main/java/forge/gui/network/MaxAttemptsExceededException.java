package forge.gui.network;

/**
 * Exception thrown when the maximum number of reconnection attempts has been exceeded.
 * This indicates that automatic reconnection has failed and manual intervention may be required.
 */
public class MaxAttemptsExceededException extends ReconnectionException {
    
    private final long totalDurationMs;
    
    public MaxAttemptsExceededException(DisconnectReason reason, int maxAttempts, long totalDurationMs) {
        super(String.format("Maximum reconnection attempts exceeded: %d attempts over %d ms", 
              maxAttempts, totalDurationMs), reason, maxAttempts);
        this.totalDurationMs = totalDurationMs;
    }
    
    public MaxAttemptsExceededException(DisconnectReason reason, int maxAttempts, long totalDurationMs, Throwable cause) {
        super(String.format("Maximum reconnection attempts exceeded: %d attempts over %d ms", 
              maxAttempts, totalDurationMs), reason, maxAttempts, cause);
        this.totalDurationMs = totalDurationMs;
    }
    
    /**
     * Gets the total duration of all reconnection attempts.
     * 
     * @return Total duration in milliseconds
     */
    public long getTotalDurationMs() {
        return totalDurationMs;
    }
    
    @Override
    public boolean shouldRetry() {
        return false; // Never retry after max attempts exceeded
    }
}