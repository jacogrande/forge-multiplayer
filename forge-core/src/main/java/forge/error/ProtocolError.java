package forge.error;

import java.util.Map;

/**
 * Base class for all network protocol-related errors.
 * These errors occur when the network protocol is violated or malformed messages are received.
 */
public abstract class ProtocolError extends NetworkError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new ProtocolError with the specified message.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     */
    protected ProtocolError(String message, boolean recoverable) {
        super(message, Severity.ERROR, Type.PROTOCOL, recoverable);
    }
    
    /**
     * Creates a new ProtocolError with the specified message and cause.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     */
    protected ProtocolError(String message, boolean recoverable, Throwable cause) {
        super(message, Severity.ERROR, Type.PROTOCOL, recoverable, cause);
    }
    
    /**
     * Creates a new ProtocolError with the specified message, cause, and context.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected ProtocolError(String message, boolean recoverable, Throwable cause, Map<String, Object> context) {
        super(message, Severity.ERROR, Type.PROTOCOL, recoverable, cause, context);
    }
    
    /**
     * Creates a new ProtocolError with custom severity.
     * 
     * @param message Error message
     * @param severity Error severity
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected ProtocolError(String message, Severity severity, boolean recoverable, 
                          Throwable cause, Map<String, Object> context) {
        super(message, severity, Type.PROTOCOL, recoverable, cause, context);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return isRecoverable() ? RecoveryStrategy.RETRY : RecoveryStrategy.RECONNECT;
    }
}