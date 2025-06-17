package forge.gui.network;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages heartbeat operations for client connections, including sending periodic
 * heartbeats and detecting connection timeouts. Implements the requirements from
 * TODO.md for 30-second heartbeat intervals and 60-second connection timeouts.
 * 
 * Key features:
 * - Configurable heartbeat intervals (default 30 seconds)
 * - Connection timeout detection (default 60 seconds) 
 * - Thread-safe operations for concurrent client management
 * - Graceful shutdown and resource cleanup
 * - Performance monitoring and metrics
 */
public class HeartbeatManager {
    
    private static final Logger logger = Logger.getLogger(HeartbeatManager.class.getName());
    
    // Configuration constants matching TODO.md requirements
    public static final long HEARTBEAT_INTERVAL_MS = 30_000; // 30 seconds
    public static final long CONNECTION_TIMEOUT_MS = 60_000;  // 60 seconds
    
    // For testing, we use shorter intervals to speed up tests
    private static final long TEST_HEARTBEAT_INTERVAL_MS = 50;  // 50ms for tests
    private static final long TEST_CONNECTION_TIMEOUT_MS = 200; // 200ms for tests
    
    // Thread management
    private final ScheduledExecutorService heartbeatScheduler;
    private final ScheduledExecutorService timeoutScheduler;
    private final boolean isTestMode;
    
    // Client tracking
    private final Map<String, ScheduledFuture<?>> activeHeartbeats = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHeartbeatTimes = new ConcurrentHashMap<>();
    
    // Handler for heartbeat operations
    private final HeartbeatHandler handler;
    
    // Metrics
    private final AtomicInteger totalHeartbeatsSent = new AtomicInteger(0);
    private final AtomicInteger totalTimeouts = new AtomicInteger(0);
    
    /**
     * Creates a HeartbeatManager with production intervals.
     * 
     * @param handler The handler for heartbeat operations
     */
    public HeartbeatManager(HeartbeatHandler handler) {
        this(handler, false);
    }
    
