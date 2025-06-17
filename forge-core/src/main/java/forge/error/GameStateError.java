package forge.error;

import java.util.Map;

/**
 * Base class for all game state-related errors.
 * These errors occur when game state synchronization fails or game state becomes corrupted.
 */
public abstract class GameStateError extends NetworkError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new GameStateError with the specified message.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     */
    protected GameStateError(String message, boolean recoverable) {
        super(message, Severity.ERROR, Type.GAME_STATE, recoverable);
    }
    
    /**
     * Creates a new GameStateError with the specified message and cause.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     */
    protected GameStateError(String message, boolean recoverable, Throwable cause) {
        super(message, Severity.ERROR, Type.GAME_STATE, recoverable, cause);
    }
    
    /**
     * Creates a new GameStateError with the specified message, cause, and context.
     * 
     * @param message Error message
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected GameStateError(String message, boolean recoverable, Throwable cause, Map<String, Object> context) {
        super(message, Severity.ERROR, Type.GAME_STATE, recoverable, cause, context);
    }
    
    /**
     * Creates a new GameStateError with custom severity.
     * 
     * @param message Error message
     * @param severity Error severity
     * @param recoverable Whether this error is recoverable
     * @param cause Underlying cause
     * @param context Additional context
     */
    protected GameStateError(String message, Severity severity, boolean recoverable, 
                           Throwable cause, Map<String, Object> context) {
        super(message, severity, Type.GAME_STATE, recoverable, cause, context);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return isRecoverable() ? RecoveryStrategy.RESYNC : RecoveryStrategy.USER_INTERVENTION;
    }
}