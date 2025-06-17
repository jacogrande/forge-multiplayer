package forge.gui.network;

/**
 * Represents a state transition event with detailed information about the change.
 * Immutable value object containing all relevant transition data.
 */
public final class StateTransition {
    
    private final String clientId;
    private final ConnectionState fromState;
    private final ConnectionState toState;
    private final long timestamp;
    private final int sequenceNumber;
    private final String reason;
    
    /**
     * Creates a new state transition record.
     * 
     * @param clientId The client whose state transitioned
     * @param fromState The previous state (null for initialization)
     * @param toState The new state
     * @param sequenceNumber The sequence number for ordering
     * @param reason Optional reason for the transition
     */
    public StateTransition(String clientId, ConnectionState fromState, ConnectionState toState, 
                          int sequenceNumber, String reason) {
        this.clientId = clientId;
        this.fromState = fromState;
        this.toState = toState;
        this.timestamp = System.currentTimeMillis();
        this.sequenceNumber = sequenceNumber;
        this.reason = reason;
    }
    
    /**
     * Creates a new state transition record without a specific reason.
     * 
     * @param clientId The client whose state transitioned
     * @param fromState The previous state
     * @param toState The new state
     * @param sequenceNumber The sequence number for ordering
     */
    public StateTransition(String clientId, ConnectionState fromState, ConnectionState toState, int sequenceNumber) {
        this(clientId, fromState, toState, sequenceNumber, null);
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public ConnectionState getFromState() {
        return fromState;
    }
    
    public ConnectionState getToState() {
        return toState;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public String getReason() {
        return reason;
    }
    
    /**
     * Calculates the duration between this transition and another.
     * 
     * @param other The other transition to compare with
     * @return The duration in milliseconds
     */
    public long getDurationSince(StateTransition other) {
        return this.timestamp - other.timestamp;
    }
    
    /**
     * Checks if this transition represents an initialization (from null state).
     * 
     * @return true if this is an initialization transition
     */
    public boolean isInitialization() {
        return fromState == null;
    }
    
    /**
     * Checks if this transition represents a disconnection.
     * 
     * @return true if transitioning to DISCONNECTED state
     */
    public boolean isDisconnection() {
        return toState == ConnectionState.DISCONNECTED;
    }
    
    /**
     * Checks if this transition represents a successful connection.
     * 
     * @return true if transitioning to CONNECTED state
     */
    public boolean isConnection() {
        return toState == ConnectionState.CONNECTED;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StateTransition{");
        sb.append("client='").append(clientId).append('\'');
        sb.append(", ").append(fromState).append(" -> ").append(toState);
        sb.append(", seq=").append(sequenceNumber);
        sb.append(", timestamp=").append(timestamp);
        if (reason != null) {
            sb.append(", reason='").append(reason).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StateTransition that = (StateTransition) obj;
        return timestamp == that.timestamp &&
               sequenceNumber == that.sequenceNumber &&
               clientId != null ? clientId.equals(that.clientId) : that.clientId == null &&
               fromState == that.fromState &&
               toState == that.toState &&
               reason != null ? reason.equals(that.reason) : that.reason == null;
    }
    
    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (fromState != null ? fromState.hashCode() : 0);
        result = 31 * result + (toState != null ? toState.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + sequenceNumber;
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }
}