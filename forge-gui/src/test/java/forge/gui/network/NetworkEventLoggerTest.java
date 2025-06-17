package forge.gui.network;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.slf4j.MDC;

import static org.testng.Assert.*;

/**
 * Test class for NetworkEventLogger functionality.
 * Validates structured logging behavior and integration.
 */
public class NetworkEventLoggerTest {
    
    private NetworkEventLogger logger;
    private NetworkMetrics metrics;
    
    @BeforeMethod
    public void setUp() {
        logger = NetworkEventLogger.forComponent("TestComponent");
        metrics = NetworkMetrics.getInstance();
        metrics.reset(); // Start with clean metrics
    }
    
    @AfterMethod
    public void tearDown() {
        // Clean up MDC
        MDC.clear();
    }
    
    @Test
    public void testLoggerCreation() {
        assertNotNull(logger, "Logger should be created successfully");
        
        NetworkEventLogger logger2 = NetworkEventLogger.forComponent("TestComponent");
        assertSame(logger, logger2, "Same component should return same logger instance");
        
        NetworkEventLogger differentLogger = NetworkEventLogger.forComponent("DifferentComponent");
        assertNotSame(logger, differentLogger, "Different components should have different loggers");
    }
    
    @Test
    public void testCorrelationIdManagement() {
        // Test correlation ID generation
        String correlationId = logger.startCorrelation();
        assertNotNull(correlationId, "Correlation ID should be generated");
        assertEquals(8, correlationId.length(), "Correlation ID should be 8 characters");
        assertEquals(correlationId, MDC.get("correlationId"), "MDC should contain correlation ID");
        
        // Test ending correlation
        logger.endCorrelation(correlationId);
        assertNull(MDC.get("correlationId"), "MDC should be cleared after ending correlation");
    }
    
    @Test
    public void testSessionContextManagement() {
        String sessionId = "test-session-123";
        String username = "testuser";
        String gameId = "game-456";
        
        logger.setSessionContext(sessionId, username, gameId);
        
        assertEquals(sessionId, MDC.get("sessionId"), "Session ID should be set in MDC");
        assertEquals(username, MDC.get("username"), "Username should be set in MDC");
        assertEquals(gameId, MDC.get("gameId"), "Game ID should be set in MDC");
        
        logger.clearSessionContext();
        
        assertNull(MDC.get("sessionId"), "Session ID should be cleared");
        assertNull(MDC.get("username"), "Username should be cleared");
        assertNull(MDC.get("gameId"), "Game ID should be cleared");
    }
    
    @Test
    public void testConnectionLogging() {
        String host = "localhost";
        int port = 7777;
        long duration = 1500;
        
        // Test successful connection
        logger.logConnection(host, port, true, duration);
        // No exception should be thrown
        
        // Test failed connection
        logger.logConnection(host, port, false, duration);
        // No exception should be thrown
    }
    
    @Test
    public void testDisconnectionLogging() {
        String reason = "Network timeout";
        
        // Test expected disconnection
        logger.logDisconnection(reason, false);
        
        // Test unexpected disconnection
        logger.logDisconnection(reason, true);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testReconnectionLogging() {
        int attempt = 2;
        int maxAttempts = 5;
        long delay = 2000;
        
        // Test successful reconnection attempt
        logger.logReconnectionAttempt(attempt, maxAttempts, delay, true);
        
        // Test failed reconnection attempt
        logger.logReconnectionAttempt(attempt, maxAttempts, delay, false);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testMessageLogging() {
        String messageType = "LoginEvent";
        int messageSize = 128;
        long processingTime = 50;
        
        // Test message sent
        logger.logMessageSent(messageType, messageSize, true);
        logger.logMessageSent(messageType, messageSize, false);
        
        // Test message received
        logger.logMessageReceived(messageType, messageSize, processingTime);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testGameStateSyncLogging() {
        String operation = "CAPTURE";
        long duration = 500;
        long stateSize = 1024;
        
        // Test successful sync
        logger.logGameStateSync(operation, true, duration, stateSize);
        
        // Test failed sync
        logger.logGameStateSync(operation, false, duration, stateSize);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testSecurityEventLogging() {
        String securityEvent = "AUTHENTICATION_FAILED";
        String details = "Invalid credentials provided";
        
        logger.logSecurityEvent(securityEvent, details, NetworkEventLogger.Severity.WARN);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testPerformanceMetricLogging() {
        String metric = "connection.latency";
        double value = 125.5;
        String unit = "ms";
        
        logger.logPerformanceMetric(metric, value, unit);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testHeartbeatLogging() {
        // Test sent heartbeat
        logger.logHeartbeat(true, null);
        
        // Test received heartbeat
        logger.logHeartbeat(false, 50L);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testErrorLogging() {
        String operation = "network_connection";
        Exception error = new RuntimeException("Test exception");
        
        logger.logError(operation, error);
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testEventBuilder() {
        // Test building a custom event
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION, NetworkEventLogger.Severity.INFO,
                "Test event with custom fields")
                .withField("customField1", "value1")
                .withField("customField2", 42)
                .withField("customField3", true)
                .log();
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testEventBuilderWithException() {
        Exception testException = new IllegalArgumentException("Test exception");
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR, NetworkEventLogger.Severity.ERROR,
                "Error event with exception")
                .withField("errorCode", "TEST_ERROR")
                .withException(testException)
                .log();
        
        // No exceptions should be thrown
    }
    
    @Test
    public void testGlobalCounters() {
        long initialEvents = NetworkEventLogger.getTotalEvents();
        long initialErrors = NetworkEventLogger.getTotalErrors();
        
        // Log some events
        logger.logConnection("localhost", 7777, true, 100);
        logger.logError("test_operation", new RuntimeException("test"));
        
        // Counters should increase (though exact values depend on other tests)
        assertTrue(NetworkEventLogger.getTotalEvents() > initialEvents, 
                  "Total events should increase");
        assertTrue(NetworkEventLogger.getTotalErrors() > initialErrors, 
                  "Total errors should increase");
    }
    
    @Test
    public void testCorrelationWithMultipleEvents() {
        String correlationId = logger.startCorrelation();
        
        // Log multiple related events
        logger.logConnection("localhost", 7777, true, 100);
        logger.logMessageSent("LoginEvent", 64, true);
        logger.logMessageReceived("WelcomeEvent", 32, 10);
        
        // All events should have the same correlation ID in their context
        assertEquals(correlationId, MDC.get("correlationId"), 
                    "All events should share correlation ID");
        
        logger.endCorrelation(correlationId);
        assertNull(MDC.get("correlationId"), "Correlation should be ended");
    }
    
    @Test
    public void testNullHandling() {
        // Test with null values - should not throw exceptions
        logger.setSessionContext(null, null, null);
        logger.clearSessionContext();
        
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION, NetworkEventLogger.Severity.INFO,
                "Test with null fields")
                .withField("nullField", null)
                .withField("validField", "value")
                .log();
        
        // No exceptions should be thrown
    }
}