package forge.gui.network;

/**
 * Enumeration representing the possible states of a network connection.
 * Provides validation for allowed state transitions to ensure connection lifecycle integrity.
 */
public enum ConnectionState {
    CONNECTING("Establishing connection"),
    CONNECTED("Connection established"),
    DISCONNECTED("Connection closed"),
    RECONNECTING("Attempting to reconnect");
    
    private final String description;
    
    ConnectionState(String description) {
        this.description = description;
    }
    
    /**
     * Gets the human-readable description of this connection state.
     * 
     * @return The description of this state
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if this state can transition to the specified new state.
     * Implements the connection state machine rules:
     * - CONNECTING can go to CONNECTED or DISCONNECTED
     * - CONNECTED can go to DISCONNECTED or RECONNECTING  
     * - DISCONNECTED can go to CONNECTING or RECONNECTING
     * - RECONNECTING can go to CONNECTED or DISCONNECTED
     * - No self-transitions allowed
     * 
     * @param newState The target state for the transition
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(ConnectionState newState) {
        if (newState == null || newState == this) {
            return false; // No null transitions or self-transitions
        }
        
        switch (this) {
            case CONNECTING:
                return newState == CONNECTED || newState == DISCONNECTED;
            case CONNECTED:
                return newState == DISCONNECTED || newState == RECONNECTING;
            case DISCONNECTED:
                return newState == CONNECTING || newState == RECONNECTING;
            case RECONNECTING:
                return newState == CONNECTED || newState == DISCONNECTED;
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}