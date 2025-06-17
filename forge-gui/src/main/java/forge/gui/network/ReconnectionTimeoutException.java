package forge.gui.network;

/**
 * Exception thrown when a reconnection attempt times out.
 * This can occur during connection establishment or handshake phase.
 */
public class ReconnectionTimeoutException extends ReconnectionException {
    
    private final long timeoutMs;
    
    public ReconnectionTimeoutException(DisconnectReason reason, int attemptNumber, long timeoutMs) {
        super(String.format("Reconnection attempt %d timed out after %d ms", 
              attemptNumber, timeoutMs), reason, attemptNumber);
        this.timeoutMs = timeoutMs;
    }
    
    public ReconnectionTimeoutException(DisconnectReason reason, int attemptNumber, long timeoutMs, Throwable cause) {
        super(String.format("Reconnection attempt %d timed out after %d ms", 
              attemptNumber, timeoutMs), reason, attemptNumber, cause);
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Gets the timeout duration that was exceeded.
     * 
     * @return Timeout duration in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
}