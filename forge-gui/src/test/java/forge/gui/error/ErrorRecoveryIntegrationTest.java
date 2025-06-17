package forge.gui.error;

import forge.error.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration test for error recovery functionality.
 * Tests end-to-end error scenarios with recovery and user feedback.
 */
public class ErrorRecoveryIntegrationTest {
    
    private ErrorRouter errorRouter;
    private ErrorRecoveryManager recoveryManager;
    private TestNotificationManager notificationManager;
    private TestRecoveryStrategy testStrategy;
    
    @BeforeMethod
    public void setUp() {
        errorRouter = new ErrorRouter();
        notificationManager = new TestNotificationManager();
        testStrategy = new TestRecoveryStrategy();
        
        recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);
        errorRouter.addRecoveryStrategy(testStrategy);
    }
    
    @AfterMethod
    public void tearDown() {
        if (recoveryManager != null) {
            recoveryManager.shutdown();
        }
    }
    
    @Test
    public void testBasicErrorRecoveryFlow() throws Exception {
        // Test the basic error recovery workflow
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
        
        // Configure strategy to succeed
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        
        // Handle the error
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
        
        // Verify recovery succeeded
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        
        // Verify user was notified
        assertTrue(notificationManager.wasNotified());
        assertEquals(1, testStrategy.getRecoveryAttempts());
    }
    
    @Test
    public void testErrorRecoveryFailure() throws Exception {
        // Test error recovery failure scenario
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
        
        // Configure strategy to fail
        testStrategy.setShouldSucceed(false);
        testStrategy.setCanRecoverResult(true);
        
        // Handle the error
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
        
        // Verify recovery failed
        assertTrue(result.isPresent());
        assertFalse(result.get().isSuccess());
        
        // Verify user was notified of failure
        assertTrue(notificationManager.wasNotified());
        assertEquals(1, testStrategy.getRecoveryAttempts());
    }
    
    @Test
    public void testUnrecoverableError() throws Exception {
        // Test handling of unrecoverable errors
        StateCorruptionError error = new StateCorruptionError(
            StateCorruptionError.CorruptionType.DATA_STRUCTURE, 
            "Critical corruption", 
            "test_component");
        
        // Configure strategy to reject this error
        testStrategy.setCanRecoverResult(false);
        
        // Handle the error
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
        
        // Should return empty result for unrecoverable errors
        assertFalse(result.isPresent());
        
        // User should still be notified
        assertTrue(notificationManager.wasNotified());
        assertEquals(0, testStrategy.getRecoveryAttempts()); // No recovery attempted
    }
    
    @Test
    public void testErrorReporting() throws Exception {
        // Test error reporting functionality
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000,
            new RuntimeException("Network failure"));
        
        testStrategy.setShouldSucceed(false);
        testStrategy.setCanRecoverResult(true);
        
        // Handle the error
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        future.get(10, TimeUnit.SECONDS);
        
        // Verify error report was generated
        ErrorRecoveryManager.ErrorReport report = recoveryManager.getLastErrorReport();
        assertNotNull(report);
        assertEquals(error.getErrorCode(), report.getErrorCode());
        assertTrue(report.getStackTrace().contains("RuntimeException"));
        assertTrue(report.getRecoveryAttempts() > 0);
        assertFalse(report.isRecoverySuccessful());
    }
    
    @Test 
    public void testPerformanceMetrics() throws Exception {
        // Test recovery performance tracking
        ConnectionTimeoutError error1 = new ConnectionTimeoutError("localhost", 7777, 5000);
        ConnectionTimeoutError error2 = new ConnectionTimeoutError("localhost", 7778, 5000);
        
        testStrategy.setShouldSucceed(true);
        testStrategy.setCanRecoverResult(true);
        testStrategy.setRecoveryDelay(100);
        
        // Handle multiple errors
        recoveryManager.handleError(error1).get(10, TimeUnit.SECONDS);
        recoveryManager.handleError(error2).get(10, TimeUnit.SECONDS);
        
        // Check metrics
        ErrorRecoveryManager.RecoveryMetrics metrics = recoveryManager.getRecoveryMetrics();
        assertEquals(2, metrics.getTotalRecoveries());
        assertEquals(2, metrics.getSuccessfulRecoveries());
        assertEquals(100.0, metrics.getSuccessRate(), 0.1);
        assertTrue(metrics.getAverageRecoveryTime() >= 100); // Should be at least 100ms
    }
    
    /**
     * Simple test notification manager.
     */
    private static class TestNotificationManager implements ErrorRecoveryManager.UserNotificationManager {
        private final List<String> notifications = new ArrayList<>();
        private final AtomicBoolean wasNotified = new AtomicBoolean(false);
        private String defaultChoice = "DEFAULT";
        
        @Override
        public void notifyUser(String message) {
            notifications.add(message);
            wasNotified.set(true);
        }
        
        @Override
        public String requestUserChoice(String prompt, List<String> options) {
            return options.isEmpty() ? defaultChoice : options.get(0);
        }
        
        public boolean wasNotified() { return wasNotified.get(); }
        public List<String> getNotifications() { return new ArrayList<>(notifications); }
        public void setDefaultChoice(String choice) { this.defaultChoice = choice; }
    }
    
    /**
     * Simple test recovery strategy.
     */
    private static class TestRecoveryStrategy implements ErrorRecoveryStrategy {
        private boolean shouldSucceed = true;
        private boolean canRecoverResult = true;
        private long recoveryDelay = 0;
        private final AtomicInteger recoveryAttempts = new AtomicInteger(0);
        
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
            return 50;
        }
        
        public void setShouldSucceed(boolean shouldSucceed) { this.shouldSucceed = shouldSucceed; }
        public void setCanRecoverResult(boolean canRecover) { this.canRecoverResult = canRecover; }
        public void setRecoveryDelay(long delay) { this.recoveryDelay = delay; }
        public int getRecoveryAttempts() { return recoveryAttempts.get(); }
    }
}