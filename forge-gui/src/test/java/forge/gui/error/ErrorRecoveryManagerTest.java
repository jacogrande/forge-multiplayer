package forge.gui.error;

import forge.error.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive test suite for ErrorRecoveryManager.
 * Tests automatic error handling, user notification integration, and recovery strategy coordination.
 * Follows TDD principles with tests defining acceptance criteria.
 */
public class ErrorRecoveryManagerTest {
    
    private ErrorRecoveryManager recoveryManager;
    private TestErrorObserver testObserver;
    private TestUserNotificationManager testNotifier;
    private TestRecoveryStrategy testStrategy;
    private ErrorRouter errorRouter;
    
    @BeforeMethod
    public void setUp() {
        errorRouter = new ErrorRouter();
        testObserver = new TestErrorObserver();
        testNotifier = new TestUserNotificationManager();
        testStrategy = new TestRecoveryStrategy();
        
        recoveryManager = new ErrorRecoveryManager(errorRouter, testNotifier);
        errorRouter.addObserver(testObserver);
        errorRouter.addRecoveryStrategy(testStrategy);
    }
    
    @AfterMethod
    public void tearDown() {
        if (recoveryManager != null) {
            recoveryManager.shutdown();
        }
    }
    
    @Test
    public void testAutomaticErrorHandling() throws Exception {
        // Acceptance Criteria: ErrorRecoveryManager automatically handles common errors
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
        
        // Configure test strategy to succeed
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(5, TimeUnit.SECONDS);
        
        // Verify automatic recovery was attempted and succeeded
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        assertEquals(1, testStrategy.getRecoveryAttempts());
        
        // Verify user was notified of recovery progress
        assertTrue(testNotifier.wasNotified());
        assertTrue(testNotifier.getNotifications().stream()
            .anyMatch(n -> n.contains("Recovery successful")));
    }
    
    @Test
    public void testProgressiveRecoveryFallback() throws Exception {
        // Acceptance Criteria: System tries multiple recovery strategies in priority order
        TestRecoveryStrategy primaryStrategy = new TestRecoveryStrategy("Primary", 80);
        TestRecoveryStrategy fallbackStrategy = new TestRecoveryStrategy("Fallback", 60);
        
        // Primary strategy fails, fallback succeeds
        primaryStrategy.setShouldSucceed(false);
        primaryStrategy.setCanRecoverResult(true);
        fallbackStrategy.setShouldSucceed(true);
        fallbackStrategy.setCanRecoverResult(true);
        
        errorRouter.addRecoveryStrategy(primaryStrategy);
        errorRouter.addRecoveryStrategy(fallbackStrategy);
        
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
        
        // Verify fallback was used after primary failed
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        
        // Should have attempted primary first, then fallback
        assertEquals(1, primaryStrategy.getRecoveryAttempts());
        assertEquals(1, fallbackStrategy.getRecoveryAttempts());
        
        // User should be notified of strategy progression
        List<String> notifications = testNotifier.getNotifications();
        assertTrue(notifications.stream().anyMatch(n -> n.contains("Trying alternative recovery")));
    }
    
    @Test
    public void testUserChoiceIntegration() throws Exception {
        // Acceptance Criteria: User can choose recovery options for certain errors
        SecurityError error = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "testuser");
        
        // Set up user choice scenario
        testNotifier.setUserChoice("RETRY_WITH_CREDENTIALS");
        recoveryManager.enableUserChoiceForErrorType(SecurityError.class);
        
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        
        // Wait for user choice prompt
        Thread.sleep(100);
        
        // Verify user was prompted for choice
        assertTrue(testNotifier.wasUserChoiceRequested());
        assertEquals("How would you like to handle this authentication error?", 
                    testNotifier.getLastUserChoicePrompt());
        
