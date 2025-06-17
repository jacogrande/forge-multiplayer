package forge.gui.error;

import forge.error.*;
import forge.gui.network.NetworkEventLogger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Advanced error recovery manager that provides automatic error handling,
 * user notification integration, and comprehensive recovery strategy coordination.
 * 
 * Features:
 * - Progressive recovery with multiple fallback strategies
 * - User choice integration for complex error scenarios
 * - Recovery performance monitoring and timeout handling
 * - Concurrent error handling with resource management
 * - Comprehensive error reporting and analytics
 * 
 * This class integrates with the existing ErrorRouter infrastructure while
 * adding advanced recovery coordination and user experience enhancements.
 */
public class ErrorRecoveryManager implements ErrorRouter.ErrorObserver {
    
    private static final NetworkEventLogger logger = NetworkEventLogger.forComponent("ErrorRecoveryManager");
    
    private final ErrorRouter errorRouter;
    private final UserNotificationManager notificationManager;
    private final ExecutorService recoveryExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    
    // Configuration
    private long defaultRecoveryTimeout = 5000; // 5 seconds
    private int maxConcurrentRecoveries = 10;
    private boolean enableUserChoiceForAll = false;
    private final Set<Class<? extends NetworkError>> userChoiceEnabledTypes = ConcurrentHashMap.newKeySet();
    
    // State tracking
    private final Semaphore concurrencyLimiter;
    private final Map<String, RecoverySession> activeRecoveries = new ConcurrentHashMap<>();
    private final AtomicReference<ErrorReport> lastErrorReport = new AtomicReference<>();
    
    // Metrics
    private final AtomicLong totalRecoveries = new AtomicLong(0);
    private final AtomicLong successfulRecoveries = new AtomicLong(0);
    private final AtomicLong totalRecoveryTime = new AtomicLong(0);
    private final AtomicInteger currentConcurrentRecoveries = new AtomicInteger(0);
    
