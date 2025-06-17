package forge.error;

import java.util.Map;

/**
 * Base class for all network connection-related errors.
 * These errors typically involve the establishment, maintenance, or loss of network connections.
 */
public abstract class ConnectionError extends NetworkError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new ConnectionError with the specified message.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     */
    protected ConnectionError(String message, boolean recoverable) {
        super(message, Severity.ERROR, Type.CONNECTION, recoverable);
    }
    
    /**
     * Creates a new ConnectionError with the specified message and cause.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     */
    protected ConnectionError(String message, boolean recoverable, Throwable cause) {
        super(message, Severity.ERROR, Type.CONNECTION, recoverable, cause);
    }
    
    /**
     * Creates a new ConnectionError with the specified message, cause, and context.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected ConnectionError(String message, boolean recoverable, Throwable cause, Map<String, Object> context) {
        super(message, Severity.ERROR, Type.CONNECTION, recoverable, cause, context);
    }
    
    /**
     * Creates a new ConnectionError with custom severity.
     * 
     * @param message Error message
     * @param severity Error severity
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected ConnectionError(String message, Severity severity, boolean recoverable, 
                            Throwable cause, Map<String, Object> context) {
        super(message, severity, Type.CONNECTION, recoverable, cause, context);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return isRecoverable() ? RecoveryStrategy.RECONNECT : RecoveryStrategy.USER_INTERVENTION;
    }
}