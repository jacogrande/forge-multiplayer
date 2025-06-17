package forge.gui.logging;

import forge.gui.network.NetworkMetrics;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Validation tests for NetworkMetrics collection accuracy.
 * Verifies that metrics are correctly collected, aggregated, and reported.
 */
public class NetworkMetricsValidationTest {
    
    private NetworkMetrics metrics;
    
    @BeforeMethod
    public void setUp() {
        metrics = NetworkMetrics.getInstance();
        metrics.reset(); // Start with clean metrics for each test
    }
    
    @AfterMethod
    public void tearDown() {
        metrics.reset(); // Clean up after each test
    }
    
    @Test
    public void testSingletonInstance() {
        NetworkMetrics instance1 = NetworkMetrics.getInstance();
        NetworkMetrics instance2 = NetworkMetrics.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    @Test
    public void testConnectionMetricsAccuracy() {
        // Record some connection attempts
        metrics.recordConnectionAttempt(true, 1000);
        metrics.recordConnectionAttempt(true, 1500);
        metrics.recordConnectionAttempt(false, 2000);
        metrics.recordConnectionAttempt(false, 500);
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        
        assertEquals(4L, summary.get("connection_attempts"));
        assertEquals(2L, summary.get("connection_successes"));
        assertEquals(2L, summary.get("connection_failures"));
        assertEquals(50.0, (Double) summary.get("connection_success_rate"), 0.1);
    }
    
    @Test
    public void testReconnectionMetricsAccuracy() {
        // Record reconnection attempts
        metrics.recordReconnectionAttempt(false, 1); // Failed first attempt
        metrics.recordReconnectionAttempt(false, 2); // Failed second attempt  
        metrics.recordReconnectionAttempt(true, 3);  // Successful third attempt
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        
        assertEquals(3L, summary.get("reconnection_attempts"));
        assertEquals(1L, summary.get("reconnection_successes"));
        assertEquals(33.33, (Double) summary.get("reconnection_success_rate"), 0.1);
    }
    
    @Test
    public void testMessageMetricsAccuracy() {
        // Record message traffic
        metrics.recordMessageSent("GAME_STATE", 1024);
        metrics.recordMessageSent("PLAYER_ACTION", 512);
        metrics.recordMessageSent("HEARTBEAT", 64); // Failed send
        
        metrics.recordMessageReceived("GAME_UPDATE", 2048, 50);
        metrics.recordMessageReceived("SERVER_RESPONSE", 256, 25);
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        
        assertEquals(3L, summary.get("messages_sent"));
        assertEquals(2L, summary.get("messages_received"));
        assertEquals(1600L, summary.get("bytes_sent")); // 1024 + 512 + 64
        assertEquals(2304L, summary.get("bytes_received")); // 2048 + 256
    }
    
    @Test
    public void testLatencyMetricsAccuracy() {
        // Record latency measurements
        metrics.recordLatency(50);
        metrics.recordLatency(75);
        metrics.recordLatency(100);
        metrics.recordLatency(25);
        
        double expectedAverage = (50 + 75 + 100 + 25) / 4.0; // 62.5
        
        assertEquals(expectedAverage, metrics.getAverageLatency(), 0.1);
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        assertEquals(4L, summary.get("latency_measurements"));
        assertEquals(expectedAverage, (Double) summary.get("average_latency_ms"), 0.1);
    }
    
    @Test
    public void testErrorMetricsAccuracy() {
        // Record various types of errors
        metrics.recordNetworkError("TIMEOUT");
        metrics.recordNetworkError("TIMEOUT");
        metrics.recordNetworkError("CONNECTION_REFUSED");
        metrics.recordNetworkError("SECURITY");
        metrics.recordNetworkError("AUTHENTICATION");
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        
        assertEquals(5L, summary.get("network_errors"));
        assertEquals(2L, summary.get("timeout_errors"));
        assertEquals(2L, summary.get("security_errors")); // SECURITY + AUTHENTICATION
    }
    
    @Test
    public void testErrorRateCalculation() {
        // Record operations and errors to test error rate calculation
        metrics.recordConnectionAttempt(true, 1000);  // 1 operation
        metrics.recordConnectionAttempt(false, 1000); // 1 operation
        metrics.recordMessageSent("TEST", 100); // 1 operation
        metrics.recordMessageReceived("TEST", 100, 50); // 1 operation
        
        metrics.recordNetworkError("TIMEOUT"); // 1 error
        
        // Total operations: 4, Total errors: 1, Error rate: 25%
        assertEquals(25.0, metrics.getErrorRate(), 0.1);
    }
    
    @Test
    public void testSessionMetricsLifecycle() {
        String sessionId = "test-session-123";
        String username = "testuser";
        
        // Start a session
        NetworkMetrics.SessionMetrics session = metrics.startSession(sessionId, username);
        
        assertNotNull(session);
        assertEquals(sessionId, session.getSessionId());
        assertEquals(username, session.getUsername());
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        assertEquals(1L, summary.get("active_sessions"));
        
        // Record some activity in the session
        session.recordMessage(1024);
        session.recordMessage(512);
        session.recordError();
        
        assertEquals(2L, session.getMessagesInSession());
        assertEquals(1536L, session.getBytesInSession());
        assertEquals(1L, session.getErrorsInSession());
        
        // End the session
        metrics.endSession(sessionId);
        
        summary = metrics.getMetricsSummary();
        assertEquals(0L, summary.get("active_sessions"));
    }
    
    @Test
    public void testSessionDurationAccuracy() throws InterruptedException {
        String sessionId = "duration-test-session";
        
        NetworkMetrics.SessionMetrics session = metrics.startSession(sessionId, "testuser");
        
        // Wait a short time to get a measurable duration
        Thread.sleep(100);
        
        long duration = session.getDurationMs();
        assertTrue(duration >= 100, "Session duration should be at least 100ms");
        assertTrue(duration < 200, "Session duration should be less than 200ms for this test");
    }
    
    @Test
    public void testThroughputCalculations() throws InterruptedException {
        // Record some activity and wait to get throughput measurements
        metrics.recordMessageSent("TEST", 1000);
        metrics.recordMessageReceived("TEST", 1000, 50);
        
        // Wait a moment for time-based calculations
        Thread.sleep(100);
        
        Map<String, Object> throughput = metrics.getThroughputStats();
        
        assertNotNull(throughput.get("messages_sent_per_second"));
        assertNotNull(throughput.get("messages_received_per_second"));
        assertNotNull(throughput.get("bytes_sent_per_second"));
        assertNotNull(throughput.get("bytes_received_per_second"));
        assertNotNull(throughput.get("uptime_seconds"));
        
        // Verify positive values
        assertTrue((Double) throughput.get("messages_sent_per_second") > 0);
        assertTrue((Double) throughput.get("messages_received_per_second") > 0);
        assertTrue((Double) throughput.get("uptime_seconds") > 0);
    }
    
    @Test
    public void testConcurrentMetricsCollection() throws InterruptedException {
        int numThreads = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        metrics.recordConnectionAttempt(true, 1000);
                        metrics.recordMessageSent("CONCURRENT_TEST", 512);
                        metrics.recordLatency(50);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent operations should complete within 10 seconds");
        
        Map<String, Object> summary = metrics.getMetricsSummary();
        
        // Verify all operations were recorded correctly
        assertEquals((long) numThreads * operationsPerThread, summary.get("connection_attempts"));
        assertEquals((long) numThreads * operationsPerThread, summary.get("connection_successes"));
        assertEquals((long) numThreads * operationsPerThread, summary.get("messages_sent"));
        assertEquals((long) numThreads * operationsPerThread, summary.get("latency_measurements"));
        
        executor.shutdown();
    }
    
    @Test
    public void testMetricsReset() {
        // Record some metrics
        metrics.recordConnectionAttempt(true, 1000);
        metrics.recordMessageSent("TEST", 512);
        metrics.recordLatency(50);
        metrics.recordNetworkError("TIMEOUT");
        
        // Verify metrics are recorded
        Map<String, Object> summary = metrics.getMetricsSummary();
        assertEquals(1L, summary.get("connection_attempts"));
        assertEquals(1L, summary.get("messages_sent"));
        assertEquals(1L, summary.get("network_errors"));
        
        // Reset metrics
        metrics.reset();
        
        // Verify all metrics are zero
        summary = metrics.getMetricsSummary();
        assertEquals(0L, summary.get("connection_attempts"));
        assertEquals(0L, summary.get("connection_successes"));
        assertEquals(0L, summary.get("connection_failures"));
        assertEquals(0L, summary.get("messages_sent"));
        assertEquals(0L, summary.get("messages_received"));
        assertEquals(0L, summary.get("network_errors"));
        assertEquals(0L, summary.get("latency_measurements"));
        assertEquals(0.0, (Double) summary.get("average_latency_ms"), 0.1);
        assertEquals(0L, summary.get("active_sessions"));
    }
    
    @Test
    public void testZeroDivisionSafety() {
        // Test that success rates are calculated safely with zero attempts
        assertEquals(0.0, metrics.getConnectionSuccessRate(), 0.1);
        assertEquals(0.0, metrics.getReconnectionSuccessRate(), 0.1);
        assertEquals(0.0, metrics.getErrorRate(), 0.1);
        assertEquals(0.0, metrics.getAverageLatency(), 0.1);
    }
    
    @Test
    public void testMetricsSummaryCompleteness() {
        Map<String, Object> summary = metrics.getMetricsSummary();
        
        // Verify all expected metrics are present
        String[] expectedKeys = {
            "connection_attempts", "connection_successes", "connection_failures", "connection_success_rate",
            "reconnection_attempts", "reconnection_successes", "reconnection_success_rate",
            "messages_sent", "messages_received", "bytes_sent", "bytes_received",
            "network_errors", "timeout_errors", "security_errors", "error_rate",
            "average_latency_ms", "latency_measurements", "active_sessions",
            "messages_sent_per_second", "messages_received_per_second", "bytes_sent_per_second",
            "bytes_received_per_second", "uptime_seconds"
        };
        
        for (String key : expectedKeys) {
            assertTrue(summary.containsKey(key), "Metrics summary should contain key: " + key);
            assertNotNull(summary.get(key), "Metrics value should not be null for key: " + key);
        }
    }
}