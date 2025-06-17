package forge.error;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central error handling dispatch system that routes errors to appropriate observers
 * and manages recovery strategies. Uses the observer pattern for error notifications
 * and provides comprehensive error tracking and statistics.
 */
public class ErrorRouter {
    
    private static final Logger logger = Logger.getLogger(ErrorRouter.class.getName());
    
    private final List<ErrorObserver> observers = new CopyOnWriteArrayList<>();
    private final List<ErrorRecoveryStrategy> recoveryStrategies = new CopyOnWriteArrayList<>();
    private final Map<String, ErrorStatistics> errorStats = new ConcurrentHashMap<>();
    private final AtomicLong totalErrorsHandled = new AtomicLong(0);
    private final AtomicLong totalRecoveriesAttempted = new AtomicLong(0);
    private final AtomicLong totalRecoveriesSucceeded = new AtomicLong(0);
    
    /**
     * Interface for observing errors routed through the system.
     */
    public interface ErrorObserver {
        /**
         * Called when an error is routed through the system.
         * 
         * @param error The error that occurred
         * @param context Additional context information
         */
        void onErrorOccurred(NetworkError error, Map<String, Object> context);
        
        /**
         * Called when error recovery is attempted.
         * 
         * @param error The error being recovered from
         * @param strategy The recovery strategy being used
         * @param context Recovery context
         */
        default void onRecoveryAttempted(NetworkError error, ErrorRecoveryStrategy strategy, RecoveryContext context) {
            // Default implementation does nothing
        }
        
        /**
         * Called when error recovery completes.
         * 
         * @param error The error that was recovered from
         * @param strategy The recovery strategy that was used
         * @param result The recovery result
         * @param context Recovery context
         */
        default void onRecoveryCompleted(NetworkError error, ErrorRecoveryStrategy strategy, 
                                       RecoveryResult result, RecoveryContext context) {
            // Default implementation does nothing
        }
    }
    
    /**
     * Statistics for a specific error type.
     */
    public static class ErrorStatistics {
        private final String errorType;
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong recoveryAttempts = new AtomicLong(0);
        private final AtomicLong recoverySuccesses = new AtomicLong(0);
        private volatile Instant firstOccurrence;
        private volatile Instant lastOccurrence;
        private volatile NetworkError.Severity highestSeverity = NetworkError.Severity.INFO;
        
        public ErrorStatistics(String errorType) {
            this.errorType = errorType;
        }
        
        public String getErrorType() { return errorType; }
        public long getCount() { return count.get(); }
        public long getRecoveryAttempts() { return recoveryAttempts.get(); }
        public long getRecoverySuccesses() { return recoverySuccesses.get(); }
        public Instant getFirstOccurrence() { return firstOccurrence; }
        public Instant getLastOccurrence() { return lastOccurrence; }
        public NetworkError.Severity getHighestSeverity() { return highestSeverity; }
        
        public double getRecoverySuccessRate() {
            long attempts = recoveryAttempts.get();
            return attempts > 0 ? (double) recoverySuccesses.get() / attempts : 0.0;
        }
        
        void recordError(NetworkError error) {
            count.incrementAndGet();
            lastOccurrence = error.getTimestamp();
            if (firstOccurrence == null) {
                firstOccurrence = error.getTimestamp();
            }
            if (error.getSeverity().isMoreSevereThan(highestSeverity)) {
                highestSeverity = error.getSeverity();
            }
        }
        
        void recordRecoveryAttempt() {
            recoveryAttempts.incrementAndGet();
        }
        
        void recordRecoverySuccess() {
            recoverySuccesses.incrementAndGet();
        }
    }
    
    /**
     * Routes an error through the system, notifying observers and attempting recovery.
     * 
     * @param error The error to route
     * @return CompletableFuture containing the recovery result, if recovery was attempted
     */
    public CompletableFuture<Optional<RecoveryResult>> routeError(NetworkError error) {
        return routeError(error, Collections.emptyMap(), null);
    }
    
    /**
     * Routes an error through the system with context.
     * 
     * @param error The error to route
     * @param context Additional context information
     * @return CompletableFuture containing the recovery result, if recovery was attempted
     */
    public CompletableFuture<Optional<RecoveryResult>> routeError(NetworkError error, Map<String, Object> context) {
        return routeError(error, context, null);
    }
    