    /**
     * Creates a new ErrorRecoveryManager.
     * 
     * @param errorRouter The ErrorRouter to integrate with
     * @param notificationManager The user notification manager
     */
    public ErrorRecoveryManager(ErrorRouter errorRouter, UserNotificationManager notificationManager) {
        this.errorRouter = errorRouter;
        this.notificationManager = notificationManager;
        this.concurrencyLimiter = new Semaphore(maxConcurrentRecoveries);
        
        // Create thread pools with proper naming
        this.recoveryExecutor = Executors.newFixedThreadPool(maxConcurrentRecoveries, 
            r -> {
                Thread t = new Thread(r, "ErrorRecovery-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
        
        this.timeoutExecutor = Executors.newScheduledThreadPool(2,
            r -> {
                Thread t = new Thread(r, "RecoveryTimeout-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
        
        // Register as observer
        errorRouter.addObserver(this);
        
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "ErrorRecoveryManager initialized")
               .withField("maxConcurrentRecoveries", maxConcurrentRecoveries)
               .withField("defaultTimeout", defaultRecoveryTimeout)
               .log();
    }
    
    /**
     * Handles an error with comprehensive recovery management.
     * 
     * @param error The error to handle
     * @return CompletableFuture containing recovery result if applicable
     */
    public CompletableFuture<Optional<RecoveryResult>> handleError(NetworkError error) {
        return handleError(error, Collections.emptyMap(), null);
    }
    
    /**
     * Handles an error with context and recovery options.
     * 
     * @param error The error to handle
     * @param context Additional context information
     * @param recoveryContext Recovery context for strategies
     * @return CompletableFuture containing recovery result if applicable
     */
    public CompletableFuture<Optional<RecoveryResult>> handleError(NetworkError error, 
                                                                   Map<String, Object> context,
                                                                   RecoveryContext recoveryContext) {
        String correlationId = logger.startCorrelation();
        
        try {
            logger.logEvent(NetworkEventLogger.EventType.ERROR,
                           severityToEventSeverity(error.getSeverity()),
                           "Handling error: {}", error.getTechnicalMessage())
                   .withField("errorType", error.getClass().getSimpleName())
                   .withField("errorCode", error.getErrorCode())
                   .withField("recoverable", error.isRecoverable())
                   .log();
            
            // Check if recovery is applicable
            if (!error.isRecoverable()) {
                notificationManager.notifyUser("Error occurred that cannot be automatically recovered: " + 
                                             error.getUserMessage());
                return CompletableFuture.completedFuture(Optional.empty());
            }
            
            // Check concurrency limits
            if (!concurrencyLimiter.tryAcquire()) {
                logger.logEvent(NetworkEventLogger.EventType.ERROR,
                               NetworkEventLogger.Severity.WARN,
                               "Recovery rejected due to concurrency limit")
                       .withField("activeConcurrentRecoveries", currentConcurrentRecoveries.get())
                       .log();
                               
                notificationManager.notifyUser("System is currently handling multiple errors. Please wait...");
                return CompletableFuture.completedFuture(Optional.empty());
            }
            
            // Create recovery session
            RecoverySession session = new RecoverySession(error, context, recoveryContext, correlationId);
            activeRecoveries.put(correlationId, session);
            currentConcurrentRecoveries.incrementAndGet();
            
            return executeRecoveryWithTimeout(session);
            
        } finally {
            logger.endCorrelation(correlationId);
        }
    }
    
    /**
     * Enables user choice integration for specific error types.
     * 
     * @param errorType The error type to enable user choice for
     */
    public void enableUserChoiceForErrorType(Class<? extends NetworkError> errorType) {
        userChoiceEnabledTypes.add(errorType);
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "Enabled user choice for error type: {}", errorType.getSimpleName())
               .log();
    }
    
    /**
     * Sets the recovery timeout for all operations.
     * 
     * @param timeoutMs Timeout in milliseconds
     */
    public void setRecoveryTimeout(long timeoutMs) {
        this.defaultRecoveryTimeout = timeoutMs;
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "Recovery timeout updated to {}ms", timeoutMs)
               .log();
    }
    
    /**
     * Gets current recovery metrics.
     * 
     * @return Recovery metrics
     */
    public RecoveryMetrics getRecoveryMetrics() {
        return new RecoveryMetrics(
            totalRecoveries.get(),
            successfulRecoveries.get(),
            totalRecoveries.get() > 0 ? (double) successfulRecoveries.get() / totalRecoveries.get() * 100.0 : 0.0,
            totalRecoveries.get() > 0 ? totalRecoveryTime.get() / totalRecoveries.get() : 0,
            currentConcurrentRecoveries.get()
        );
    }
    
    /**
     * Gets the last error report for debugging.
     * 
     * @return Last error report or null if none
     */
    public ErrorReport getLastErrorReport() {
        return lastErrorReport.get();
    }
    
    /**
     * Shuts down the recovery manager and cleans up resources.
     */
    public void shutdown() {
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "Shutting down ErrorRecoveryManager")
               .withField("activeRecoveries", activeRecoveries.size())
               .log();
        
        // Cancel active recoveries
        for (RecoverySession session : activeRecoveries.values()) {
            if (session.future != null && !session.future.isDone()) {
                session.future.cancel(true);
            }
        }
        activeRecoveries.clear();
        
        // Shutdown executors
        recoveryExecutor.shutdown();
        timeoutExecutor.shutdown();
        
        try {
            if (!recoveryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                recoveryExecutor.shutdownNow();
            }
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recoveryExecutor.shutdownNow();
            timeoutExecutor.shutdownNow();
        }
    }
    
    // ErrorRouter.ErrorObserver implementation
    
    @Override
    public void onErrorOccurred(NetworkError error, Map<String, Object> context) {
        // We handle errors through handleError method, so we don't need to do anything here
        // This method is called by ErrorRouter when routing errors
    }
    
    @Override
    public void onRecoveryAttempted(NetworkError error, ErrorRecoveryStrategy strategy, RecoveryContext context) {
        notificationManager.notifyUser("Attempting to recover from " + error.getUserMessage() + 
                                     " using " + strategy.getClass().getSimpleName());
    }
    
    @Override
    public void onRecoveryCompleted(NetworkError error, ErrorRecoveryStrategy strategy, 
                                   RecoveryResult result, RecoveryContext context) {
        if (result.isSuccess()) {
            notificationManager.notifyUser("Recovery successful: " + result.getMessage());
        } else if (result.canRetry()) {
            notificationManager.notifyUser("Trying alternative recovery method...");
        } else {
            notificationManager.notifyUser("Recovery failed: " + result.getMessage());
        }
    }
    
    private CompletableFuture<Optional<RecoveryResult>> executeRecoveryWithTimeout(RecoverySession session) {
        totalRecoveries.incrementAndGet();
        
        CompletableFuture<Optional<RecoveryResult>> recoveryFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return performRecovery(session);
            } catch (Exception e) {
                logger.logError("recovery_execution", e);
                return Optional.of(RecoveryResult.failure("Recovery execution failed", 0, e));
            }
        }, recoveryExecutor);
        