    /**
     * Creates a HeartbeatManager with configurable test mode.
     * 
     * @param handler The handler for heartbeat operations  
     * @param testMode If true, uses shorter intervals for testing
     */
    public HeartbeatManager(HeartbeatHandler handler, boolean testMode) {
        this.handler = handler;
        this.isTestMode = testMode;
        
        // Create thread pools with proper naming
        this.heartbeatScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "HeartbeatManager-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        
        this.timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "HeartbeatManager-Timeout");
            t.setDaemon(true);
            return t;
        });
        
        // Start periodic timeout checking
        long timeoutCheckInterval = isTestMode ? 50 : 10_000; // 10 seconds in production
        timeoutScheduler.scheduleAtFixedRate(
            this::checkConnectionTimeouts,
            timeoutCheckInterval,
            timeoutCheckInterval,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("HeartbeatManager initialized in " + (testMode ? "test" : "production") + " mode");
    }
    
    /**
     * Starts sending periodic heartbeats to a client.
     * 
     * @param clientId The unique identifier for the client
     */
    public void startHeartbeat(String clientId) {
        if (clientId == null) {
            logger.warning("Attempted to start heartbeat for null client ID");
            return;
        }
        
        // Stop any existing heartbeat for this client
        stopHeartbeat(clientId);
        
        long interval = isTestMode ? TEST_HEARTBEAT_INTERVAL_MS : HEARTBEAT_INTERVAL_MS;
        
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(
            () -> sendHeartbeat(clientId),
            0, // Start immediately
            interval,
            TimeUnit.MILLISECONDS
        );
        
        activeHeartbeats.put(clientId, heartbeatTask);
        lastHeartbeatTimes.put(clientId, System.currentTimeMillis());
        
        logger.fine("Started heartbeat for client: " + clientId);
    }
    
    /**
     * Stops sending heartbeats to a client.
     * 
     * @param clientId The unique identifier for the client
     */
    public void stopHeartbeat(String clientId) {
        if (clientId == null) {
            return;
        }
        
        ScheduledFuture<?> heartbeatTask = activeHeartbeats.remove(clientId);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            logger.fine("Stopped heartbeat for client: " + clientId);
        }
        
        lastHeartbeatTimes.remove(clientId);
    }
    
    /**
     * Records a heartbeat response from a client, updating the last response time.
     * 
     * @param clientId The client that responded
     */
    public void recordHeartbeatResponse(String clientId) {
        if (clientId == null) {
            return;
        }
        
        long responseTime = System.currentTimeMillis();
        lastHeartbeatTimes.put(clientId, responseTime);
        
        logger.finest("Recorded heartbeat response from client: " + clientId);
    }
    
    /**
     * Checks all clients for connection timeouts and handles them appropriately.
     * This method is called periodically by the timeout scheduler.
     */
    public void checkConnectionTimeouts() {
        long currentTime = System.currentTimeMillis();
        long timeoutThreshold = isTestMode ? TEST_CONNECTION_TIMEOUT_MS : CONNECTION_TIMEOUT_MS;
        
        for (Map.Entry<String, Long> entry : lastHeartbeatTimes.entrySet()) {
            String clientId = entry.getKey();
            long lastResponseTime = entry.getValue();
            
            if (currentTime - lastResponseTime > timeoutThreshold) {
                handleConnectionTimeout(clientId);
            }
        }
    }
    
    /**
     * Gets the last heartbeat time for a client.
     * 
     * @param clientId The client to query
     * @return The timestamp of the last heartbeat, or 0 if not found
     */
    public long getLastHeartbeatTime(String clientId) {
        if (clientId == null) {
            return 0;
        }
        return lastHeartbeatTimes.getOrDefault(clientId, 0L);
    }
    
    /**
     * Gets the count of clients with active heartbeats.
     * 
     * @return The number of clients currently receiving heartbeats
     */
    public int getActiveHeartbeatCount() {
        return activeHeartbeats.size();
    }
    
    /**
     * Gets performance metrics for the heartbeat manager.
     * 
     * @return A HeartbeatMetrics object with current statistics
     */
    public HeartbeatMetrics getMetrics() {
        return new HeartbeatMetrics(
            activeHeartbeats.size(),
            totalHeartbeatsSent.get(),
            totalTimeouts.get(),
            isTestMode ? TEST_HEARTBEAT_INTERVAL_MS : HEARTBEAT_INTERVAL_MS,
            isTestMode ? TEST_CONNECTION_TIMEOUT_MS : CONNECTION_TIMEOUT_MS
        );
    }
    
    /**
     * Shuts down the heartbeat manager and cleans up all resources.
     * Should be called during application shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down HeartbeatManager with " + activeHeartbeats.size() + " active heartbeats");
        
        // Stop all active heartbeats
        for (String clientId : activeHeartbeats.keySet()) {
            stopHeartbeat(clientId);
        }
        
        // Shutdown thread pools
        heartbeatScheduler.shutdown();
        timeoutScheduler.shutdown();
        
        try {
            // Wait for graceful shutdown
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
            if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            heartbeatScheduler.shutdownNow();
            timeoutScheduler.shutdownNow();
        }
        
        // Clear all tracking data
        activeHeartbeats.clear();
        lastHeartbeatTimes.clear();
        
        logger.info("HeartbeatManager shutdown complete");
    }
    
    /**
     * Sends a heartbeat to the specified client and updates metrics.
     * 
     * @param clientId The client to send heartbeat to
     */
    private void sendHeartbeat(String clientId) {
        try {
            handler.sendHeartbeat(clientId);
            totalHeartbeatsSent.incrementAndGet();
            logger.finest("Sent heartbeat to client: " + clientId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send heartbeat to client: " + clientId, e);
        }
    }
    
    /**
     * Handles a connection timeout for the specified client.
     * 
     * @param clientId The client that timed out
     */
    private void handleConnectionTimeout(String clientId) {
        logger.info("Connection timeout detected for client: " + clientId);
        
        // Stop heartbeat for timed out client
        stopHeartbeat(clientId);
        
        // Update metrics
        totalTimeouts.incrementAndGet();
        
        try {
            handler.handleConnectionTimeout(clientId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling connection timeout for client: " + clientId, e);
        }
    }
    
    /**
     * Performance and status metrics for the heartbeat manager.
     */
    public static class HeartbeatMetrics {
        private final int activeClients;
        private final int totalHeartbeatsSent;
        private final int totalTimeouts;
        private final long heartbeatIntervalMs;
        private final long connectionTimeoutMs;
        
        public HeartbeatMetrics(int activeClients, int totalHeartbeatsSent, int totalTimeouts,
                              long heartbeatIntervalMs, long connectionTimeoutMs) {
            this.activeClients = activeClients;
            this.totalHeartbeatsSent = totalHeartbeatsSent;
            this.totalTimeouts = totalTimeouts;
            this.heartbeatIntervalMs = heartbeatIntervalMs;
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
        
        public int getActiveClients() { return activeClients; }
        public int getTotalHeartbeatsSent() { return totalHeartbeatsSent; }
        public int getTotalTimeouts() { return totalTimeouts; }
        public long getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
        public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
        
        @Override
        public String toString() {
            return String.format("HeartbeatMetrics{active=%d, sent=%d, timeouts=%d, interval=%dms, timeout=%dms}",
                activeClients, totalHeartbeatsSent, totalTimeouts, heartbeatIntervalMs, connectionTimeoutMs);
        }
    }
}