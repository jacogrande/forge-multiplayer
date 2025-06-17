package forge.error;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Unit tests for the ConnectionError hierarchy.
 */
public class ConnectionErrorTest {
    
    @Test
    public void testConnectionTimeoutError() {
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 8080, 5000);
        
        assertEquals(error.getHost(), "localhost");
        assertEquals(error.getPort(), 8080);
        assertEquals(error.getTimeoutMs(), 5000);
        assertEquals(error.getSeverity(), NetworkError.Severity.ERROR);
        assertEquals(error.getType(), NetworkError.Type.CONNECTION);
        assertTrue(error.isRecoverable());
        assertEquals(error.getRecommendedRecoveryStrategy(), NetworkError.RecoveryStrategy.RETRY);
        
        String userMessage = error.getUserMessage();
        assertTrue(userMessage.contains("localhost:8080"));
        assertTrue(userMessage.contains("timed out"));
    }
    
    @Test
    public void testConnectionTimeoutErrorWithCause() {
        Exception cause = new RuntimeException("Socket timeout");
        ConnectionTimeoutError error = new ConnectionTimeoutError("example.com", 443, 10000, cause);
        
        assertEquals(error.getCause(), cause);
        assertTrue(error.getContextValue("host").isPresent());
        assertEquals(error.getContextValue("host").get(), "example.com");
        assertTrue(error.getContextValue("port", Integer.class).isPresent());
        assertEquals(error.getContextValue("port", Integer.class).get(), Integer.valueOf(443));
        assertTrue(error.getContextValue("timeoutMs", Long.class).isPresent());
        assertEquals(error.getContextValue("timeoutMs", Long.class).get(), Long.valueOf(10000));
    }
    
    @Test
    public void testConnectionRefusedError() {
        ConnectionRefusedError error = new ConnectionRefusedError("127.0.0.1", 9999);
        
        assertEquals(error.getHost(), "127.0.0.1");
        assertEquals(error.getPort(), 9999);
        assertEquals(error.getSeverity(), NetworkError.Severity.ERROR);
        assertTrue(error.isRecoverable());
        assertEquals(error.getRecommendedRecoveryStrategy(), NetworkError.RecoveryStrategy.RETRY);
        
        String userMessage = error.getUserMessage();
        assertTrue(userMessage.contains("127.0.0.1:9999"));
        assertTrue(userMessage.contains("Unable to connect"));
    }
    
    @Test
    public void testConnectionRefusedErrorWithCause() {
        Exception cause = new java.net.ConnectException("Connection refused");
        ConnectionRefusedError error = new ConnectionRefusedError("server.example.com", 22, cause);
        
        assertEquals(error.getCause(), cause);
        assertTrue(error.getContextValue("reason", String.class).isPresent());
        assertEquals(error.getContextValue("reason", String.class).get(), "CONNECTION_REFUSED");
    }
    
    @Test
    public void testConnectionLostError() {
        ConnectionLostError error = new ConnectionLostError(
            ConnectionLostError.Reason.NETWORK_FAILURE, 30000);
        
        assertEquals(error.getReason(), ConnectionLostError.Reason.NETWORK_FAILURE);
        assertEquals(error.getConnectionDurationMs(), 30000);
        assertTrue(error.isRecoverable());
        assertEquals(error.getRecommendedRecoveryStrategy(), NetworkError.RecoveryStrategy.RECONNECT);
        
        String userMessage = error.getUserMessage();
        assertTrue(userMessage.contains("Lost connection"));
        assertTrue(userMessage.contains("reconnect"));
    }
    
    @Test
    public void testConnectionLostErrorReasons() {
        // Test all enum values
        for (ConnectionLostError.Reason reason : ConnectionLostError.Reason.values()) {
            ConnectionLostError error = new ConnectionLostError(reason, 1000);
            assertNotNull(error.getReason().getDescription());
            assertEquals(error.getReason(), reason);
        }
    }
    
    @Test
    public void testConnectionLostErrorWithCause() {
        Exception cause = new java.io.IOException("Network unreachable");
        ConnectionLostError error = new ConnectionLostError(
            ConnectionLostError.Reason.HEARTBEAT_TIMEOUT, 45000, cause);
        
        assertEquals(error.getCause(), cause);
        assertTrue(error.getContextValue("reason").isPresent());
        assertEquals(error.getContextValue("reason").get(), ConnectionLostError.Reason.HEARTBEAT_TIMEOUT);
        assertTrue(error.getContextValue("connectionDurationMs", Long.class).isPresent());
        assertEquals(error.getContextValue("connectionDurationMs", Long.class).get(), Long.valueOf(45000));
        assertTrue(error.getContextValue("recoverable", Boolean.class).isPresent());
        assertTrue(error.getContextValue("recoverable", Boolean.class).get());
    }
    
    @Test
    public void testConnectionErrorInheritance() {
        ConnectionTimeoutError timeoutError = new ConnectionTimeoutError("test", 80, 1000);
        assertTrue(timeoutError instanceof ConnectionError);
        assertTrue(timeoutError instanceof NetworkError);
        
        ConnectionRefusedError refusedError = new ConnectionRefusedError("test", 80);
        assertTrue(refusedError instanceof ConnectionError);
        assertTrue(refusedError instanceof NetworkError);
        
        ConnectionLostError lostError = new ConnectionLostError(ConnectionLostError.Reason.UNKNOWN, 1000);
        assertTrue(lostError instanceof ConnectionError);
        assertTrue(lostError instanceof NetworkError);
    }
}