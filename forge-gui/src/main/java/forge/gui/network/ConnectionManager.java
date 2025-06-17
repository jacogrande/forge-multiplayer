package forge.gui.network;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Thread-safe manager for tracking client connection states and notifying observers
 * of state changes. Provides the foundation for robust connection lifecycle management
 * in the networking layer.
 * 
 * Key features:
 * - Thread-safe operations for concurrent client access
 * - State transition validation using ConnectionState rules
 * - Observer pattern for real-time state change notifications
 * - Graceful handling of invalid operations
 */
public class ConnectionManager {
    
    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());
    
    // Thread-safe collections for concurrent access
    private final Map<String, ConnectionState> clientStates = new ConcurrentHashMap<>();
    private final List<ConnectionStateObserver> observers = new CopyOnWriteArrayList<>();
    
    // Performance metrics tracking
    private volatile long lastStateChangeTime = 0;
    private volatile int totalStateChanges = 0;
    
    /**
     * Registers a new client with the connection manager.
     * Clients start in DISCONNECTED state.
     * 
     * @param clientId The unique identifier for the client
     */
    public void registerClient(String clientId) {
        if (clientId == null) {
            logger.warning("Attempted to register null client ID");
            return;
        }
        
        ConnectionState previousState = clientStates.put(clientId, ConnectionState.DISCONNECTED);
        
        if (previousState == null) {
            logger.info("Registered new client: " + clientId);
            notifyObservers(clientId, null, ConnectionState.DISCONNECTED);
        } else {
            logger.warning("Client " + clientId + " was already registered with state: " + previousState);
        }
    }
    
    /**
     * Unregisters a client from the connection manager.
     * Removes all state tracking for the client.
     * 
     * @param clientId The unique identifier for the client to remove
     */
    public void unregisterClient(String clientId) {
        if (clientId == null) {
            logger.warning("Attempted to unregister null client ID");
            return;
        }
        
        ConnectionState removedState = clientStates.remove(clientId);
        
        if (removedState != null) {
            logger.info("Unregistered client: " + clientId + " (was in state: " + removedState + ")");
            notifyObservers(clientId, removedState, null);
        } else {
            logger.warning("Attempted to unregister unknown client: " + clientId);
        }
    }
    
    /**
     * Updates the connection state for a client if the transition is valid.
     * Invalid transitions are rejected and logged.
     * 
     * @param clientId The client whose state should be updated
     * @param newState The desired new state
     * @return true if the state was updated, false if the transition was invalid
     */
    public boolean updateConnectionState(String clientId, ConnectionState newState) {
        if (clientId == null || newState == null) {
            logger.warning("Attempted to update state with null client ID or state");
            return false;
        }
        
        return clientStates.compute(clientId, (id, currentState) -> {
            if (currentState == null) {
                logger.warning("Attempted to update state for unregistered client: " + clientId);
                return null; // Keep the client unregistered
            }
            
            if (currentState.canTransitionTo(newState)) {
                logger.fine("State transition for " + clientId + ": " + currentState + " -> " + newState);
                
                // Update metrics
                lastStateChangeTime = System.currentTimeMillis();
                totalStateChanges++;
                
                // Notify observers
                notifyObservers(clientId, currentState, newState);
                
                return newState;
            } else {
                logger.warning("Invalid state transition rejected for " + clientId + 
                             ": " + currentState + " -> " + newState);
                return currentState; // Keep current state
            }
        }) != null;
    }
    
    /**
     * Gets the current connection state for a client.
     * 
     * @param clientId The client ID to query
     * @return The current connection state, or null if client is not registered
     */
    public ConnectionState getConnectionState(String clientId) {
        if (clientId == null) {
            return null;
        }
        return clientStates.get(clientId);
    }
    
    /**
     * Checks if a client is currently registered.
     * 
     * @param clientId The client ID to check
     * @return true if the client is registered, false otherwise
     */
    public boolean isClientRegistered(String clientId) {
        if (clientId == null) {
            return false;
        }
        return clientStates.containsKey(clientId);
    }
    
    /**
     * Gets a list of all currently registered client IDs.
     * 
     * @return A new list containing all registered client IDs
     */
    public List<String> getAllClients() {
        return new ArrayList<>(clientStates.keySet());
    }
    
    /**
     * Gets the count of clients in a specific state.
     * 
     * @param state The state to count
     * @return The number of clients currently in the specified state
     */
    public int getClientCountInState(ConnectionState state) {
        if (state == null) {
            return 0;
        }
        
        return (int) clientStates.values().stream()
                .filter(clientState -> clientState == state)
                .count();
    }
    
    /**
     * Adds an observer to receive connection state change notifications.
     * 
     * @param observer The observer to add
     */
    public void addObserver(ConnectionStateObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            logger.fine("Added connection state observer: " + observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Removes an observer from receiving notifications.
     * 
     * @param observer The observer to remove
     */
    public void removeObserver(ConnectionStateObserver observer) {
        if (observer != null && observers.remove(observer)) {
            logger.fine("Removed connection state observer: " + observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Gets performance metrics for the connection manager.
     * 
     * @return A ConnectionMetrics object with current statistics
     */
    public ConnectionMetrics getMetrics() {
        return new ConnectionMetrics(
            clientStates.size(),
            totalStateChanges,
            lastStateChangeTime,
            getClientCountInState(ConnectionState.CONNECTED),
            getClientCountInState(ConnectionState.CONNECTING),
            getClientCountInState(ConnectionState.DISCONNECTED),
            getClientCountInState(ConnectionState.RECONNECTING)
        );
    }
    
    /**
     * Shuts down the connection manager and cleans up resources.
     * Should be called during application shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down ConnectionManager with " + clientStates.size() + " clients");
        
        // Notify observers of shutdown
        for (String clientId : new ArrayList<>(clientStates.keySet())) {
            unregisterClient(clientId);
        }
        
        // Clear all data
        clientStates.clear();
        observers.clear();
        
        logger.info("ConnectionManager shutdown complete");
    }
    
    /**
     * Notifies all observers of a connection state change.
     * 
     * @param clientId The client whose state changed
     * @param oldState The previous state (null for new registrations)
     * @param newState The new state (null for unregistrations)
     */
    private void notifyObservers(String clientId, ConnectionState oldState, ConnectionState newState) {
        for (ConnectionStateObserver observer : observers) {
            try {
                observer.onConnectionStateChanged(clientId, oldState, newState);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Observer failed to handle state change notification", e);
            }
        }
    }
    
    /**
     * Performance and state metrics for the connection manager.
     */
    public static class ConnectionMetrics {
        private final int totalClients;
        private final int totalStateChanges;
        private final long lastStateChangeTime;
        private final int connectedClients;
        private final int connectingClients;
        private final int disconnectedClients;
        private final int reconnectingClients;
        
        public ConnectionMetrics(int totalClients, int totalStateChanges, long lastStateChangeTime,
                               int connectedClients, int connectingClients, int disconnectedClients, int reconnectingClients) {
            this.totalClients = totalClients;
            this.totalStateChanges = totalStateChanges;
            this.lastStateChangeTime = lastStateChangeTime;
            this.connectedClients = connectedClients;
            this.connectingClients = connectingClients;
            this.disconnectedClients = disconnectedClients;
            this.reconnectingClients = reconnectingClients;
        }
        
        public int getTotalClients() { return totalClients; }
        public int getTotalStateChanges() { return totalStateChanges; }
        public long getLastStateChangeTime() { return lastStateChangeTime; }
        public int getConnectedClients() { return connectedClients; }
        public int getConnectingClients() { return connectingClients; }
        public int getDisconnectedClients() { return disconnectedClients; }
        public int getReconnectingClients() { return reconnectingClients; }
        
        @Override
        public String toString() {
            return String.format("ConnectionMetrics{total=%d, connected=%d, connecting=%d, disconnected=%d, reconnecting=%d, stateChanges=%d}",
                totalClients, connectedClients, connectingClients, disconnectedClients, reconnectingClients, totalStateChanges);
        }
    }
}