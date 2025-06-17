package forge.error;

import java.util.Map;

/**
 * Base class for all security-related errors in the network system.
 * These errors involve authentication, authorization, and validation failures.
 */
public abstract class SecurityError extends NetworkError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new SecurityError with the specified message.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     */
    protected SecurityError(String message, boolean recoverable) {
        super(message, Severity.CRITICAL, Type.SECURITY, recoverable);
    }
    
    /**
     * Creates a new SecurityError with the specified message and cause.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     */
    protected SecurityError(String message, boolean recoverable, Throwable cause) {
        super(message, Severity.CRITICAL, Type.SECURITY, recoverable, cause);
    }
    
    /**
     * Creates a new SecurityError with the specified message, cause, and context.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected SecurityError(String message, boolean recoverable, Throwable cause, Map<String, Object> context) {
        super(message, Severity.CRITICAL, Type.SECURITY, recoverable, cause, context);
    }
    
    /**
     * Creates a new SecurityError with custom severity.
     * 
     * @param message Error message
     * @param severity Error severity
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected SecurityError(String message, Severity severity, boolean recoverable, 
                          Throwable cause, Map<String, Object> context) {
        super(message, severity, Type.SECURITY, recoverable, cause, context);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return isRecoverable() ? RecoveryStrategy.USER_INTERVENTION : RecoveryStrategy.NONE;
    }
}