        // Complete the recovery
        Optional<RecoveryResult> result = future.get(5, TimeUnit.SECONDS);
        assertTrue(result.isPresent());
    }
    
    @Test
    public void testRecoveryTimeoutHandling() throws Exception {
        // Acceptance Criteria: Recovery operations timeout appropriately
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
        
        // Configure strategy to take too long
        testStrategy.setRecoveryDelay(10000); // 10 seconds
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        
        // Set recovery timeout to 2 seconds
        recoveryManager.setRecoveryTimeout(2000);
        
        long startTime = System.currentTimeMillis();
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(5, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        // Should timeout after ~2 seconds
        assertTrue(duration < 3000);
        assertTrue(result.isPresent());
        assertTrue(result.get().isFailure());
        assertTrue(result.get().getMessage().contains("timeout"));
        
        // User should be notified of timeout
        assertTrue(testNotifier.getNotifications().stream()
            .anyMatch(n -> n.contains("Recovery timed out")));
    }
    
    @Test
    public void testRecoveryMetricsTracking() throws Exception {
        // Acceptance Criteria: System tracks recovery performance metrics
        ConnectionTimeoutError error1 = new ConnectionTimeoutError("localhost", 7777, 5000);
        ConnectionTimeoutError error2 = new ConnectionTimeoutError("localhost", 7777, 5000);
        
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        testStrategy.setRecoveryDelay(100); // Quick recovery
        
        // Perform multiple recoveries
        recoveryManager.handleError(error1).get(5, TimeUnit.SECONDS);
        recoveryManager.handleError(error2).get(5, TimeUnit.SECONDS);
        
        // Check metrics
        ErrorRecoveryManager.RecoveryMetrics metrics = recoveryManager.getRecoveryMetrics();
        assertEquals(2, metrics.getTotalRecoveries());
        assertEquals(2, metrics.getSuccessfulRecoveries());
        assertEquals(100.0, metrics.getSuccessRate(), 0.1);
        assertTrue(metrics.getAverageRecoveryTime() < 1000); // Should be quick
    }
    
    @Test
    public void testConcurrentErrorHandling() throws Exception {
        // Acceptance Criteria: Multiple errors can be handled concurrently
        int errorCount = 5;
        CountDownLatch latch = new CountDownLatch(errorCount);
        List<CompletableFuture<Optional<RecoveryResult>>> futures = new ArrayList<>();
        
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        testStrategy.setRecoveryDelay(200);
        
        // Submit multiple errors concurrently
        for (int i = 0; i < errorCount; i++) {
            ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777 + i, 5000);
            CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
            future.thenRun(latch::countDown);
            futures.add(future);
        }
        
        // Wait for all to complete
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        
        // Verify all succeeded
        for (CompletableFuture<Optional<RecoveryResult>> future : futures) {
            Optional<RecoveryResult> result = future.get();
            assertTrue(result.isPresent());
            assertTrue(result.get().isSuccess());
        }
        
        // Verify concurrent processing
        assertEquals(errorCount, testStrategy.getRecoveryAttempts());
    }
    
    @Test
    public void testGracefulDegradation() throws Exception {
        // Acceptance Criteria: System degrades gracefully when recovery is not possible
        GameStateError unrecoverableError = new StateCorruptionError(StateCorruptionError.CorruptionType.DATA_STRUCTURE, "Unrecoverable corruption", "test_operation");
        
        // Configure all strategies to reject this error
        testStrategy.setCanRecoverResult(false);
        
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(unrecoverableError);
        Optional<RecoveryResult> result = future.get(5, TimeUnit.SECONDS);
        
        // Should return empty result for unrecoverable errors
        assertFalse(result.isPresent());
        
        // User should be notified that error cannot be recovered
        assertTrue(testNotifier.getNotifications().stream()
            .anyMatch(n -> n.contains("cannot be automatically recovered")));
    }
    
    @Test
    public void testErrorReportingMechanism() throws Exception {
        // Acceptance Criteria: Comprehensive error reporting for debugging
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000,
            new RuntimeException("Underlying cause"));
        
        testStrategy.setShouldSucceed(false);
        testStrategy.setCanRecoverResult(true);
        
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        future.get(5, TimeUnit.SECONDS);
        
        // Verify error report was generated
        ErrorRecoveryManager.ErrorReport report = recoveryManager.getLastErrorReport();
        assertNotNull(report);
        assertEquals(error.getErrorCode(), report.getErrorCode());
        assertNotNull(report.getStackTrace());
        assertNotNull(report.getSystemInfo());
        assertTrue(report.getRecoveryAttempts() > 0);
    }
    
    @Test
    public void testRecoveryPerformanceRequirement() throws Exception {
        // Acceptance Criteria: Recovery completes within 5 seconds for common errors
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
        
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        testStrategy.setRecoveryDelay(1000); // 1 second recovery time
        
        long startTime = System.currentTimeMillis();
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Should complete well within 5 seconds
        assertTrue(totalTime < 5000);
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        
        // Recovery time should be tracked accurately
        assertTrue(result.get().getDurationMs() >= 1000);
        assertTrue(result.get().getDurationMs() < 2000);
    }
    
    /**
     * Test implementation of ErrorObserver for monitoring error routing.
     */
    private static class TestErrorObserver implements ErrorRouter.ErrorObserver {
        private final List<NetworkError> observedErrors = new ArrayList<>();
        private final AtomicInteger recoveryAttempts = new AtomicInteger(0);
        private final AtomicInteger recoveryCompletions = new AtomicInteger(0);
        
        @Override
        public void onErrorOccurred(NetworkError error, Map<String, Object> context) {
            observedErrors.add(error);
        }
        
        @Override
        public void onRecoveryAttempted(NetworkError error, ErrorRecoveryStrategy strategy, RecoveryContext context) {
            recoveryAttempts.incrementAndGet();
        }
        
        @Override
        public void onRecoveryCompleted(NetworkError error, ErrorRecoveryStrategy strategy, 
                                       RecoveryResult result, RecoveryContext context) {
            recoveryCompletions.incrementAndGet();
        }
        
        public List<NetworkError> getObservedErrors() { return observedErrors; }
        public int getRecoveryAttempts() { return recoveryAttempts.get(); }
        public int getRecoveryCompletions() { return recoveryCompletions.get(); }
    }
    
    /**
     * Test implementation of user notification system.
     */
    private static class TestUserNotificationManager implements ErrorRecoveryManager.UserNotificationManager {
        private final List<String> notifications = new ArrayList<>();
        private final AtomicBoolean wasNotified = new AtomicBoolean(false);
        private final AtomicBoolean userChoiceRequested = new AtomicBoolean(false);
        private final AtomicReference<String> lastUserChoicePrompt = new AtomicReference<>();
        private String userChoice = "DEFAULT";
        
        public void notifyUser(String message) {
            notifications.add(message);
            wasNotified.set(true);
        }
        
        public String requestUserChoice(String prompt, List<String> options) {
            userChoiceRequested.set(true);
            lastUserChoicePrompt.set(prompt);
            return userChoice;
        }
        
        public boolean wasNotified() { return wasNotified.get(); }
        public List<String> getNotifications() { return new ArrayList<>(notifications); }
        public boolean wasUserChoiceRequested() { return userChoiceRequested.get(); }
        public String getLastUserChoicePrompt() { return lastUserChoicePrompt.get(); }
        public void setUserChoice(String choice) { this.userChoice = choice; }
    }
    
    /**
     * Test implementation of recovery strategy.
     */
    private static class TestRecoveryStrategy implements ErrorRecoveryStrategy {
        private final String name;
        private final int priority;
        private boolean shouldSucceed = true;
        private boolean canRecoverResult = true;
        private long recoveryDelay = 0;
        private final AtomicInteger recoveryAttempts = new AtomicInteger(0);
        
        public TestRecoveryStrategy() {
            this("Default", 50);
        }
        
        public TestRecoveryStrategy(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        @Override
        public boolean canRecover(NetworkError error) {
            return canRecoverResult;
        }
        
        @Override
        public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
            recoveryAttempts.incrementAndGet();
            
            return CompletableFuture.supplyAsync(() -> {
                if (recoveryDelay > 0) {
                    try {
                        Thread.sleep(recoveryDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return RecoveryResult.failure("Interrupted", recoveryDelay, e);
                    }
                }
                
                if (shouldSucceed) {
                    return RecoveryResult.success("Recovery successful", recoveryDelay);
                } else {
                    return RecoveryResult.failure("Recovery failed", recoveryDelay);
                }
            });
        }
        
        @Override
        public NetworkError.RecoveryStrategy getRecoveryType() {
            return NetworkError.RecoveryStrategy.RETRY;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        public void setShouldSucceed(boolean shouldSucceed) { this.shouldSucceed = shouldSucceed; }
        public void setCanRecoverResult(boolean canRecover) { this.canRecoverResult = canRecover; }
        public void setRecoveryDelay(long delay) { this.recoveryDelay = delay; }
        public int getRecoveryAttempts() { return recoveryAttempts.get(); }
        public String getName() { return name; }
    }
}