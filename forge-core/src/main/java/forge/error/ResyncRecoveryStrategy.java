package forge.error;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Recovery strategy that attempts to resynchronize game state.
 * Suitable for game state errors and synchronization issues.
 */
public class ResyncRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = Logger.getLogger(ResyncRecoveryStrategy.class.getName());
    
    private final Supplier<Boolean> resyncOperation;
    private final int maxAttempts;
    
    /**
     * Creates a new ResyncRecoveryStrategy with default parameters.
     * 
     * @param resyncOperation Operation to perform resynchronization that returns true on success
     */
    public ResyncRecoveryStrategy(Supplier<Boolean> resyncOperation) {
        this(resyncOperation, 2); // Fewer attempts for resync
    }
    
    /**
     * Creates a new ResyncRecoveryStrategy with custom parameters.
     * 
     * @param resyncOperation Operation to perform resynchronization that returns true on success
     * @param maxAttempts Maximum number of resync attempts
     */
    public ResyncRecoveryStrategy(Supplier<Boolean> resyncOperation, int maxAttempts) {
        this.resyncOperation = resyncOperation;
        this.maxAttempts = maxAttempts;
    }
    
    @Override
    public boolean canRecover(NetworkError error) {
        // Can recover from game state errors and validation errors that recommend resync
        return error.isRecoverable() && 
               (error instanceof GameStateError ||
                error instanceof ValidationError ||
                error.getRecommendedRecoveryStrategy() == NetworkError.RecoveryStrategy.RESYNC);
    }
    
    @Override
    public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                logger.log(Level.INFO, "Resync attempt {0}/{1} for error: {2}", 
                          new Object[]{attempt, maxAttempts, error.getErrorCode()});
                
                try {
                    if (resyncOperation.get()) {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.log(Level.INFO, "Resync successful after {0} attempts in {1}ms", 
                                  new Object[]{attempt, duration});
                        return RecoveryResult.success(
                            String.format("Game state resynchronized after %d attempts", attempt), 
                            duration
                        ).withMetadata("attempts", attempt)
                         .withMetadata("strategy", "resync");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Resync attempt " + attempt + " failed", e);
                    
                    if (attempt == maxAttempts) {
                        long duration = System.currentTimeMillis() - startTime;
                        return RecoveryResult.failure(
                            String.format("All %d resync attempts failed", maxAttempts), 
                            duration, e
                        );
                    }
                }
                
                // Short delay before next attempt
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000); // Fixed 1-second delay for resync
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        long duration = System.currentTimeMillis() - startTime;
                        return RecoveryResult.failure("Resync interrupted", duration, e);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return RecoveryResult.failure(
                String.format("All %d resync attempts failed", maxAttempts), 
                duration
            );
        });
    }
    
    @Override
    public NetworkError.RecoveryStrategy getRecoveryType() {
        return NetworkError.RecoveryStrategy.RESYNC;
    }
    
    @Override
    public int getPriority() {
        return 80; // High priority for game state issues
    }
    
    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    @Override
    public long getRetryDelayMs(int attemptNumber) {
        return 1000; // Fixed 1-second delay for resync operations
    }
    
    @Override
    public void onRecoveryFailed(NetworkError error, RecoveryContext context, RecoveryResult lastResult) {
        logger.log(Level.SEVERE, "Resync recovery failed for error {0}: {1}. Game state may be corrupted.", 
                  new Object[]{error.getErrorCode(), lastResult.getMessage()});
    }
    
    @Override
    public void onRecoverySucceeded(NetworkError error, RecoveryContext context, RecoveryResult result) {
        logger.log(Level.INFO, "Resync recovery succeeded for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), result.getMessage()});
    }
}