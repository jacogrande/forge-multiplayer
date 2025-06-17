package forge.gui.network;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

/**
 * Collects and provides network performance metrics for monitoring and debugging.
 * Integrates with NetworkEventLogger to provide structured metrics logging.
 * 
 * Features:
 * - Connection statistics (attempts, successes, failures)
 * - Message throughput tracking
 * - Latency measurements
 * - Error rate monitoring
 * - Session duration tracking
 * 
 * Thread-safe implementation suitable for concurrent network operations.
 */
public class NetworkMetrics {
    
    private static final NetworkMetrics instance = new NetworkMetrics();
    private static final NetworkEventLogger metricsLogger = NetworkEventLogger.forComponent("NetworkMetrics");
    
    // Connection metrics
    private final AtomicLong connectionAttempts = new AtomicLong(0);
    private final AtomicLong connectionSuccesses = new AtomicLong(0);
    private final AtomicLong connectionFailures = new AtomicLong(0);
    private final AtomicLong reconnectionAttempts = new AtomicLong(0);
    private final AtomicLong reconnectionSuccesses = new AtomicLong(0);
    
    // Message metrics
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    
    // Error metrics
    private final AtomicLong networkErrors = new AtomicLong(0);
    private final AtomicLong timeoutErrors = new AtomicLong(0);
    private final AtomicLong securityErrors = new AtomicLong(0);
    
    // Latency tracking
    private final AtomicReference<Double> averageLatency = new AtomicReference<>(0.0);
    private final AtomicLong latencyMeasurements = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    
    // Session tracking
    private final Map<String, SessionMetrics> activeSessions = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> startTime = new AtomicReference<>(Instant.now());
    
    private NetworkMetrics() {}
    
    /**
     * Gets the singleton instance of NetworkMetrics.
     * 
     * @return The NetworkMetrics instance
     */
    public static NetworkMetrics getInstance() {
        return instance;
    }
    
    /**
     * Records a connection attempt.
     * 
     * @param success Whether the connection succeeded
     * @param durationMs The connection duration in milliseconds
     */
    public void recordConnectionAttempt(boolean success, long durationMs) {
        connectionAttempts.incrementAndGet();
        
        if (success) {
            connectionSuccesses.incrementAndGet();
            metricsLogger.logPerformanceMetric("connection.duration", durationMs, "ms");
        } else {
            connectionFailures.incrementAndGet();
        }
        
        // Log connection success rate
        double successRate = getConnectionSuccessRate();
        metricsLogger.logPerformanceMetric("connection.success_rate", successRate, "percent");
    }
    
    /**
     * Records a reconnection attempt.
     * 
     * @param success Whether the reconnection succeeded
     * @param attemptNumber The attempt number in the sequence
     */
    public void recordReconnectionAttempt(boolean success, int attemptNumber) {
        reconnectionAttempts.incrementAndGet();
        
        if (success) {
            reconnectionSuccesses.incrementAndGet();
            metricsLogger.logPerformanceMetric("reconnection.attempts_to_success", attemptNumber, "count");
        }
        
        double reconnectionSuccessRate = getReconnectionSuccessRate();
        metricsLogger.logPerformanceMetric("reconnection.success_rate", reconnectionSuccessRate, "percent");
    }
    
    /**
     * Records a message being sent.
     * 
     * @param messageType The type of message
     * @param sizeBytes The message size in bytes
     */
    public void recordMessageSent(String messageType, int sizeBytes) {
        messagesSent.incrementAndGet();
        bytesSent.addAndGet(sizeBytes);
        
        // Log throughput metrics periodically
        if (messagesSent.get() % 100 == 0) {
            metricsLogger.logPerformanceMetric("messages.sent_total", messagesSent.get(), "count");
            metricsLogger.logPerformanceMetric("bandwidth.sent_total", bytesSent.get(), "bytes");
        }
    }
    
