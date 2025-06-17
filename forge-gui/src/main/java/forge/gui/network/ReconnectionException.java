package forge.gui.network;

/**
 * Base exception class for reconnection-related errors.
 * Provides structured error handling for different reconnection failure scenarios.
 */
public class ReconnectionException extends Exception {
    
    private final DisconnectReason reason;
    private final int attemptNumber;
    
    public ReconnectionException(String message, DisconnectReason reason) {
        this(message, reason, 0, null);
    }
    
    public ReconnectionException(String message, DisconnectReason reason, Throwable cause) {
        this(message, reason, 0, cause);
    }
    
    public ReconnectionException(String message, DisconnectReason reason, int attemptNumber) {
        this(message, reason, attemptNumber, null);
    }
    
    public ReconnectionException(String message, DisconnectReason reason, int attemptNumber, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.attemptNumber = attemptNumber;
    }
    
    /**
     * Gets the reason for the original disconnection.
     * 
     * @return The disconnect reason
     */
    public DisconnectReason getReason() {
        return reason;
    }
    
    /**
     * Gets the attempt number when this exception occurred.
     * 
     * @return The attempt number (0 for initial failure)
     */
    public int getAttemptNumber() {
        return attemptNumber;
    }
    
    /**
     * Determines if another reconnection attempt should be made.
     * 
     * @return true if retry is recommended
     */
    public boolean shouldRetry() {
        return reason != null && reason.canReconnect() && attemptNumber < reason.getMaxAttempts();
    }
}