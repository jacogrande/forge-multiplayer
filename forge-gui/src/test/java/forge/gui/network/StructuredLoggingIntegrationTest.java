package forge.gui.network;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

import forge.util.logging.LoggingBridge;

import static org.testng.Assert.*;

/**
 * Integration test for structured logging across network components.
 * Demonstrates logging bridge functionality and end-to-end structured logging.
 */
public class StructuredLoggingIntegrationTest {
    
    @BeforeClass
    public void setUpLogging() {
        // Initialize the JUL to SLF4J bridge
        LoggingBridge.initialize();
        assertTrue(LoggingBridge.isInitialized(), "Logging bridge should be initialized");
    }
    
    @AfterClass
    public void cleanUpLogging() {
        LoggingBridge.cleanup();
    }
    
    @Test
    public void testNetworkEventLoggerIntegration() {
        NetworkEventLogger logger = NetworkEventLogger.forComponent("IntegrationTest");
        NetworkMetrics metrics = NetworkMetrics.getInstance();
        
        // Start a session
        String sessionId = "integration-test-session";
        String username = "testuser";
        NetworkMetrics.SessionMetrics sessionMetrics = metrics.startSession(sessionId, username);
        
        logger.setSessionContext(sessionId, username, "test-game-id");
        String correlationId = logger.startCorrelation();
        
        try {
            // Simulate a connection sequence
            logger.logConnection("localhost", 7777, true, 1200);
            metrics.recordConnectionAttempt(true, 1200);
            
            // Simulate message exchange
            logger.logMessageSent("LoginEvent", 128, true);
            metrics.recordMessageSent("LoginEvent", 128);
            sessionMetrics.recordMessage(128);
            
            logger.logMessageReceived("WelcomeEvent", 64, 25);
            metrics.recordMessageReceived("WelcomeEvent", 64, 25);
            sessionMetrics.recordMessage(64);
            
            // Simulate latency measurement
            metrics.recordLatency(45);
            logger.logHeartbeat(false, 45L);
            
            // Simulate game state sync
            logger.logGameStateSync("CAPTURE", true, 500, 2048);
            
            // Simulate a security event
            logger.logSecurityEvent("PLAYER_AUTHENTICATED", "Player successfully authenticated", 
                                   NetworkEventLogger.Severity.INFO);
            
            // Log performance metrics
            logger.logPerformanceMetric("connection.throughput", 1024.5, "bytes/sec");
            
            // Verify metrics were recorded
            assertTrue(metrics.getConnectionSuccessRate() > 0, "Connection success rate should be > 0");
            assertTrue(metrics.getAverageLatency() > 0, "Average latency should be > 0");
            assertTrue(sessionMetrics.getMessagesInSession() > 0, "Session should have messages");
            assertTrue(sessionMetrics.getBytesInSession() > 0, "Session should have bytes transferred");
            
        } finally {
            logger.endCorrelation(correlationId);
            logger.clearSessionContext();
            metrics.endSession(sessionId);
        }
    }
    
    @Test
    public void testErrorScenarios() {
        NetworkEventLogger logger = NetworkEventLogger.forComponent("ErrorTest");
        NetworkMetrics metrics = NetworkMetrics.getInstance();
        
        // Simulate connection failure
        logger.logConnection("unreachable-host", 9999, false, 5000);
        metrics.recordConnectionAttempt(false, 5000);
        metrics.recordNetworkError("CONNECTION_TIMEOUT");
        
        // Simulate message send failure
        logger.logMessageSent("GameEvent", 256, false);
        logger.logError("MESSAGE_SEND_FAILED", new RuntimeException("Network unreachable"));
        metrics.recordNetworkError("MESSAGE_SEND_FAILED");
        
        // Simulate security error
        logger.logSecurityEvent("AUTHORIZATION_FAILED", "Player not authorized for this game", 
                               NetworkEventLogger.Severity.WARN);
        metrics.recordNetworkError("SECURITY");
        
        // Verify error metrics
        assertTrue(metrics.getErrorRate() > 0, "Error rate should be > 0 after errors");
        assertTrue(NetworkEventLogger.getTotalErrors() > 0, "Total errors should be > 0");
    }
    