    /**
     * Routes an error through the system with context and recovery context.
     * 
     * @param error The error to route
     * @param context Additional context information
     * @param recoveryContext Recovery context for recovery strategies
     * @return CompletableFuture containing the recovery result, if recovery was attempted
     */
    public CompletableFuture<Optional<RecoveryResult>> routeError(NetworkError error, 
                                                                Map<String, Object> context,
                                                                RecoveryContext recoveryContext) {
        totalErrorsHandled.incrementAndGet();
        
        // Update statistics
        updateStatistics(error);
        
        // Log the error
        logError(error, context);
        
        // Notify observers
        notifyObservers(error, context);
        
        // Attempt recovery if applicable
        if (error.isRecoverable() && !recoveryStrategies.isEmpty()) {
            return attemptRecovery(error, recoveryContext != null ? recoveryContext : createDefaultRecoveryContext())
                .thenApply(Optional::of);
        } else {
            logger.log(Level.INFO, "Error {0} is not recoverable or no recovery strategies available", 
                      error.getErrorCode());
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }
    
    /**
     * Routes an exception by first classifying it into a NetworkError.
     * 
     * @param exception The exception to route
     * @return CompletableFuture containing the recovery result, if recovery was attempted
     */
    public CompletableFuture<Optional<RecoveryResult>> routeException(Throwable exception) {
        NetworkError error = ErrorClassifier.classify(exception);
        return routeError(error);
    }
    
    /**
     * Routes an exception with context.
     * 
     * @param exception The exception to route
     * @param context Additional context information
     * @return CompletableFuture containing the recovery result, if recovery was attempted
     */
    public CompletableFuture<Optional<RecoveryResult>> routeException(Throwable exception, Map<String, Object> context) {
        NetworkError error = ErrorClassifier.classify(exception);
        return routeError(error, context);
    }
    
    /**
     * Adds an error observer.
     * 
     * @param observer The observer to add
     */
    public void addObserver(ErrorObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            logger.log(Level.INFO, "Added error observer: {0}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Removes an error observer.
     * 
     * @param observer The observer to remove
     */
    public void removeObserver(ErrorObserver observer) {
        if (observers.remove(observer)) {
            logger.log(Level.INFO, "Removed error observer: {0}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Adds a recovery strategy.
     * 
     * @param strategy The recovery strategy to add
     */
    public void addRecoveryStrategy(ErrorRecoveryStrategy strategy) {
        if (strategy != null && !recoveryStrategies.contains(strategy)) {
            // Insert in priority order (highest priority first)
            int insertIndex = 0;
            for (int i = 0; i < recoveryStrategies.size(); i++) {
                if (strategy.getPriority() > recoveryStrategies.get(i).getPriority()) {
                    break;
                }
                insertIndex++;
            }
            recoveryStrategies.add(insertIndex, strategy);
            logger.log(Level.INFO, "Added recovery strategy: {0} with priority {1}", 
                      new Object[]{strategy.getClass().getSimpleName(), strategy.getPriority()});
        }
    }
    
    /**
     * Removes a recovery strategy.
     * 
     * @param strategy The recovery strategy to remove
     */
    public void removeRecoveryStrategy(ErrorRecoveryStrategy strategy) {
        if (recoveryStrategies.remove(strategy)) {
            logger.log(Level.INFO, "Removed recovery strategy: {0}", strategy.getClass().getSimpleName());
        }
    }
    
    /**
     * Gets all current error statistics.
     * 
     * @return Unmodifiable map of error type to statistics
     */
    public Map<String, ErrorStatistics> getErrorStatistics() {
        return Collections.unmodifiableMap(errorStats);
    }
    
    /**
     * Gets statistics for a specific error type.
     * 
     * @param errorType The error type (class simple name)
     * @return Optional containing statistics if available
     */
    public Optional<ErrorStatistics> getStatistics(String errorType) {
        return Optional.ofNullable(errorStats.get(errorType));
    }
    
    /**
     * Gets overall system statistics.
     * 
     * @return Map containing overall statistics
     */
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrorsHandled", totalErrorsHandled.get());
        stats.put("totalRecoveriesAttempted", totalRecoveriesAttempted.get());
        stats.put("totalRecoveriesSucceeded", totalRecoveriesSucceeded.get());
        stats.put("overallRecoverySuccessRate", 
                 totalRecoveriesAttempted.get() > 0 ? 
                 (double) totalRecoveriesSucceeded.get() / totalRecoveriesAttempted.get() : 0.0);
        stats.put("uniqueErrorTypes", errorStats.size());
        stats.put("registeredObservers", observers.size());
        stats.put("registeredStrategies", recoveryStrategies.size());
        return Collections.unmodifiableMap(stats);
    }
    
    /**
     * Clears all error statistics.
     */
    public void clearStatistics() {
        errorStats.clear();
        totalErrorsHandled.set(0);
        totalRecoveriesAttempted.set(0);
        totalRecoveriesSucceeded.set(0);
        logger.log(Level.INFO, "Cleared all error statistics");
    }
    
    private void updateStatistics(NetworkError error) {
        String errorType = error.getClass().getSimpleName();
        ErrorStatistics stats = errorStats.computeIfAbsent(errorType, ErrorStatistics::new);
        stats.recordError(error);
    }
    
    private void logError(NetworkError error, Map<String, Object> context) {
        Level logLevel = severityToLogLevel(error.getSeverity());
        if (logger.isLoggable(logLevel)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error routed: ").append(error.getTechnicalMessage());
            if (context != null && !context.isEmpty()) {
                sb.append(" | Context: ").append(context);
            }
            logger.log(logLevel, sb.toString());
        }
    }
    
    private Level severityToLogLevel(NetworkError.Severity severity) {
        switch (severity) {
            case INFO: return Level.INFO;
            case WARN: return Level.WARNING;
            case ERROR: return Level.SEVERE;
            case CRITICAL: return Level.SEVERE;
            default: return Level.INFO;
        }
    }
    
    private void notifyObservers(NetworkError error, Map<String, Object> context) {
        for (ErrorObserver observer : observers) {
            try {
                observer.onErrorOccurred(error, context);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error observer threw exception", e);
            }
        }
    }
    
    private CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext recoveryContext) {
        totalRecoveriesAttempted.incrementAndGet();
        
        // Find the best recovery strategy
        Optional<ErrorRecoveryStrategy> strategy = findBestRecoveryStrategy(error);
        if (!strategy.isPresent()) {
            logger.log(Level.INFO, "No suitable recovery strategy found for error {0}", error.getErrorCode());
            return CompletableFuture.completedFuture(
                RecoveryResult.notApplicable("No suitable recovery strategy available")
            );
        }
        
        ErrorRecoveryStrategy selectedStrategy = strategy.get();
        logger.log(Level.INFO, "Attempting recovery for error {0} using strategy {1}", 
                  new Object[]{error.getErrorCode(), selectedStrategy.getClass().getSimpleName()});
        
        // Notify observers of recovery attempt
        for (ErrorObserver observer : observers) {
            try {
                observer.onRecoveryAttempted(error, selectedStrategy, recoveryContext);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error observer threw exception during recovery attempt notification", e);
            }
        }
        
        // Update statistics
        String errorType = error.getClass().getSimpleName();
        ErrorStatistics stats = errorStats.get(errorType);
        if (stats != null) {
            stats.recordRecoveryAttempt();
        }
        
        return selectedStrategy.attemptRecovery(error, recoveryContext)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.log(Level.SEVERE, "Recovery strategy threw exception", throwable);
                    result = RecoveryResult.failure("Recovery strategy failed with exception", 0, throwable);
                }
                
                // Update statistics on success
                if (result.isSuccess() && stats != null) {
                    stats.recordRecoverySuccess();
                    totalRecoveriesSucceeded.incrementAndGet();
                }
                
                // Notify strategy of completion
                try {
                    if (result.isSuccess()) {
                        selectedStrategy.onRecoverySucceeded(error, recoveryContext, result);
                    } else {
                        selectedStrategy.onRecoveryFailed(error, recoveryContext, result);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Recovery strategy threw exception during completion callback", e);
                }
                
                // Notify observers of completion
                for (ErrorObserver observer : observers) {
                    try {
                        observer.onRecoveryCompleted(error, selectedStrategy, result, recoveryContext);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error observer threw exception during recovery completion notification", e);
                    }
                }
            });
    }
    
    private Optional<ErrorRecoveryStrategy> findBestRecoveryStrategy(NetworkError error) {
        return recoveryStrategies.stream()
            .filter(strategy -> strategy.canRecover(error))
            .findFirst(); // List is already sorted by priority
    }
    
    private RecoveryContext createDefaultRecoveryContext() {
        return new RecoveryContext(null, null, null);
    }
}