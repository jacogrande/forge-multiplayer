package forge.error;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Recovery strategy that attempts to reconnect to the server.
 * Suitable for connection errors and some protocol errors.
 */
public class ReconnectRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = Logger.getLogger(ReconnectRecoveryStrategy.class.getName());
    
    private final Supplier<Boolean> reconnectOperation;
    private final int maxAttempts;
    private final long baseDelayMs;
    
    /**
     * Creates a new ReconnectRecoveryStrategy with default parameters.
     * 
     * @param reconnectOperation Operation to perform reconnection that returns true on success
     */
    public ReconnectRecoveryStrategy(Supplier<Boolean> reconnectOperation) {
        this(reconnectOperation, 5, 2000);
    }
    
    /**
     * Creates a new ReconnectRecoveryStrategy with custom parameters.
     * 
     * @param reconnectOperation Operation to perform reconnection that returns true on success
     * @param maxAttempts Maximum number of reconnection attempts
     * @param baseDelayMs Base delay between attempts
     */
    public ReconnectRecoveryStrategy(Supplier<Boolean> reconnectOperation, int maxAttempts, long baseDelayMs) {
        this.reconnectOperation = reconnectOperation;
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
    }
    
    @Override
    public boolean canRecover(NetworkError error) {
        // Can recover from connection errors and some protocol errors
        return error.isRecoverable() && 
               (error instanceof ConnectionError ||
                error.getRecommendedRecoveryStrategy() == NetworkError.RecoveryStrategy.RECONNECT);
    }
    
    @Override
    public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                logger.log(Level.INFO, "Reconnection attempt {0}/{1} for error: {2}", 
                          new Object[]{attempt, maxAttempts, error.getErrorCode()});
                
                try {
                    if (reconnectOperation.get()) {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.log(Level.INFO, "Reconnection successful after {0} attempts in {1}ms", 
                                  new Object[]{attempt, duration});
                        return RecoveryResult.success(
                            String.format("Reconnection successful after %d attempts", attempt), 
                            duration
                        ).withMetadata("attempts", attempt)
                         .withMetadata("strategy", "reconnect");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Reconnection attempt " + attempt + " failed", e);
                    
                    if (attempt == maxAttempts) {
                        long duration = System.currentTimeMillis() - startTime;
                        return RecoveryResult.failure(
                            String.format("All %d reconnection attempts failed", maxAttempts), 
                            duration, e
                        );
                    }
                }
                
                // Wait before next attempt
                if (attempt < maxAttempts) {
                    long delay = getRetryDelayMs(attempt);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        long duration = System.currentTimeMillis() - startTime;
                        return RecoveryResult.failure("Reconnection interrupted", duration, e);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return RecoveryResult.failure(
                String.format("All %d reconnection attempts failed", maxAttempts), 
                duration
            );
        });
    }
    
    @Override
    public NetworkError.RecoveryStrategy getRecoveryType() {
        return NetworkError.RecoveryStrategy.RECONNECT;
    }
    
    @Override
    public int getPriority() {
        return 70; // High priority for connection errors
    }
    
    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    @Override
    public long getRetryDelayMs(int attemptNumber) {
        // Exponential backoff with jitter for reconnection
        long delay = baseDelayMs * (1L << (attemptNumber - 1));
        // Add jitter to prevent thundering herd
        long jitter = (long) (Math.random() * 1000);
        return Math.min(delay + jitter, 30000); // Cap at 30 seconds
    }
    
    @Override
    public void onRecoveryFailed(NetworkError error, RecoveryContext context, RecoveryResult lastResult) {
        logger.log(Level.SEVERE, "Reconnection recovery failed for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), lastResult.getMessage()});
    }
    
    @Override
    public void onRecoverySucceeded(NetworkError error, RecoveryContext context, RecoveryResult result) {
        logger.log(Level.INFO, "Reconnection recovery succeeded for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), result.getMessage()});
    }
}