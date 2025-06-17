package forge.gui.logging;

import forge.gui.network.NetworkEventLogger;
import forge.gui.util.LoggingInitializer;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.JsonEncoder;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation tests for structured JSON logging functionality.
 * Verifies that log output is properly formatted as JSON with required fields.
 */
public class StructuredLoggingValidationTest {
    
    private NetworkEventLogger logger;
    private ByteArrayOutputStream logOutput;
    private OutputStreamAppender<ILoggingEvent> testAppender;
    
    @BeforeClass
    public void setUpClass() {
        // Initialize the logging system
        LoggingInitializer.initialize();
    }
    
    @BeforeMethod
    public void setUp() {
        logger = NetworkEventLogger.forComponent("StructuredLoggingTest");
        MDC.clear();
        
        // Set up a test appender to capture log output
        setupTestAppender();
    }
    
    @AfterMethod
    public void tearDown() {
        if (testAppender != null) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(testAppender);
        }
        MDC.clear();
    }
    
    private void setupTestAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Create output stream to capture logs
        logOutput = new ByteArrayOutputStream();
        
        // Create JSON encoder
        JsonEncoder jsonEncoder = new JsonEncoder();
        jsonEncoder.setContext(context);
        jsonEncoder.start();
        
        // Create and configure appender
        testAppender = new OutputStreamAppender<>();
        testAppender.setContext(context);
        testAppender.setName("TestAppender");
        testAppender.setEncoder(jsonEncoder);
        testAppender.setOutputStream(logOutput);
        testAppender.start();
        
        // Attach to logger
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(testAppender);
    }
    
    @Test
    public void testLoggingInitializerHealth() {
        assertTrue(LoggingInitializer.isInitialized());
        assertTrue(LoggingInitializer.healthCheck());
        
        String systemInfo = LoggingInitializer.getSystemInfo();
        assertNotNull(systemInfo);
        assertTrue(systemInfo.contains("Initialized: true"));
    }
    
    @Test
    public void testBasicStructuredLogging() {
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                       NetworkEventLogger.Severity.INFO,
                       "Test structured log message")
               .withField("testField", "testValue")
               .withField("numericField", 42)
               .log();
        
        String output = logOutput.toString();
        
        // Should contain JSON structure
        assertTrue(isValidJson(output), "Log output should be valid JSON");
        assertTrue(output.contains("\"eventType\":\"connection\""));
        assertTrue(output.contains("\"testField\":\"testValue\""));
        assertTrue(output.contains("\"numericField\":42"));
    }
    
    @Test
    public void testMDCIntegration() {
        String correlationId = logger.startCorrelation();
        logger.setSessionContext("session-123", "testuser", "game-456");
        
        logger.logEvent(NetworkEventLogger.EventType.SESSION,
                       NetworkEventLogger.Severity.INFO,
                       "Test session context logging")
               .log();
        
        String output = logOutput.toString();
        
        assertTrue(output.contains("\"correlationId\":\"" + correlationId + "\""));
        assertTrue(output.contains("\"sessionId\":\"session-123\""));
        assertTrue(output.contains("\"username\":\"testuser\""));
        assertTrue(output.contains("\"gameId\":\"game-456\""));
        
        logger.endCorrelation(correlationId);
        logger.clearSessionContext();
    }
    
    @Test
    public void testTimestampFormat() {
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.DEBUG,
                       "Timestamp test")
               .log();
        
        String output = logOutput.toString();
        
        // Should contain ISO timestamp
        assertTrue(output.contains("\"timestamp\":"));
        
        // Verify timestamp format (ISO 8601)
        Pattern timestampPattern = Pattern.compile("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertTrue(timestampPattern.matcher(output).find(), "Should contain ISO 8601 timestamp");
    }
    
    @Test
    public void testLogLevelsInJson() {
        // Test different log levels
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.TRACE,
                       "Trace level message").log();
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.DEBUG,
                       "Debug level message").log();
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.INFO,
                       "Info level message").log();
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.WARN,
                       "Warn level message").log();
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.ERROR,
                       "Error level message").log();
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.CRITICAL,
                       "Critical level message").log();
        
        String output = logOutput.toString();
        
        // All severity levels should be represented in the JSON
        assertTrue(output.contains("Trace level message"));
        assertTrue(output.contains("Debug level message"));
        assertTrue(output.contains("Info level message"));
        assertTrue(output.contains("Warn level message"));
        assertTrue(output.contains("Error level message"));
        assertTrue(output.contains("Critical level message"));
    }
    
    @Test
    public void testExceptionLogging() {
        Exception testException = new RuntimeException("Test exception message");
        
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.ERROR,
                       "Error with exception")
               .withException(testException)
               .log();
        
        String output = logOutput.toString();
        
        // Should contain exception information
        assertTrue(output.contains("Test exception message"));
        assertTrue(output.contains("RuntimeException"));
    }
    
    @Test
    public void testComplexFieldTypes() {
        Map<String, Object> complexData = Map.of(
            "stringField", "stringValue",
            "intField", 123,
            "boolField", true,
            "doubleField", 45.67
        );
        
        logger.logEvent(NetworkEventLogger.EventType.GAME_STATE_SYNC,
                       NetworkEventLogger.Severity.INFO,
                       "Complex data logging test")
               .withFields(complexData)
               .withField("arrayLike", "[1,2,3]")
               .log();
        
        String output = logOutput.toString();
        
        assertTrue(output.contains("\"stringField\":\"stringValue\""));
        assertTrue(output.contains("\"intField\":123"));
        assertTrue(output.contains("\"boolField\":true"));
        assertTrue(output.contains("\"doubleField\":45.67"));
    }
    
    @Test
    public void testSpecialCharacterHandling() {
        logger.logEvent(NetworkEventLogger.EventType.ERROR,
                       NetworkEventLogger.Severity.WARN,
                       "Message with \"quotes\" and \\backslashes\\ and newlines\n")
               .withField("specialChars", "Value with \"quotes\" and \\ backslashes")
               .log();
        
        String output = logOutput.toString();
        
        // JSON should be valid despite special characters
        assertTrue(isValidJson(output), "JSON should remain valid with special characters");
    }
    
    @Test
    public void testPerformanceMetricsFormat() {
        logger.logPerformanceMetric("connection_latency", 125.5, "ms");
        logger.logPerformanceMetric("throughput", 1024.0, "bytes/sec");
        
        String output = logOutput.toString();
        
        // Performance metrics should have structured format
        assertTrue(output.contains("\"metric\":\"connection_latency\""));
        assertTrue(output.contains("\"value\":\"125.5\""));
        assertTrue(output.contains("\"unit\":\"ms\""));
        assertTrue(output.contains("\"metric\":\"throughput\""));
        assertTrue(output.contains("\"unit\":\"bytes/sec\""));
    }
    
    @Test
    public void testHighVolumeLogging() {
        // Test that high volume logging maintains JSON structure
        for (int i = 0; i < 100; i++) {
            logger.logEvent(NetworkEventLogger.EventType.MESSAGE_SENT,
                           NetworkEventLogger.Severity.DEBUG,
                           "High volume message {}", i)
                   .withField("messageNumber", i)
                   .withField("batch", "high_volume_test")
                   .log();
        }
        
        String output = logOutput.toString();
        
        // Should contain all messages and maintain valid JSON structure
        assertTrue(output.contains("High volume message 0"));
        assertTrue(output.contains("High volume message 99"));
        assertTrue(output.contains("\"messageNumber\":0"));
        assertTrue(output.contains("\"messageNumber\":99"));
        assertTrue(output.contains("\"batch\":\"high_volume_test\""));
    }
    
    @Test
    public void testComponentLoggerNaming() {
        NetworkEventLogger clientLogger = NetworkEventLogger.forComponent("FGameClient");
        NetworkEventLogger serverLogger = NetworkEventLogger.forComponent("FServerManager");
        
        clientLogger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                             NetworkEventLogger.Severity.INFO,
                             "Client message").log();
        
        serverLogger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                             NetworkEventLogger.Severity.INFO,
                             "Server message").log();
        
        String output = logOutput.toString();
        
        // Should be able to distinguish between component loggers
        assertTrue(output.contains("Client message"));
        assertTrue(output.contains("Server message"));
    }
    
    @Test
    public void testNullValueHandling() {
        logger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                       NetworkEventLogger.Severity.INFO,
                       "Null value test")
               .withField("nullField", null)
               .withField("validField", "validValue")
               .log();
        
        String output = logOutput.toString();
        
        // Should handle null values gracefully
        assertTrue(isValidJson(output));
        assertTrue(output.contains("\"validField\":\"validValue\""));
        // Null fields should be omitted from output
        assertFalse(output.contains("nullField"));
    }
    
    @Test
    public void testLogOutputStructure() {
        logger.logConnection("localhost", 7777, true, 1500);
        
        String output = logOutput.toString();
        
        // Verify required structured fields are present
        assertTrue(output.contains("\"eventType\":\"connection\""));
        assertTrue(output.contains("\"host\":\"localhost\""));
        assertTrue(output.contains("\"port\":7777"));
        assertTrue(output.contains("\"success\":true"));
        assertTrue(output.contains("\"durationMs\":1500"));
        assertTrue(output.contains("\"timestamp\":"));
    }
    
    /**
     * Basic JSON validation - checks for balanced braces and quotes.
     * For production use, would use a proper JSON parser.
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Count braces and brackets to ensure they're balanced
            int braceCount = 0;
            int bracketCount = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (char c : json.toCharArray()) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                
                if (!inString) {
                    switch (c) {
                        case '{': braceCount++; break;
                        case '}': braceCount--; break;
                        case '[': bracketCount++; break;
                        case ']': bracketCount--; break;
                    }
                }
            }
            
            return braceCount == 0 && bracketCount == 0 && !inString;
            
        } catch (Exception e) {
            return false;
        }
    }
}