    @Test
    public void testReconnectionScenario() {
        NetworkEventLogger logger = NetworkEventLogger.forComponent("ReconnectionTest");
        NetworkMetrics metrics = NetworkMetrics.getInstance();
        
        String correlationId = logger.startCorrelation();
        
        try {
            // Simulate disconnection
            logger.logDisconnection("Network timeout", true);
            
            // Simulate reconnection attempts
            for (int attempt = 1; attempt <= 3; attempt++) {
                boolean success = (attempt == 3); // Success on third attempt
                long delay = attempt * 1000;
                
                logger.logReconnectionAttempt(attempt, 5, delay, success);
                metrics.recordReconnectionAttempt(success, attempt);
                
                if (success) {
                    logger.logConnection("localhost", 7777, true, 2000);
                    metrics.recordConnectionAttempt(true, 2000);
                    
                    // Simulate state recovery
                    logger.logGameStateSync("RESTORE", true, 750, 2048);
                    break;
                }
            }
            
            // Verify reconnection metrics
            assertTrue(metrics.getReconnectionSuccessRate() > 0, "Reconnection success rate should be > 0");
            
        } finally {
            logger.endCorrelation(correlationId);
        }
    }
    
    @Test
    public void testPerformanceMetricsCollection() {
        NetworkEventLogger logger = NetworkEventLogger.forComponent("PerformanceTest");
        NetworkMetrics metrics = NetworkMetrics.getInstance();
        
        // Simulate various performance metrics
        logger.logPerformanceMetric("network.bandwidth", 1500.0, "kbps");
        logger.logPerformanceMetric("game.fps", 60.0, "fps");
        logger.logPerformanceMetric("memory.usage", 85.5, "percent");
        
        // Record latency measurements
        for (int i = 0; i < 10; i++) {
            long latency = 50 + (i * 5); // 50-95ms range
            metrics.recordLatency(latency);
            logger.logHeartbeat(false, latency);
        }
        
        // Verify performance tracking
        assertTrue(metrics.getAverageLatency() > 0, "Average latency should be calculated");
        
        // Get comprehensive metrics summary
        var summary = metrics.getMetricsSummary();
        assertNotNull(summary, "Metrics summary should be available");
        assertTrue(summary.containsKey("average_latency_ms"), "Summary should contain latency");
        assertTrue(summary.containsKey("connection_attempts"), "Summary should contain connection attempts");
    }
    
    @Test
    public void testConcurrentLogging() throws InterruptedException {
        NetworkEventLogger logger = NetworkEventLogger.forComponent("ConcurrencyTest");
        NetworkMetrics metrics = NetworkMetrics.getInstance();
        
        int threadCount = 5;
        int operationsPerThread = 10;
        Thread[] threads = new Thread[threadCount];
        
        // Create multiple threads that log concurrently
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    String correlationId = logger.startCorrelation();
                    try {
                        logger.logConnection("host-" + threadId, 7777 + threadId, true, 100 + i);
                        metrics.recordConnectionAttempt(true, 100 + i);
                        
                        logger.logMessageSent("Event-" + threadId + "-" + i, 64, true);
                        metrics.recordMessageSent("Event-" + threadId + "-" + i, 64);
                        
                    } finally {
                        logger.endCorrelation(correlationId);
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }
        
        // Verify that concurrent operations completed without issues
        assertTrue(NetworkEventLogger.getTotalEvents() >= threadCount * operationsPerThread, 
                  "All concurrent operations should be logged");
    }
    
    @Test
    public void testLoggingBridgePerformance() {
        // Test that JUL-to-SLF4J bridge is working
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("TestJULLogger");
        
        long startTime = System.currentTimeMillis();
        
        // Log through JUL (should be bridged to SLF4J)
        for (int i = 0; i < 100; i++) {
            julLogger.info("JUL log message " + i);
        }
        
        long julTime = System.currentTimeMillis() - startTime;
        
        // Log through SLF4J directly
        org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger("TestSLF4JLogger");
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            slf4jLogger.info("SLF4J log message {}", i);
        }
        
        long slf4jTime = System.currentTimeMillis() - startTime;
        
        // JUL should take longer due to bridge overhead, but not excessively
        assertTrue(julTime >= slf4jTime, "JUL logging should have some overhead");
        
        // Performance info should be available
        String perfInfo = LoggingBridge.getPerformanceInfo();
        assertNotNull(perfInfo, "Performance info should be available");
        assertTrue(perfInfo.contains("overhead"), "Performance info should mention overhead");
    }
}