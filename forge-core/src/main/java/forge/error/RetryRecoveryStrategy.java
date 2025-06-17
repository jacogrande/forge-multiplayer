package forge.error;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Recovery strategy that implements retry logic with exponential backoff.
 * Suitable for transient errors that may resolve themselves with time.
 */
public class RetryRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = Logger.getLogger(RetryRecoveryStrategy.class.getName());
    
    private final Supplier<Boolean> retryOperation;
    private final int maxAttempts;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    
    /**
     * Creates a new RetryRecoveryStrategy with default parameters.
     * 
     * @param retryOperation Operation to retry that returns true on success
     */
    public RetryRecoveryStrategy(Supplier<Boolean> retryOperation) {
        this(retryOperation, 3, 1000, 2.0, 30000);
    }
    
    /**
     * Creates a new RetryRecoveryStrategy with custom parameters.
     * 
     * @param retryOperation Operation to retry that returns true on success
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelayMs Initial delay between attempts
     * @param backoffMultiplier Exponential backoff multiplier
     * @param maxDelayMs Maximum delay between attempts
     */
    public RetryRecoveryStrategy(Supplier<Boolean> retryOperation, int maxAttempts, 
                               long initialDelayMs, double backoffMultiplier, long maxDelayMs) {
        this.retryOperation = retryOperation;
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
    }
    
    @Override
    public boolean canRecover(NetworkError error) {
        // Can recover from most errors that are marked as recoverable
        // Specifically good for connection and protocol errors
        return error.isRecoverable() && 
               (error instanceof ConnectionError || 
                error instanceof ProtocolError ||
                error.getRecommendedRecoveryStrategy() == NetworkError.RecoveryStrategy.RETRY);
    }
    
    @Override
    public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                logger.log(Level.INFO, "Retry attempt {0}/{1} for error: {2}", 
                          new Object[]{attempt, maxAttempts, error.getErrorCode()});
                
                try {
                    if (retryOperation.get()) {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.log(Level.INFO, "Retry successful after {0} attempts in {1}ms", 
                                  new Object[]{attempt, duration});
                        return RecoveryResult.success(
                            String.format("Retry successful after %d attempts", attempt), 
                            duration
                        ).withMetadata("attempts", attempt)
                         .withMetadata("strategy", "retry");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Retry attempt " + attempt + " failed", e);
                    
                    if (attempt == maxAttempts) {
                        long duration = System.currentTimeMillis() - startTime;
                        return RecoveryResult.failure(
                            String.format("All %d retry attempts failed", maxAttempts), 
                            duration, e
                        );
                    }
                }
                
                // Calculate delay for next attempt
                if (attempt < maxAttempts) {
                    long delay = calculateDelay(attempt);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        long duration = System.currentTimeMillis() - startTime;
                        return RecoveryResult.failure("Retry interrupted", duration, e);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return RecoveryResult.failure(
                String.format("All %d retry attempts failed", maxAttempts), 
                duration
            );
        });
    }
    
    /**
     * Calculates the delay for the given attempt using exponential backoff.
     * 
     * @param attempt Attempt number (starting from 1)
     * @return Delay in milliseconds
     */
    private long calculateDelay(int attempt) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(delay, maxDelayMs);
    }
    
    @Override
    public NetworkError.RecoveryStrategy getRecoveryType() {
        return NetworkError.RecoveryStrategy.RETRY;
    }
    
    @Override
    public int getPriority() {
        return 60; // Higher than default priority
    }
    
    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    @Override
    public long getRetryDelayMs(int attemptNumber) {
        return calculateDelay(attemptNumber);
    }
    
    @Override
    public void onRecoveryFailed(NetworkError error, RecoveryContext context, RecoveryResult lastResult) {
        logger.log(Level.WARNING, "Retry recovery failed for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), lastResult.getMessage()});
    }
    
    @Override
    public void onRecoverySucceeded(NetworkError error, RecoveryContext context, RecoveryResult result) {
        logger.log(Level.INFO, "Retry recovery succeeded for error {0}: {1}", 
                  new Object[]{error.getErrorCode(), result.getMessage()});
    }
}