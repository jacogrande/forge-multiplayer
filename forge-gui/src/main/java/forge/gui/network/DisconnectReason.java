package forge.gui.network;

/**
 * Enumeration of possible disconnection reasons for classifying network failures
 * and determining appropriate reconnection strategies.
 */
public enum DisconnectReason {
    /**
     * Network connectivity issues (timeout, connection refused, etc.)
     * Recovery: Automatic reconnection with exponential backoff
     */
    NETWORK_ERROR("Network connectivity lost", true, true),
    
    /**
     * Server shutdown or restart
     * Recovery: Automatic reconnection with extended backoff
     */
    SERVER_SHUTDOWN("Server unavailable", true, true),
    
    /**
     * Client failed to respond to heartbeat
     * Recovery: Immediate reconnection attempt
     */
    CLIENT_TIMEOUT("Connection timeout", true, true),
    
    /**
     * Authentication or authorization failure
     * Recovery: User intervention required
     */
    AUTHENTICATION_FAILURE("Authentication failed", false, false),
    
    /**
     * Game state error or corruption
     * Recovery: Manual reconnection with state recovery
     */
    GAME_ERROR("Game state error", true, false),
    
    /**
     * User manually disconnected
     * Recovery: No automatic reconnection
     */
    USER_INITIATED("User disconnected", false, false);
    
    private final String description;
    private final boolean canReconnect;
    private final boolean shouldAutoReconnect;
    
    DisconnectReason(String description, boolean canReconnect, boolean shouldAutoReconnect) {
        this.description = description;
        this.canReconnect = canReconnect;
        this.shouldAutoReconnect = shouldAutoReconnect;
    }
    
    /**
     * Gets a human-readable description of the disconnection reason.
     * 
     * @return The description string
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if reconnection is possible for this disconnect reason.
     * 
     * @return true if reconnection attempts should be made
     */
    public boolean canReconnect() {
        return canReconnect;
    }
    
    /**
     * Determines if automatic reconnection should be attempted.
     * 
     * @return true if automatic reconnection is appropriate
     */
    public boolean shouldAutoReconnect() {
        return shouldAutoReconnect;
    }
    
    /**
     * Gets the recommended initial delay for reconnection attempts.
     * 
     * @return Initial delay in milliseconds
     */
    public long getInitialDelayMs() {
        switch (this) {
            case CLIENT_TIMEOUT:
                return 500; // Quick retry for timeouts
            case NETWORK_ERROR:
                return 1000; // Standard retry
            case SERVER_SHUTDOWN:
                return 2000; // Longer delay for server issues
            case GAME_ERROR:
                return 1500; // Moderate delay for game errors
            default:
                return 1000; // Default delay
        }
    }
    
    /**
     * Gets the maximum number of reconnection attempts for this reason.
     * 
     * @return Maximum attempts, or 0 if no reconnection should be attempted
     */
    public int getMaxAttempts() {
        switch (this) {
            case NETWORK_ERROR:
            case CLIENT_TIMEOUT:
                return 5; // Standard retry count
            case SERVER_SHUTDOWN:
                return 3; // Fewer attempts for server issues
            case GAME_ERROR:
                return 2; // Limited attempts for game errors
            default:
                return 0; // No automatic attempts
        }
    }
}