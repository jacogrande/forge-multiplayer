package forge.error;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the NetworkError base class and hierarchy.
 */
public class NetworkErrorTest {
    
    private TestNetworkError testError;
    private Map<String, Object> testContext;
    
    @BeforeMethod
    public void setUp() {
        testContext = new HashMap<>();
        testContext.put("testKey", "testValue");
        testContext.put("numericKey", 42);
    }
    
    @Test
    public void testBasicNetworkErrorCreation() {
        testError = new TestNetworkError("Test message", NetworkError.Severity.ERROR, 
                                        NetworkError.Type.CONNECTION, true);
        
        assertEquals(testError.getMessage(), "Test message");
        assertEquals(testError.getSeverity(), NetworkError.Severity.ERROR);
        assertEquals(testError.getType(), NetworkError.Type.CONNECTION);
        assertTrue(testError.isRecoverable());
        assertNotNull(testError.getErrorCode());
        assertNotNull(testError.getTimestamp());
        assertTrue(testError.getErrorId() > 0);
    }
    
    @Test
    public void testNetworkErrorWithCause() {
        Exception cause = new RuntimeException("Root cause");
        testError = new TestNetworkError("Test message", NetworkError.Severity.CRITICAL, 
                                        NetworkError.Type.SECURITY, false, cause);
        
        assertEquals(testError.getCause(), cause);
        assertFalse(testError.isRecoverable());
        assertEquals(testError.getSeverity(), NetworkError.Severity.CRITICAL);
    }
    
    @Test
    public void testNetworkErrorWithContext() {
        testError = new TestNetworkError("Test message", NetworkError.Severity.WARN, 
                                        NetworkError.Type.GAME_STATE, true, null, testContext);
        
        assertEquals(testError.getContext().size(), 2);
        assertTrue(testError.getContextValue("testKey").isPresent());
        assertEquals(testError.getContextValue("testKey").get(), "testValue");
        
        assertTrue(testError.getContextValue("numericKey", Integer.class).isPresent());
        assertEquals(testError.getContextValue("numericKey", Integer.class).get(), Integer.valueOf(42));
        
        assertFalse(testError.getContextValue("nonexistent").isPresent());
        assertFalse(testError.getContextValue("testKey", Integer.class).isPresent());
    }
    
    @Test
    public void testContextManipulation() {
        testError = new TestNetworkError("Test message", NetworkError.Severity.INFO, 
                                        NetworkError.Type.PROTOCOL, true);
        
        testError.withContext("newKey", "newValue");
        assertTrue(testError.getContextValue("newKey").isPresent());
        assertEquals(testError.getContextValue("newKey").get(), "newValue");
    }
    
    @Test
    public void testErrorCodeGeneration() {
        testError = new TestNetworkError("Test message", NetworkError.Severity.ERROR, 
                                        NetworkError.Type.CONNECTION, true);
        
        String errorCode = testError.getErrorCode();
        assertTrue(errorCode.startsWith("CON-"));
        assertTrue(errorCode.contains("-"));
        assertTrue(errorCode.length() > 10);
    }
    
    @Test
    public void testSeverityComparison() {
        assertTrue(NetworkError.Severity.CRITICAL.isMoreSevereThan(NetworkError.Severity.ERROR));
        assertTrue(NetworkError.Severity.ERROR.isMoreSevereThan(NetworkError.Severity.WARN));
        assertTrue(NetworkError.Severity.WARN.isMoreSevereThan(NetworkError.Severity.INFO));
        
        assertFalse(NetworkError.Severity.INFO.isMoreSevereThan(NetworkError.Severity.WARN));
        assertFalse(NetworkError.Severity.WARN.isMoreSevereThan(NetworkError.Severity.ERROR));
        assertFalse(NetworkError.Severity.ERROR.isMoreSevereThan(NetworkError.Severity.CRITICAL));
    }
    
    @Test
    public void testUserMessage() {
        testError = new TestNetworkError("Technical message", NetworkError.Severity.ERROR, 
                                        NetworkError.Type.CONNECTION, true);
        
        String userMessage = testError.getUserMessage();
        assertTrue(userMessage.contains("Error"));
        assertTrue(userMessage.contains("Technical message"));
    }
    
    @Test
    public void testTechnicalMessage() {
        testError = new TestNetworkError("Test message", NetworkError.Severity.WARN, 
                                        NetworkError.Type.PROTOCOL, true, null, testContext);
        
        String techMessage = testError.getTechnicalMessage();
        assertTrue(techMessage.contains("NetworkError"));
        assertTrue(techMessage.contains("PRO-"));
        assertTrue(techMessage.contains("WARN"));
        assertTrue(techMessage.contains("Recoverable: true"));
        assertTrue(techMessage.contains("Test message"));
        assertTrue(techMessage.contains("Context:"));
    }
    
    @Test
    public void testTimestampOrdering() throws InterruptedException {
        TestNetworkError error1 = new TestNetworkError("First", NetworkError.Severity.INFO, 
                                                       NetworkError.Type.APPLICATION, true);
        Thread.sleep(1); // Ensure different timestamps
        TestNetworkError error2 = new TestNetworkError("Second", NetworkError.Severity.INFO, 
                                                       NetworkError.Type.APPLICATION, true);
        
        assertTrue(error1.getTimestamp().isBefore(error2.getTimestamp()));
        assertTrue(error1.getErrorId() < error2.getErrorId());
    }
    
    @Test
    public void testRecoveryStrategy() {
        testError = new TestNetworkError("Test message", NetworkError.Severity.ERROR, 
                                        NetworkError.Type.CONNECTION, true);
        
        assertEquals(testError.getRecommendedRecoveryStrategy(), NetworkError.RecoveryStrategy.RETRY);
    }
    
    // Helper test class that extends NetworkError
    private static class TestNetworkError extends NetworkError {
        private static final long serialVersionUID = 1L;
        
        public TestNetworkError(String message, Severity severity, Type type, boolean recoverable) {
            super(message, severity, type, recoverable);
        }
        
        public TestNetworkError(String message, Severity severity, Type type, boolean recoverable, Throwable cause) {
            super(message, severity, type, recoverable, cause);
        }
        
        public TestNetworkError(String message, Severity severity, Type type, boolean recoverable, 
                               Throwable cause, Map<String, Object> context) {
            super(message, severity, type, recoverable, cause, context);
        }
        
        @Override
        public RecoveryStrategy getRecommendedRecoveryStrategy() {
            return RecoveryStrategy.RETRY;
        }
    }
}