package forge.gui.logging;

import forge.gui.network.NetworkEventLogger;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import org.slf4j.MDC;
import java.util.Map;

/**
 * Validation tests for the existing NetworkEventLogger functionality.
 * These tests verify that the current structured logging implementation
 * meets the requirements specified in Task 4.2.
 */
public class NetworkEventLoggerValidationTest {
    
    private NetworkEventLogger logger;
    private static final String TEST_COMPONENT = "NetworkEventLoggerValidationTest";
    
    @BeforeMethod
    public void setUp() {
        logger = NetworkEventLogger.forComponent(TEST_COMPONENT);
        MDC.clear(); // Clean slate for each test
    }
    
    @AfterMethod
    public void tearDown() {
        MDC.clear(); // Clean up after each test
    }
    
    @Test
    public void testLoggerCreationForComponent() {
        // Verify component-specific logger creation
        NetworkEventLogger clientLogger = NetworkEventLogger.forComponent("FGameClient");
        NetworkEventLogger serverLogger = NetworkEventLogger.forComponent("FServerManager");
        
        assertNotNull(clientLogger);
        assertNotNull(serverLogger);
        
        // Should return same instance for same component
        NetworkEventLogger clientLogger2 = NetworkEventLogger.forComponent("FGameClient");
        assertSame(clientLogger, clientLogger2);
    }
    
    @Test
    public void testCorrelationIdTracking() {
        // Test correlation ID lifecycle
        String correlationId = logger.startCorrelation();
        
        assertNotNull(correlationId);
        assertEquals(8, correlationId.length()); // Should be 8 chars from UUID
        assertEquals(correlationId, MDC.get("correlationId"));
        
        logger.endCorrelation(correlationId);
        assertNull(MDC.get("correlationId"));
    }
    
    @Test
    public void testCorrelationIdPersistenceDuringLogging() {
        String correlationId = logger.startCorrelation();
        
        // Log an event and verify correlation ID is maintained
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION, 
                       NetworkEventLogger.Severity.INFO,
                       "Test correlation tracking").log();
        
        // Correlation ID should still be present after logging
        assertEquals(correlationId, MDC.get("correlationId"));
        
