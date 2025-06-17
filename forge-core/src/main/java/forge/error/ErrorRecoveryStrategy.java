package forge.error;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for implementing error recovery strategies.
 * Recovery strategies encapsulate the logic for automatically handling specific types of errors.
 */
public interface ErrorRecoveryStrategy {
    
    /**
     * Determines if this strategy can recover from the given error.
     * 
     * @param error The error to evaluate
     * @return true if this strategy can handle the error
     */
    boolean canRecover(NetworkError error);
    
    /**
     * Attempts to recover from the given error.
     * This operation is asynchronous and returns a future with the recovery result.
     * 
     * @param error The error to recover from
     * @param context Recovery context containing necessary information
     * @return A future containing the recovery result
     */
    CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context);
    
    /**
     * Gets the recovery type that this strategy implements.
     * 
     * @return The recovery type
     */
    NetworkError.RecoveryStrategy getRecoveryType();
    
    /**
     * Gets the priority of this strategy when multiple strategies can handle the same error.
     * Higher values indicate higher priority.
     * 
     * @return Strategy priority (0-100)
     */
    default int getPriority() {
        return 50; // Default medium priority
    }
    
    /**
     * Gets the maximum number of recovery attempts this strategy should make.
     * 
     * @return Maximum attempts (0 for unlimited, -1 for single attempt)
     */
    default int getMaxAttempts() {
        return 3; // Default to 3 attempts
    }
    
    /**
     * Gets the delay between recovery attempts in milliseconds.
     * 
     * @param attemptNumber The current attempt number (starting from 1)
     * @return Delay in milliseconds
     */
    default long getRetryDelayMs(int attemptNumber) {
        // Default exponential backoff: 1s, 2s, 4s, etc.
        return 1000L * (1L << (attemptNumber - 1));
    }
    
    /**
     * Called when recovery has failed after all attempts.
     * Allows the strategy to perform cleanup or logging.
     * 
     * @param error The original error
     * @param context Recovery context
     * @param lastResult The result of the final recovery attempt
     */
    default void onRecoveryFailed(NetworkError error, RecoveryContext context, RecoveryResult lastResult) {
        // Default implementation does nothing
    }
    
    /**
     * Called when recovery has succeeded.
     * Allows the strategy to perform cleanup or logging.
     * 
     * @param error The original error
     * @param context Recovery context
     * @param result The successful recovery result
     */
    default void onRecoverySucceeded(NetworkError error, RecoveryContext context, RecoveryResult result) {
        // Default implementation does nothing
    }
}