        // Setup timeout
        ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(() -> {
            if (!recoveryFuture.isDone()) {
                recoveryFuture.cancel(true);
                notificationManager.notifyUser("Recovery timed out after " + defaultRecoveryTimeout + "ms");
                logger.logEvent(NetworkEventLogger.EventType.ERROR,
                               NetworkEventLogger.Severity.WARN,
                               "Recovery timed out")
                       .withField("timeout", defaultRecoveryTimeout)
                       .withField("errorType", session.error.getClass().getSimpleName())
                       .log();
            }
        }, defaultRecoveryTimeout, TimeUnit.MILLISECONDS);
        
        session.future = recoveryFuture;
        session.timeoutFuture = timeoutFuture;
        
        return recoveryFuture.whenComplete((result, throwable) -> {
            // Cleanup
            timeoutFuture.cancel(false);
            activeRecoveries.remove(session.correlationId);
            concurrencyLimiter.release();
            currentConcurrentRecoveries.decrementAndGet();
            
            // Update metrics
            long sessionDuration = System.currentTimeMillis() - session.startTime;
            totalRecoveryTime.addAndGet(sessionDuration);
            
            if (result != null && result.isPresent() && result.get().isSuccess()) {
                successfulRecoveries.incrementAndGet();
            }
            
            // Generate error report
            generateErrorReport(session, result, throwable);
            
            if (throwable != null && !recoveryFuture.isCancelled()) {
                logger.logError("recovery_completion", throwable);
            }
        });
    }
    
    private Optional<RecoveryResult> performRecovery(RecoverySession session) {
        String correlationId = session.correlationId;
        
        try {
            logger.startCorrelation(correlationId);
            
            // Check if user choice is required
            if (shouldRequestUserChoice(session.error)) {
                String userChoice = requestUserChoice(session.error);
                session.context.put("userChoice", userChoice);
            }
            
            // Route through ErrorRouter for standard recovery processing
            CompletableFuture<Optional<RecoveryResult>> routerResult = 
                errorRouter.routeError(session.error, session.context, session.recoveryContext);
                
            return routerResult.get(defaultRecoveryTimeout - 1000, TimeUnit.MILLISECONDS); // Leave buffer for timeout
            
        } catch (TimeoutException e) {
            return Optional.of(RecoveryResult.failure("Recovery timed out", defaultRecoveryTimeout, e));
        } catch (Exception e) {
            logger.logError("recovery_processing", e);
            return Optional.of(RecoveryResult.failure("Recovery processing failed", 0, e));
        } finally {
            logger.endCorrelation(correlationId);
        }
    }
    
    private boolean shouldRequestUserChoice(NetworkError error) {
        return enableUserChoiceForAll || 
               userChoiceEnabledTypes.stream().anyMatch(type -> type.isInstance(error));
    }
    
    private String requestUserChoice(NetworkError error) {
        List<String> options = Arrays.asList("RETRY", "SKIP", "ABORT");
        String prompt = "How would you like to handle this " + 
                       error.getClass().getSimpleName().replace("Error", "").toLowerCase() + " error?";
        
        return notificationManager.requestUserChoice(prompt, options);
    }
    
    private void generateErrorReport(RecoverySession session, 
                                    Optional<RecoveryResult> result, 
                                    Throwable throwable) {
        try {
            ErrorReport report = new ErrorReport(
                session.error.getErrorCode(),
                session.error.getClass().getSimpleName(),
                session.error.getTechnicalMessage(),
                session.error.getUserMessage(),
                getStackTrace(session.error),
                System.getProperty("java.version") + " / " + System.getProperty("os.name"),
                session.context,
                result != null && result.isPresent() ? 1 : 0,
                result != null && result.isPresent() && result.get().isSuccess(),
                System.currentTimeMillis() - session.startTime,
                throwable != null ? throwable.getMessage() : null
            );
            
            lastErrorReport.set(report);
            
            logger.logEvent(NetworkEventLogger.EventType.ERROR,
                           NetworkEventLogger.Severity.DEBUG,
                           "Generated error report")
                   .withField("errorCode", report.getErrorCode())
                   .withField("recoverySuccess", report.isRecoverySuccessful())
                   .withField("recoveryDuration", report.getRecoveryDuration())
                   .log();
                   
        } catch (Exception e) {
            logger.logError("error_report_generation", e);
        }
    }
    
    private String getStackTrace(NetworkError error) {
        if (error.getCause() != null) {
            Throwable cause = error.getCause();
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            cause.printStackTrace(pw);
            return sw.toString();
        }
        return "No stack trace available";
    }
    
    private NetworkEventLogger.Severity severityToEventSeverity(NetworkError.Severity severity) {
        switch (severity) {
            case INFO: return NetworkEventLogger.Severity.INFO;
            case WARN: return NetworkEventLogger.Severity.WARN;
            case ERROR: return NetworkEventLogger.Severity.ERROR;
            case CRITICAL: return NetworkEventLogger.Severity.CRITICAL;
            default: return NetworkEventLogger.Severity.INFO;
        }
    }
    
    /**
     * Recovery session tracking.
     */
    private static class RecoverySession {
        final NetworkError error;
        final Map<String, Object> context;
        final RecoveryContext recoveryContext;
        final String correlationId;
        final long startTime;
        CompletableFuture<Optional<RecoveryResult>> future;
        ScheduledFuture<?> timeoutFuture;
        
        RecoverySession(NetworkError error, Map<String, Object> context, 
                       RecoveryContext recoveryContext, String correlationId) {
            this.error = error;
            this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
            this.recoveryContext = recoveryContext;
            this.correlationId = correlationId;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Recovery performance metrics.
     */
    public static class RecoveryMetrics {
        private final long totalRecoveries;
        private final long successfulRecoveries;
        private final double successRate;
        private final long averageRecoveryTime;
        private final int currentConcurrentRecoveries;
        
        RecoveryMetrics(long totalRecoveries, long successfulRecoveries, double successRate,
                       long averageRecoveryTime, int currentConcurrentRecoveries) {
            this.totalRecoveries = totalRecoveries;
            this.successfulRecoveries = successfulRecoveries;
            this.successRate = successRate;
            this.averageRecoveryTime = averageRecoveryTime;
            this.currentConcurrentRecoveries = currentConcurrentRecoveries;
        }
        
        public long getTotalRecoveries() { return totalRecoveries; }
        public long getSuccessfulRecoveries() { return successfulRecoveries; }
        public double getSuccessRate() { return successRate; }
        public long getAverageRecoveryTime() { return averageRecoveryTime; }
        public int getCurrentConcurrentRecoveries() { return currentConcurrentRecoveries; }
    }
    
    /**
     * Comprehensive error report for debugging.
     */
    public static class ErrorReport {
        private final String errorCode;
        private final String errorType;
        private final String technicalMessage;
        private final String userMessage;
        private final String stackTrace;
        private final String systemInfo;
        private final Map<String, Object> context;
        private final int recoveryAttempts;
        private final boolean recoverySuccessful;
        private final long recoveryDuration;
        private final String additionalInfo;
        private final Instant timestamp;
        
        ErrorReport(String errorCode, String errorType, String technicalMessage, String userMessage,
                   String stackTrace, String systemInfo, Map<String, Object> context,
                   int recoveryAttempts, boolean recoverySuccessful, long recoveryDuration,
                   String additionalInfo) {
            this.errorCode = errorCode;
            this.errorType = errorType;
            this.technicalMessage = technicalMessage;
            this.userMessage = userMessage;
            this.stackTrace = stackTrace;
            this.systemInfo = systemInfo;
            this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
            this.recoveryAttempts = recoveryAttempts;
            this.recoverySuccessful = recoverySuccessful;
            this.recoveryDuration = recoveryDuration;
            this.additionalInfo = additionalInfo;
            this.timestamp = Instant.now();
        }
        
        // Getters
        public String getErrorCode() { return errorCode; }
        public String getErrorType() { return errorType; }
        public String getTechnicalMessage() { return technicalMessage; }
        public String getUserMessage() { return userMessage; }
        public String getStackTrace() { return stackTrace; }
        public String getSystemInfo() { return systemInfo; }
        public Map<String, Object> getContext() { return Collections.unmodifiableMap(context); }
        public int getRecoveryAttempts() { return recoveryAttempts; }
        public boolean isRecoverySuccessful() { return recoverySuccessful; }
        public long getRecoveryDuration() { return recoveryDuration; }
        public String getAdditionalInfo() { return additionalInfo; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Interface for user notification management.
     * This will be implemented by platform-specific notification systems.
     */
    public interface UserNotificationManager {
        /**
         * Notifies the user with a message.
         * 
         * @param message The message to display
         */
        void notifyUser(String message);
        
        /**
         * Requests a choice from the user.
         * 
         * @param prompt The prompt to display
         * @param options Available options
         * @return The user's choice
         */
        String requestUserChoice(String prompt, List<String> options);
    }
}