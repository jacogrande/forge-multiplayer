package forge.gui.network;

/**
 * Simple message class for heartbeat communication between client and server.
 * Contains minimal data to keep heartbeat traffic lightweight.
 */
public class HeartbeatMessage {
    
    /**
     * Type of heartbeat message
     */
    public enum Type {
        PING,     // Heartbeat request from server to client
        PONG      // Heartbeat response from client to server
    }
    
    private final Type type;
    private final long timestamp;
    private final String clientId;
    
    /**
     * Creates a new heartbeat message.
     * 
     * @param type The type of heartbeat message
     * @param clientId The client ID for this heartbeat
     */
    public HeartbeatMessage(Type type, String clientId) {
        this.type = type;
        this.clientId = clientId;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a heartbeat message with a specific timestamp.
     * 
     * @param type The type of heartbeat message
     * @param clientId The client ID for this heartbeat
     * @param timestamp The timestamp for the message
     */
    public HeartbeatMessage(Type type, String clientId, long timestamp) {
        this.type = type;
        this.clientId = clientId;
        this.timestamp = timestamp;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Calculates the round-trip time from this message to a response.
     * 
     * @param responseTimestamp The timestamp of the response
     * @return The round-trip time in milliseconds
     */
    public long calculateRoundTripTime(long responseTimestamp) {
        return responseTimestamp - timestamp;
    }
    
    /**
     * Creates a PING heartbeat message.
     * 
     * @param clientId The client to ping
     * @return A new PING heartbeat message
     */
    public static HeartbeatMessage createPing(String clientId) {
        return new HeartbeatMessage(Type.PING, clientId);
    }
    
    /**
     * Creates a PONG response to a PING message.
     * 
     * @param clientId The client sending the response
     * @return A new PONG heartbeat message
     */
    public static HeartbeatMessage createPong(String clientId) {
        return new HeartbeatMessage(Type.PONG, clientId);
    }
    
    @Override
    public String toString() {
        return String.format("HeartbeatMessage{type=%s, clientId='%s', timestamp=%d}", 
            type, clientId, timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HeartbeatMessage that = (HeartbeatMessage) obj;
        return timestamp == that.timestamp &&
               type == that.type &&
               clientId != null ? clientId.equals(that.clientId) : that.clientId == null;
    }
    
    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}