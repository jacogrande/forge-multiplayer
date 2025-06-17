package forge.gui.network;

/**
 * Interface for handling heartbeat-related operations.
 * Implementations define how heartbeats are sent and timeouts are handled.
 */
public interface HeartbeatHandler {
    
    /**
     * Sends a heartbeat message to the specified client.
     * 
     * @param clientId The unique identifier of the client to send heartbeat to
     */
    void sendHeartbeat(String clientId);
    
    /**
     * Handles a connection timeout for the specified client.
     * Called when a client fails to respond to heartbeats within the timeout period.
     * 
     * @param clientId The unique identifier of the client that timed out
     */
    void handleConnectionTimeout(String clientId);
}