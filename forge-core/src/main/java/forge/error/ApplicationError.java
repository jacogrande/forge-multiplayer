package forge.error;

import java.util.Map;

/**
 * Base class for application-level errors that occur outside of network protocols.
 * These include resource limitations, configuration errors, and other system-level issues.
 */
public abstract class ApplicationError extends NetworkError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new ApplicationError with the specified message.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     */
    protected ApplicationError(String message, boolean recoverable) {
        super(message, Severity.ERROR, Type.APPLICATION, recoverable);
    }
    
    /**
     * Creates a new ApplicationError with the specified message and cause.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     */
    protected ApplicationError(String message, boolean recoverable, Throwable cause) {
        super(message, Severity.ERROR, Type.APPLICATION, recoverable, cause);
    }
    
    /**
     * Creates a new ApplicationError with the specified message, cause, and context.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected ApplicationError(String message, boolean recoverable, Throwable cause, Map<String, Object> context) {
        super(message, Severity.ERROR, Type.APPLICATION, recoverable, cause, context);
    }
    
    /**
     * Creates a new ApplicationError with custom severity.
     * 
     * @param message Error message
     * @param severity Error severity
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected ApplicationError(String message, Severity severity, boolean recoverable, 
                             Throwable cause, Map<String, Object> context) {
        super(message, severity, Type.APPLICATION, recoverable, cause, context);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return isRecoverable() ? RecoveryStrategy.FALLBACK : RecoveryStrategy.USER_INTERVENTION;
    }
}