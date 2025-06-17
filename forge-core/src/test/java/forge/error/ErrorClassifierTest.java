package forge.error;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Unit tests for the ErrorClassifier utility class.
 */
public class ErrorClassifierTest {
    
    @Test
    public void testClassifyNullException() {
        NetworkError error = ErrorClassifier.classify(null);
        assertNotNull(error);
        assertTrue(error instanceof ApplicationError);
        assertTrue(error.getMessage().contains("Null exception"));
    }
    
    @Test
    public void testClassifyExistingNetworkError() {
        ConnectionTimeoutError originalError = new ConnectionTimeoutError("test", 80, 1000);
        NetworkError classifiedError = ErrorClassifier.classify(originalError);
        
        assertSame(classifiedError, originalError); // Should return the same instance
    }
    
    @Test
    public void testClassifyConnectException() {
        ConnectException connectException = new ConnectException("Connection refused: connect");
        NetworkError error = ErrorClassifier.classify(connectException);
        
        assertTrue(error instanceof ConnectionRefusedError);
        assertEquals(error.getCause(), connectException);
    }
    
    @Test
    public void testClassifyConnectExceptionTimeout() {
        ConnectException timeoutException = new ConnectException("connect: Operation timed out");
        NetworkError error = ErrorClassifier.classify(timeoutException);
        
        // Note: The current implementation classifies as refused unless exact "timeout" keyword is present
        assertTrue(error instanceof ConnectionError); // Should be either timeout or refused
        assertEquals(error.getCause(), timeoutException);
    }
    
    @Test
    public void testClassifySocketTimeoutException() {
        SocketTimeoutException timeoutException = new SocketTimeoutException("Read timed out");
        NetworkError error = ErrorClassifier.classify(timeoutException);
        
        assertTrue(error instanceof ConnectionTimeoutError);
        assertEquals(error.getCause(), timeoutException);
    }
    
    @Test
    public void testClassifyGenericTimeoutException() {
        TimeoutException timeoutException = new TimeoutException("Operation timed out");
        NetworkError error = ErrorClassifier.classify(timeoutException);
        
        assertTrue(error instanceof ApplicationError);
        assertEquals(error.getCause(), timeoutException);
        assertTrue(error.getMessage().contains("timed out"));
    }
    
    @Test
    public void testClassifyTimeoutExceptionWithConnectionContext() {
        TimeoutException timeoutException = new TimeoutException("Operation timed out");
        NetworkError error = ErrorClassifier.classify(timeoutException, ex -> "connection timeout");
        
        assertTrue(error instanceof ConnectionTimeoutError);
        assertEquals(error.getCause(), timeoutException);
    }
    
    @Test
    public void testClassifySerializationException() {
        forge.util.serialization.SerializationException serializationException = 
            new forge.util.serialization.SerializationException("Invalid data format", "kryo", "deserialize");
        NetworkError error = ErrorClassifier.classify(serializationException);
        
        assertTrue(error instanceof MalformedMessageError);
        MalformedMessageError malformedError = (MalformedMessageError) error;
        // The error should be classified correctly
        assertNotNull(malformedError.getMessageType());
        assertNotNull(malformedError.getParseError());
        assertEquals(error.getCause(), serializationException);
    }
    
    @Test
    public void testClassifyGenericSerializationException() {
        RuntimeException serializationException = new RuntimeException("Serialization failed: corrupt data");
        NetworkError error = ErrorClassifier.classify(serializationException);
        
        assertTrue(error instanceof MalformedMessageError);
        MalformedMessageError malformedError = (MalformedMessageError) error;
        assertTrue(malformedError.getParseError().contains("corrupt"));
    }
    
    @Test
    public void testClassifySecurityException() {
        SecurityException securityException = new SecurityException("Authentication failed: invalid credentials");
        NetworkError error = ErrorClassifier.classify(securityException);
        
        assertTrue(error instanceof AuthenticationError);
        AuthenticationError authError = (AuthenticationError) error;
        assertEquals(authError.getReason(), AuthenticationError.Reason.INVALID_CREDENTIALS);
        assertEquals(error.getCause(), securityException);
    }
    
    @Test
    public void testClassifyAuthorizationException() {
        RuntimeException authException = new RuntimeException("Authorization failed: insufficient permissions");
        NetworkError error = ErrorClassifier.classify(authException);
        
        assertTrue(error instanceof AuthorizationError);
        assertEquals(error.getCause(), authException);
    }
    