        logger.endCorrelation(correlationId);
    }
    
    @Test
    public void testSessionContextManagement() {
        String sessionId = "test-session-123";
        String username = "testuser";
        String gameId = "game-456";
        
        logger.setSessionContext(sessionId, username, gameId);
        
        assertEquals(sessionId, MDC.get("sessionId"));
        assertEquals(username, MDC.get("username"));
        assertEquals(gameId, MDC.get("gameId"));
        
        logger.clearSessionContext();
        
        assertNull(MDC.get("sessionId"));
        assertNull(MDC.get("username"));
        assertNull(MDC.get("gameId"));
    }
    
    @Test
    public void testConnectionEventLogging() {
        String host = "localhost";
        int port = 7777;
        boolean success = true;
        long duration = 1500;
        
        // This should not throw exceptions
        logger.logConnection(host, port, success, duration);
        
        // Test failed connection
        logger.logConnection(host, port, false, duration);
    }
    
    @Test
    public void testDisconnectionEventLogging() {
        String reason = "Network timeout";
        boolean unexpected = true;
        
        logger.logDisconnection(reason, unexpected);
        
        // Test expected disconnection
        logger.logDisconnection("User logout", false);
    }
    
    @Test
    public void testReconnectionEventLogging() {
        int attempt = 2;
        int maxAttempts = 5;
        long delay = 2000;
        boolean success = false;
        
        logger.logReconnectionAttempt(attempt, maxAttempts, delay, success);
        
        // Test successful reconnection
        logger.logReconnectionAttempt(3, maxAttempts, 4000, true);
    }
    
    @Test
    public void testMessageLogging() {
        String messageType = "GAME_STATE_UPDATE";
        int messageSize = 2048;
        long processingTime = 50;
        
        logger.logMessageSent(messageType, messageSize, true);
        logger.logMessageSent(messageType, messageSize, false); // Failed send
        
        logger.logMessageReceived(messageType, messageSize, processingTime);
    }
    
    @Test
    public void testGameStateSyncLogging() {
        String operation = "CAPTURE";
        boolean success = true;
        long duration = 250;
        long stateSize = 65536;
        
        logger.logGameStateSync(operation, success, duration, stateSize);
        
        // Test failed sync
        logger.logGameStateSync("RESTORE", false, duration, stateSize);
    }
    
    @Test
    public void testSecurityEventLogging() {
        String securityEvent = "INVALID_ACTION_ATTEMPT";
        String details = "Player attempted to play card during opponent's turn";
        NetworkEventLogger.Severity severity = NetworkEventLogger.Severity.WARN;
        
        logger.logSecurityEvent(securityEvent, details, severity);
    }
    
    @Test
    public void testPerformanceMetricLogging() {
        String metric = "serialization_time";
        double value = 15.5;
        String unit = "ms";
        
        // Should not throw exceptions
        logger.logPerformanceMetric(metric, value, unit);
    }
    
    @Test
    public void testHeartbeatLogging() {
        // Test sent heartbeat
        logger.logHeartbeat(true, null);
        
        // Test received heartbeat with latency
        logger.logHeartbeat(false, 25L);
    }
    
    @Test
    public void testErrorLogging() {
        String operation = "game_state_deserialization";
        Exception error = new RuntimeException("Test error message");
        
        logger.logError(operation, error);
    }
    
    @Test
    public void testStructuredLogEventBuilder() {
        // Test the fluent API for building structured log events
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "Test structured event: {} with value {}",
                       "metric_name", 42)
               .withField("customField", "customValue")
               .withField("numericField", 123)
               .withField("booleanField", true)
               .log();
    }
    
    @Test
    public void testLogEventBuilderWithMultipleFields() {
        Map<String, Object> additionalFields = Map.of(
            "field1", "value1",
            "field2", 42,
            "field3", true
        );
        
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                       NetworkEventLogger.Severity.DEBUG,
                       "Multi-field test event")
               .withFields(additionalFields)
               .withField("singleField", "singleValue")
               .log();
    }
    
    @Test
    public void testLogEventBuilderWithException() {
        Exception testException = new IllegalArgumentException("Test exception for logging");
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.ERROR,
                       "Error event with exception")
               .withField("errorContext", "unit_test")
               .withException(testException)
               .log();
    }
    
    @Test
    public void testEventTypeValues() {
        // Verify all event types have proper string values
        assertEquals("connection", NetworkEventLogger.EventType.CONNECTION.getValue());
        assertEquals("disconnection", NetworkEventLogger.EventType.DISCONNECTION.getValue());
        assertEquals("reconnection", NetworkEventLogger.EventType.RECONNECTION.getValue());
        assertEquals("message_sent", NetworkEventLogger.EventType.MESSAGE_SENT.getValue());
        assertEquals("message_received", NetworkEventLogger.EventType.MESSAGE_RECEIVED.getValue());
        assertEquals("game_state_sync", NetworkEventLogger.EventType.GAME_STATE_SYNC.getValue());
        assertEquals("security", NetworkEventLogger.EventType.SECURITY_EVENT.getValue());
        assertEquals("performance", NetworkEventLogger.EventType.PERFORMANCE.getValue());
        assertEquals("error", NetworkEventLogger.EventType.ERROR.getValue());
        assertEquals("heartbeat", NetworkEventLogger.EventType.HEARTBEAT.getValue());
        assertEquals("session", NetworkEventLogger.EventType.SESSION.getValue());
    }
    
    @Test
    public void testSeverityLevels() {
        // Verify all severity levels are available
        NetworkEventLogger.Severity[] severities = NetworkEventLogger.Severity.values();
        
        assertEquals(6, severities.length);
        assertTrue(containsSeverity(severities, NetworkEventLogger.Severity.TRACE));
        assertTrue(containsSeverity(severities, NetworkEventLogger.Severity.DEBUG));
        assertTrue(containsSeverity(severities, NetworkEventLogger.Severity.INFO));
        assertTrue(containsSeverity(severities, NetworkEventLogger.Severity.WARN));
        assertTrue(containsSeverity(severities, NetworkEventLogger.Severity.ERROR));
        assertTrue(containsSeverity(severities, NetworkEventLogger.Severity.CRITICAL));
    }
    
    @Test
    public void testEventCounters() {
        long initialEvents = NetworkEventLogger.getTotalEvents();
        long initialErrors = NetworkEventLogger.getTotalErrors();
        
        // Log some events
        logger.logConnection("test", 8080, true, 100);
        logger.logError("test_operation", new RuntimeException("test"));
        
        // Verify counters increased
        assertTrue(NetworkEventLogger.getTotalEvents() > initialEvents);
        assertTrue(NetworkEventLogger.getTotalErrors() > initialErrors);
    }
    
    @Test
    public void testNullSafetyInLogEventBuilder() {
        // Test that null values are handled gracefully
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                       NetworkEventLogger.Severity.INFO,
                       "Null safety test")
               .withField("nullField", null)
               .withFields(null)
               .log();
    }
    
    private boolean containsSeverity(NetworkEventLogger.Severity[] severities, NetworkEventLogger.Severity target) {
        for (NetworkEventLogger.Severity severity : severities) {
            if (severity == target) {
                return true;
            }
        }
        return false;
    }
}