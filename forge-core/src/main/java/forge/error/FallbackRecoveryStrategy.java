package forge.error;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Recovery strategy that provides fallback functionality when primary operations fail.
 * Suitable for application errors and resource limitations.
 */
public class FallbackRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = Logger.getLogger(FallbackRecoveryStrategy.class.getName());
    
    private final Supplier<Boolean> fallbackOperation;
    private final String fallbackDescription;
    
    /**
     * Creates a new FallbackRecoveryStrategy.
     * 
     * @param fallbackOperation Operation to perform as fallback that returns true on success
     * @param fallbackDescription Description of the fallback behavior
     */
    public FallbackRecoveryStrategy(Supplier<Boolean> fallbackOperation, String fallbackDescription) {
        this.fallbackOperation = fallbackOperation;
        this.fallbackDescription = fallbackDescription;
    }
    
    @Override
    public boolean canRecover(NetworkError error) {
        // Can recover from application errors and some other recoverable errors
        return error.isRecoverable() && 
               (error instanceof ApplicationError ||
                error.getRecommendedRecoveryStrategy() == NetworkError.RecoveryStrategy.FALLBACK);
    }
    
    @Override
    public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            logger.log(Level.INFO, "Attempting fallback recovery for error: {0}", error.getErrorCode());
            
            try {
                if (fallbackOperation.get()) {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.log(Level.INFO, "Fallback recovery successful in {0}ms: {1}", 
                              new Object[]{duration, fallbackDescription});
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("strategy", "fallback");
                    metadata.put("fallbackDescription", fallbackDescription);
                    return RecoveryResult.partialSuccess(
                        String.format("Fallback successful: %s", fallbackDescription), 
                        duration,
                        metadata
                    );
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fallback recovery failed", e);
                long duration = System.currentTimeMillis() - startTime;
                return RecoveryResult.failure(
                    String.format("Fallback failed: %s", fallbackDescription), 
                    duration, e
                );
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return RecoveryResult.failure(
                String.format("Fallback operation failed: %s", fallbackDescription), 
                duration
            );
        });
    }
    
    @Override
    public NetworkError.RecoveryStrategy getRecoveryType() {
        return NetworkError.RecoveryStrategy.FALLBACK;
    }
    
    @Override
    public int getPriority() {
        return 40; // Lower priority - fallback when other strategies fail
    }
    
    @Override
    public int getMaxAttempts() {
        return 1; // Fallback is typically single attempt
    }
    
    @Override
    public long getRetryDelayMs(int attemptNumber) {
        return 0; // No delay for fallback
    }
    
    @Override
    public void onRecoveryFailed(NetworkError error, RecoveryContext context, RecoveryResult lastResult) {
        logger.log(Level.WARNING, "Fallback recovery failed for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), lastResult.getMessage()});
    }
    
    @Override
    public void onRecoverySucceeded(NetworkError error, RecoveryContext context, RecoveryResult result) {
        logger.log(Level.INFO, "Fallback recovery succeeded for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), result.getMessage()});
    }
}