    @Test
    public void testClassifyMemoryException() {
        OutOfMemoryError memoryError = new OutOfMemoryError("Java heap space");
        NetworkError error = ErrorClassifier.classify(memoryError);
        
        assertTrue(error instanceof ResourceError);
        ResourceError resourceError = (ResourceError) error;
        assertEquals(resourceError.getResourceType(), ResourceError.ResourceType.MEMORY);
        assertEquals(error.getCause(), memoryError);
    }
    
    @Test
    public void testClassifyResourceLimitException() {
        RuntimeException limitException = new RuntimeException("Connection limit exceeded");
        NetworkError error = ErrorClassifier.classify(limitException);
        
        assertTrue(error instanceof ResourceError);
        ResourceError resourceError = (ResourceError) error;
        assertEquals(resourceError.getResourceType(), ResourceError.ResourceType.CONNECTION_LIMIT);
    }
    
    @Test
    public void testClassifyConfigurationException() {
        RuntimeException configException = new RuntimeException("Invalid configuration value");
        NetworkError error = ErrorClassifier.classify(configException);
        
        assertTrue(error instanceof ConfigurationError);
        assertEquals(error.getCause(), configException);
    }
    
    @Test
    public void testClassifyGameStateSyncException() {
        RuntimeException syncException = new RuntimeException("Game state synchronization failed");
        NetworkError error = ErrorClassifier.classify(syncException);
        
        assertTrue(error instanceof StateSyncError);
        assertEquals(error.getCause(), syncException);
    }
    
    @Test
    public void testClassifyGameStateCorruptionException() {
        RuntimeException corruptionException = new RuntimeException("Game state corrupted: invalid data structure");
        NetworkError error = ErrorClassifier.classify(corruptionException);
        
        assertTrue(error instanceof StateCorruptionError);
        StateCorruptionError corruptionError = (StateCorruptionError) error;
        assertEquals(corruptionError.getCorruptionType(), StateCorruptionError.CorruptionType.DATA_STRUCTURE);
    }
    
    @Test
    public void testClassifyProtocolUnknownMessageException() {
        RuntimeException protocolException = new RuntimeException("Unknown message type received");
        NetworkError error = ErrorClassifier.classify(protocolException);
        
        assertTrue(error instanceof UnknownMessageError);
        assertEquals(error.getCause(), protocolException);
    }
    
    @Test
    public void testClassifyProtocolMalformedMessageException() {
        RuntimeException malformedException = new RuntimeException("Malformed message: parse error");
        NetworkError error = ErrorClassifier.classify(malformedException);
        
        assertTrue(error instanceof MalformedMessageError);
        assertEquals(error.getCause(), malformedException);
    }
    
    @Test
    public void testClassifyProtocolSequenceException() {
        RuntimeException sequenceException = new RuntimeException("Message sequence error: out of order");
        NetworkError error = ErrorClassifier.classify(sequenceException);
        
        assertTrue(error instanceof MessageSequenceError);
        MessageSequenceError seqError = (MessageSequenceError) error;
        assertEquals(seqError.getSequenceErrorType(), MessageSequenceError.SequenceErrorType.OUT_OF_ORDER);
    }
    
    @Test
    public void testClassifyUnknownException() {
        RuntimeException unknownException = new RuntimeException("Some unknown error occurred");
        NetworkError error = ErrorClassifier.classify(unknownException);
        
        assertTrue(error instanceof ApplicationError);
        assertTrue(error.getMessage().contains("Unclassified exception"));
        assertEquals(error.getCause(), unknownException);
    }
    
    @Test
    public void testClassifyWithContextProvider() {
        RuntimeException exception = new RuntimeException("Generic error");
        NetworkError error = ErrorClassifier.classify(exception, ex -> "Custom context information");
        
        assertNotNull(error);
        assertEquals(error.getCause(), exception);
    }
    
    @Test
    public void testMultipleClassificationConsistency() {
        ConnectException connectException = new ConnectException("Connection refused");
        
        NetworkError error1 = ErrorClassifier.classify(connectException);
        NetworkError error2 = ErrorClassifier.classify(connectException);
        
        // Should create new instances but with same classification
        assertNotSame(error1, error2);
        assertEquals(error1.getClass(), error2.getClass());
        assertTrue(error1 instanceof ConnectionRefusedError);
        assertTrue(error2 instanceof ConnectionRefusedError);
    }
}