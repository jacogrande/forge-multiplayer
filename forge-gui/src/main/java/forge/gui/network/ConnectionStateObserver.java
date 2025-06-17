package forge.gui.network;

/**
 * Observer interface for receiving notifications about connection state changes.
 * Implementations can register with ConnectionManager to receive real-time updates
 * about client connection state transitions.
 */
public interface ConnectionStateObserver {
    
    /**
     * Called when a client's connection state changes.
     * 
     * @param clientId The unique identifier of the client whose state changed
     * @param oldState The previous connection state (null for new registrations)
     * @param newState The new connection state
     */
    void onConnectionStateChanged(String clientId, ConnectionState oldState, ConnectionState newState);
}