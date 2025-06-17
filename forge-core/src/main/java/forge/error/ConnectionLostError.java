package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when an established connection is lost unexpectedly.
 * This can occur due to network issues, server crashes, or idle timeouts.
 */
public class ConnectionLostError extends ConnectionError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Reason for connection loss.
     */
    public enum Reason {
        /**
         * Network connectivity was lost.
         */
        NETWORK_FAILURE("Network connection lost"),
        
        /**
         * Server stopped responding to heartbeats.
         */
        HEARTBEAT_TIMEOUT("Server heartbeat timeout"),
        
        /**
         * Connection was idle for too long.
         */
        IDLE_TIMEOUT("Idle timeout exceeded"),
        
        /**
         * Server closed the connection.
         */
        SERVER_DISCONNECT("Server closed connection"),
        
        /**
         * Unknown reason for disconnection.
         */
        UNKNOWN("Unknown disconnection reason");
        
        private final String description;
        
        Reason(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final Reason reason;
    private final long connectionDurationMs;
    
    /**
     * Creates a new ConnectionLostError.
     * 
     * @param reason Reason for connection loss
     * @param connectionDurationMs How long the connection was active
     */
    public ConnectionLostError(Reason reason, long connectionDurationMs) {
        this(reason, connectionDurationMs, null);
    }
    
    /**
     * Creates a new ConnectionLostError with cause.
     * 
     * @param reason Reason for connection loss
     * @param connectionDurationMs How long the connection was active
     * @param cause Underlying cause
     */
    public ConnectionLostError(Reason reason, long connectionDurationMs, Throwable cause) {
        super(String.format("Connection lost: %s (duration: %d ms)", 
                          reason.getDescription(), connectionDurationMs),
              true, cause, createContext(reason, connectionDurationMs));
        this.reason = reason;
        this.connectionDurationMs = connectionDurationMs;
    }
    
    private static Map<String, Object> createContext(Reason reason, long connectionDurationMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("reason", reason);
        context.put("connectionDurationMs", connectionDurationMs);
        context.put("recoverable", true);
        return context;
    }
    
    /**
     * Gets the reason for connection loss.
     * 
     * @return Connection loss reason
     */
    public Reason getReason() {
        return reason;
    }
    
    /**
     * Gets how long the connection was active before being lost.
     * 
     * @return Connection duration in milliseconds
     */
    public long getConnectionDurationMs() {
        return connectionDurationMs;
    }
    
    @Override
    public String getUserMessage() {
        return String.format("Lost connection to server: %s. Attempting to reconnect...", 
                           reason.getDescription());
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RECONNECT;
    }
}