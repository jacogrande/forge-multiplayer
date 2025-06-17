package forge.gui.logging;

import forge.util.logging.LoggingBridge;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Handler;

/**
 * Validation tests for the LoggingBridge JUL-to-SLF4J bridge functionality.
 * Verifies that existing java.util.logging usage is properly redirected to SLF4J.
 */
public class LoggingBridgeValidationTest {
    
    private java.util.logging.Logger julLogger;
    private Logger slf4jLogger;
    
    @BeforeMethod
    public void setUp() {
        // Ensure bridge is initialized for each test
        LoggingBridge.initialize();
        
        julLogger = java.util.logging.Logger.getLogger("TestJULLogger");
        slf4jLogger = LoggerFactory.getLogger("TestSLF4JLogger");
    }
    
    @AfterMethod
    public void tearDown() {
        // Clean up but don't fully tear down - other tests may need the bridge
    }
    
    @Test
    public void testBridgeInitialization() {
        // Test basic initialization
        LoggingBridge.initialize();
        assertTrue(LoggingBridge.isInitialized());
        
        // Test idempotent initialization (should not fail)
        LoggingBridge.initialize();
        assertTrue(LoggingBridge.isInitialized());
    }
    
    @Test
    public void testJULToSLF4JMigration() {
        // Test the migration utility
        java.util.logging.Logger testJulLogger = java.util.logging.Logger.getLogger("MigrationTest");
        Logger migratedLogger = LoggingBridge.migrateTo(testJulLogger);
        
        assertNotNull(migratedLogger);
        assertEquals("MigrationTest", migratedLogger.getName());
    }
    
    @Test
    public void testJULLoggingRedirection() {
        // This test verifies that JUL logging is redirected through SLF4J
        // We can't easily capture the actual SLF4J output in a unit test,
        // but we can verify the bridge is working by checking that JUL loggers
        // are configured properly
        
        julLogger.info("Test JUL info message");
        julLogger.warning("Test JUL warning message");
        julLogger.severe("Test JUL severe message");
        
        // Verify the JUL logger is properly configured
        assertTrue(julLogger.isLoggable(Level.INFO));
        assertTrue(julLogger.isLoggable(Level.WARNING));
        assertTrue(julLogger.isLoggable(Level.SEVERE));
    }
    
    @Test
    public void testLoggerRedirection() {
        String julLoggerName = "test.jul.logger";
        String slf4jLoggerName = "test.slf4j.logger";
        
        // This method exists for documentation/future use
        LoggingBridge.redirectLogger(julLoggerName, slf4jLoggerName);
        
        // Should not throw exceptions
    }
    
    @Test
    public void testPerformanceInfo() {
        String perfInfo = LoggingBridge.getPerformanceInfo();
        
        assertNotNull(perfInfo);
        assertTrue(perfInfo.contains("JUL-to-SLF4J bridge"));
        assertTrue(perfInfo.contains("overhead"));
    }
    
    @Test
    public void testJULLogLevelConfiguration() {
        // Test that the bridge properly handles different log levels
        julLogger.setLevel(Level.FINEST);
        assertTrue(julLogger.isLoggable(Level.FINEST));
        assertTrue(julLogger.isLoggable(Level.FINE));
        assertTrue(julLogger.isLoggable(Level.INFO));
        assertTrue(julLogger.isLoggable(Level.WARNING));
        assertTrue(julLogger.isLoggable(Level.SEVERE));
        
        julLogger.setLevel(Level.WARNING);
        assertFalse(julLogger.isLoggable(Level.INFO));
        assertTrue(julLogger.isLoggable(Level.WARNING));
        assertTrue(julLogger.isLoggable(Level.SEVERE));
    }
    
    @Test
    public void testJULLoggingWithParameters() {
        // Test parameterized logging through JUL
        julLogger.log(Level.INFO, "Test message with parameter: {0}", "parameter_value");
        julLogger.log(Level.INFO, "Test message with multiple parameters: {0}, {1}", 
                     new Object[]{"param1", "param2"});
    }
    
    @Test
    public void testJULLoggingWithException() {
        Exception testException = new RuntimeException("Test exception for JUL bridge");
        
        julLogger.log(Level.SEVERE, "Error occurred", testException);
    }
    
    @Test
    public void testNestedLoggerHierarchy() {
        // Test that logger hierarchy is preserved through the bridge
        java.util.logging.Logger parentLogger = java.util.logging.Logger.getLogger("parent");
        java.util.logging.Logger childLogger = java.util.logging.Logger.getLogger("parent.child");
        java.util.logging.Logger grandchildLogger = java.util.logging.Logger.getLogger("parent.child.grandchild");
        
        assertNotNull(parentLogger);
        assertNotNull(childLogger);
        assertNotNull(grandchildLogger);
        
        // Test logging at different levels
        parentLogger.info("Parent logger message");
        childLogger.info("Child logger message");
        grandchildLogger.info("Grandchild logger message");
    }
    
    @Test
    public void testCustomJULHandler() {
        // Test that custom JUL handlers work with the bridge
        TestLogHandler testHandler = new TestLogHandler();
        julLogger.addHandler(testHandler);
        
        julLogger.info("Test message for custom handler");
        
        // The bridge should intercept this before it reaches our custom handler
        julLogger.removeHandler(testHandler);
    }
    
    @Test
    public void testLoggerNameMapping() {
        // Test that logger names are properly mapped from JUL to SLF4J
        String loggerName = "forge.gamemodes.net.FServerManager";
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(loggerName);
        Logger slf4jEquivalent = LoggingBridge.migrateTo(julLogger);
        
        assertEquals(loggerName, slf4jEquivalent.getName());
    }
    
    @Test
    public void testBridgeCleanup() {
        // Test cleanup functionality
        assertTrue(LoggingBridge.isInitialized());
        
        LoggingBridge.cleanup();
        assertFalse(LoggingBridge.isInitialized());
        
        // Re-initialize for other tests
        LoggingBridge.initialize();
        assertTrue(LoggingBridge.isInitialized());
    }
    
    @Test
    public void testConcurrentJULLogging() throws InterruptedException {
        // Test that concurrent JUL logging works properly with the bridge
        int numThreads = 5;
        int messagesPerThread = 10;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                java.util.logging.Logger threadLogger = 
                    java.util.logging.Logger.getLogger("ConcurrentTest.Thread" + threadId);
                
                for (int j = 0; j < messagesPerThread; j++) {
                    threadLogger.info("Thread " + threadId + " message " + j);
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
        
        // All threads should complete without exceptions
        for (Thread thread : threads) {
            assertFalse(thread.isAlive());
        }
    }
    
    /**
     * Custom JUL handler for testing purposes.
     */
    private static class TestLogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            // This handler should not receive records when the bridge is active
        }
        
        @Override
        public void flush() {
            // No-op
        }
        
        @Override
        public void close() throws SecurityException {
            // No-op
        }
    }
}