    /**
     * Records a message being received.
     * 
     * @param messageType The type of message
     * @param sizeBytes The message size in bytes
     * @param processingTimeMs Time taken to process the message
     */
    public void recordMessageReceived(String messageType, int sizeBytes, long processingTimeMs) {
        messagesReceived.incrementAndGet();
        bytesReceived.addAndGet(sizeBytes);
        
        // Track message processing performance
        metricsLogger.logPerformanceMetric("message.processing_time", processingTimeMs, "ms");
        
        // Log throughput metrics periodically
        if (messagesReceived.get() % 100 == 0) {
            metricsLogger.logPerformanceMetric("messages.received_total", messagesReceived.get(), "count");
            metricsLogger.logPerformanceMetric("bandwidth.received_total", bytesReceived.get(), "bytes");
        }
    }
    
    /**
     * Records network latency measurement.
     * 
     * @param latencyMs The measured latency in milliseconds
     */
    public void recordLatency(long latencyMs) {
        latencyMeasurements.incrementAndGet();
        totalLatency.addAndGet(latencyMs);
        
        // Update running average
        double newAverage = (double) totalLatency.get() / latencyMeasurements.get();
        averageLatency.set(newAverage);
        
        metricsLogger.logPerformanceMetric("network.latency", latencyMs, "ms");
        metricsLogger.logPerformanceMetric("network.latency_average", newAverage, "ms");
    }
    
    /**
     * Records a network error.
     * 
     * @param errorType The type of error (e.g., "TIMEOUT", "CONNECTION_REFUSED")
     */
    public void recordNetworkError(String errorType) {
        networkErrors.incrementAndGet();
        
        switch (errorType.toUpperCase()) {
            case "TIMEOUT":
                timeoutErrors.incrementAndGet();
                break;
            case "SECURITY":
            case "AUTHENTICATION":
            case "AUTHORIZATION":
                securityErrors.incrementAndGet();
                break;
        }
        
        double errorRate = getErrorRate();
        metricsLogger.logPerformanceMetric("network.error_rate", errorRate, "percent");
    }
    
    /**
     * Starts tracking metrics for a new session.
     * 
     * @param sessionId The session identifier
     * @param username The username (optional)
     * @return A SessionMetrics object for tracking session-specific metrics
     */
    public SessionMetrics startSession(String sessionId, String username) {
        SessionMetrics session = new SessionMetrics(sessionId, username);
        activeSessions.put(sessionId, session);
        
        metricsLogger.logPerformanceMetric("sessions.active_count", activeSessions.size(), "count");
        return session;
    }
    
    /**
     * Ends tracking for a session.
     * 
     * @param sessionId The session identifier
     */
    public void endSession(String sessionId) {
        SessionMetrics session = activeSessions.remove(sessionId);
        if (session != null) {
            long sessionDuration = session.getDurationMs();
            metricsLogger.logPerformanceMetric("session.duration", sessionDuration, "ms");
            metricsLogger.logPerformanceMetric("sessions.active_count", activeSessions.size(), "count");
        }
    }
    
    /**
     * Gets the current connection success rate.
     * 
     * @return Success rate as a percentage (0-100)
     */
    public double getConnectionSuccessRate() {
        long attempts = connectionAttempts.get();
        if (attempts == 0) return 0.0;
        return (double) connectionSuccesses.get() / attempts * 100.0;
    }
    
    /**
     * Gets the current reconnection success rate.
     * 
     * @return Success rate as a percentage (0-100)
     */
    public double getReconnectionSuccessRate() {
        long attempts = reconnectionAttempts.get();
        if (attempts == 0) return 0.0;
        return (double) reconnectionSuccesses.get() / attempts * 100.0;
    }
    
    /**
     * Gets the current error rate.
     * 
     * @return Error rate as a percentage of total operations
     */
    public double getErrorRate() {
        long totalOperations = connectionAttempts.get() + messagesSent.get() + messagesReceived.get();
        if (totalOperations == 0) return 0.0;
        return (double) networkErrors.get() / totalOperations * 100.0;
    }
    
    /**
     * Gets the current average latency.
     * 
     * @return Average latency in milliseconds
     */
    public double getAverageLatency() {
        return averageLatency.get();
    }
    
    /**
     * Gets current throughput statistics.
     * 
     * @return A map containing throughput metrics
     */
    public Map<String, Object> getThroughputStats() {
        long uptimeMs = Instant.now().toEpochMilli() - startTime.get().toEpochMilli();
        double uptimeSeconds = uptimeMs / 1000.0;
        
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("messages_sent_per_second", uptimeSeconds > 0 ? messagesSent.get() / uptimeSeconds : 0);
        stats.put("messages_received_per_second", uptimeSeconds > 0 ? messagesReceived.get() / uptimeSeconds : 0);
        stats.put("bytes_sent_per_second", uptimeSeconds > 0 ? bytesSent.get() / uptimeSeconds : 0);
        stats.put("bytes_received_per_second", uptimeSeconds > 0 ? bytesReceived.get() / uptimeSeconds : 0);
        stats.put("uptime_seconds", uptimeSeconds);
        
        return stats;
    }
    
    /**
     * Gets a comprehensive metrics summary.
     * 
     * @return A map containing all current metrics
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        
        // Connection metrics
        summary.put("connection_attempts", connectionAttempts.get());
        summary.put("connection_successes", connectionSuccesses.get());
        summary.put("connection_failures", connectionFailures.get());
        summary.put("connection_success_rate", getConnectionSuccessRate());
        
        // Reconnection metrics
        summary.put("reconnection_attempts", reconnectionAttempts.get());
        summary.put("reconnection_successes", reconnectionSuccesses.get());
        summary.put("reconnection_success_rate", getReconnectionSuccessRate());
        
        // Message metrics
        summary.put("messages_sent", messagesSent.get());
        summary.put("messages_received", messagesReceived.get());
        summary.put("bytes_sent", bytesSent.get());
        summary.put("bytes_received", bytesReceived.get());
        
        // Error metrics
        summary.put("network_errors", networkErrors.get());
        summary.put("timeout_errors", timeoutErrors.get());
        summary.put("security_errors", securityErrors.get());
        summary.put("error_rate", getErrorRate());
        
        // Performance metrics
        summary.put("average_latency_ms", getAverageLatency());
        summary.put("latency_measurements", latencyMeasurements.get());
        
        // Session metrics
        summary.put("active_sessions", activeSessions.size());
        
        // Throughput metrics
        summary.putAll(getThroughputStats());
        
        return summary;
    }
    
    /**
     * Resets all metrics to zero. Useful for testing or periodic resets.
     */
    public void reset() {
        connectionAttempts.set(0);
        connectionSuccesses.set(0);
        connectionFailures.set(0);
        reconnectionAttempts.set(0);
        reconnectionSuccesses.set(0);
        
        messagesSent.set(0);
        messagesReceived.set(0);
        bytesSent.set(0);
        bytesReceived.set(0);
        
        networkErrors.set(0);
        timeoutErrors.set(0);
        securityErrors.set(0);
        
        averageLatency.set(0.0);
        latencyMeasurements.set(0);
        totalLatency.set(0);
        
        activeSessions.clear();
        startTime.set(Instant.now());
        
        metricsLogger.logEvent(NetworkEventLogger.EventType.PERFORMANCE, 
                              NetworkEventLogger.Severity.INFO, 
                              "Network metrics reset").log();
    }
    
    /**
     * Represents metrics for an individual session.
     */
    public static class SessionMetrics {
        private final String sessionId;
        private final String username;
        private final Instant startTime;
        private final AtomicLong messagesInSession = new AtomicLong(0);
        private final AtomicLong bytesInSession = new AtomicLong(0);
        private final AtomicLong errorsInSession = new AtomicLong(0);
        
        SessionMetrics(String sessionId, String username) {
            this.sessionId = sessionId;
            this.username = username;
            this.startTime = Instant.now();
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public long getDurationMs() {
            return Instant.now().toEpochMilli() - startTime.toEpochMilli();
        }
        
        public void recordMessage(int bytes) {
            messagesInSession.incrementAndGet();
            bytesInSession.addAndGet(bytes);
        }
        
        public void recordError() {
            errorsInSession.incrementAndGet();
        }
        
        public long getMessagesInSession() {
            return messagesInSession.get();
        }
        
        public long getBytesInSession() {
            return bytesInSession.get();
        }
        
        public long getErrorsInSession() {
            return errorsInSession.get();
        }
    }
}