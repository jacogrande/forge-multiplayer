package forge.error;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Unit tests for the ErrorRouter class and error routing functionality.
 */
public class ErrorRouterTest {
    
    private ErrorRouter errorRouter;
    private TestErrorObserver testObserver;
    private ConnectionTimeoutError testError;
    
    @BeforeMethod
    public void setUp() {
        errorRouter = new ErrorRouter();
        testObserver = new TestErrorObserver();
        testError = new ConnectionTimeoutError("localhost", 8080, 5000);
    }
    
    @Test
    public void testErrorRouterCreation() {
        assertNotNull(errorRouter);
        assertTrue(errorRouter.getErrorStatistics().isEmpty());
        
        Map<String, Object> stats = errorRouter.getOverallStatistics();
        assertEquals(stats.get("totalErrorsHandled"), 0L);
        assertEquals(stats.get("totalRecoveriesAttempted"), 0L);
        assertEquals(stats.get("totalRecoveriesSucceeded"), 0L);
        assertEquals(stats.get("uniqueErrorTypes"), 0);
        assertEquals(stats.get("registeredObservers"), 0);
        assertEquals(stats.get("registeredStrategies"), 0);
    }
    
    @Test
    public void testAddAndRemoveObserver() {
        errorRouter.addObserver(testObserver);
        
        Map<String, Object> stats = errorRouter.getOverallStatistics();
        assertEquals(stats.get("registeredObservers"), 1);
        
        errorRouter.removeObserver(testObserver);
        stats = errorRouter.getOverallStatistics();
        assertEquals(stats.get("registeredObservers"), 0);
    }
    
    @Test
    public void testObserverNotification() throws ExecutionException, InterruptedException {
        errorRouter.addObserver(testObserver);
        
        CompletableFuture<Optional<RecoveryResult>> future = errorRouter.routeError(testError);
        future.get(); // Wait for completion
        
        assertTrue(testObserver.errorReceived);
        assertEquals(testObserver.lastError, testError);
        assertNotNull(testObserver.lastContext);
    }
    
    @Test
    public void testAddAndRemoveRecoveryStrategy() {
        TestRecoveryStrategy strategy = new TestRecoveryStrategy();
        errorRouter.addRecoveryStrategy(strategy);
        
        Map<String, Object> stats = errorRouter.getOverallStatistics();
        assertEquals(stats.get("registeredStrategies"), 1);
        
        errorRouter.removeRecoveryStrategy(strategy);
        stats = errorRouter.getOverallStatistics();
        assertEquals(stats.get("registeredStrategies"), 0);
    }
    
    @Test
    public void testRecoveryStrategyPriorityOrdering() {
        TestRecoveryStrategy lowPriority = new TestRecoveryStrategy(30);
        TestRecoveryStrategy highPriority = new TestRecoveryStrategy(80);
        TestRecoveryStrategy mediumPriority = new TestRecoveryStrategy(50);
        
        // Add in random order
        errorRouter.addRecoveryStrategy(lowPriority);
        errorRouter.addRecoveryStrategy(highPriority);
        errorRouter.addRecoveryStrategy(mediumPriority);
        
        // All should be able to recover the test error
        lowPriority.canRecoverResult = true;
        highPriority.canRecoverResult = true;
        mediumPriority.canRecoverResult = true;
        
        // Add observer to track which strategy is used
        TestRecoveryObserver recoveryObserver = new TestRecoveryObserver();
        errorRouter.addObserver(recoveryObserver);
        
        try {
            errorRouter.routeError(testError).get();
            
            // Highest priority strategy should be selected
            assertEquals(recoveryObserver.usedStrategy, highPriority);
        } catch (Exception e) {
            fail("Recovery should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testErrorStatisticsTracking() throws ExecutionException, InterruptedException {
        errorRouter.routeError(testError).get();
        
        Map<String, Object> overallStats = errorRouter.getOverallStatistics();
        assertEquals(overallStats.get("totalErrorsHandled"), 1L);
        assertEquals(overallStats.get("uniqueErrorTypes"), 1);
        
        Optional<ErrorRouter.ErrorStatistics> stats = errorRouter.getStatistics("ConnectionTimeoutError");
        assertTrue(stats.isPresent());
        assertEquals(stats.get().getCount(), 1);
        assertEquals(stats.get().getErrorType(), "ConnectionTimeoutError");
        assertNotNull(stats.get().getFirstOccurrence());
        assertNotNull(stats.get().getLastOccurrence());
        assertEquals(stats.get().getHighestSeverity(), NetworkError.Severity.ERROR);
    }
    
    @Test
    public void testMultipleErrorsStatistics() throws ExecutionException, InterruptedException {
        ConnectionRefusedError refusedError = new ConnectionRefusedError("test", 80);
        ValidationError validationError = new ValidationError(
            ValidationError.ValidationType.INVALID_ACTION, 1, "test"
        );
        
        errorRouter.routeError(testError).get();
        errorRouter.routeError(refusedError).get();
        errorRouter.routeError(testError).get(); // Same type again
        errorRouter.routeError(validationError).get();
        
        Map<String, Object> overallStats = errorRouter.getOverallStatistics();
        assertEquals(overallStats.get("totalErrorsHandled"), 4L);
        assertEquals(overallStats.get("uniqueErrorTypes"), 3);
        
        Optional<ErrorRouter.ErrorStatistics> timeoutStats = errorRouter.getStatistics("ConnectionTimeoutError");
        assertTrue(timeoutStats.isPresent());
        assertEquals(timeoutStats.get().getCount(), 2); // Two timeout errors
        
        Optional<ErrorRouter.ErrorStatistics> refusedStats = errorRouter.getStatistics("ConnectionRefusedError");
        assertTrue(refusedStats.isPresent());
        assertEquals(refusedStats.get().getCount(), 1);
        
        Optional<ErrorRouter.ErrorStatistics> validationStats = errorRouter.getStatistics("ValidationError");
        assertTrue(validationStats.isPresent());
        assertEquals(validationStats.get().getCount(), 1);
    }
    
    @Test
    public void testRecoveryWithSuccessfulStrategy() throws ExecutionException, InterruptedException {
        TestRecoveryStrategy successStrategy = new TestRecoveryStrategy();
        successStrategy.canRecoverResult = true;
        successStrategy.recoveryResult = RecoveryResult.success("Recovery successful", 100);
        
        errorRouter.addRecoveryStrategy(successStrategy);
        TestRecoveryObserver observer = new TestRecoveryObserver();
        errorRouter.addObserver(observer);
        
        CompletableFuture<Optional<RecoveryResult>> future = errorRouter.routeError(testError);
        Optional<RecoveryResult> result = future.get();
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        assertTrue(observer.recoveryAttempted);
        assertTrue(observer.recoveryCompleted);
        
        // Check statistics
        Map<String, Object> overallStats = errorRouter.getOverallStatistics();
        assertEquals(overallStats.get("totalRecoveriesAttempted"), 1L);
        assertEquals(overallStats.get("totalRecoveriesSucceeded"), 1L);
        
        Optional<ErrorRouter.ErrorStatistics> stats = errorRouter.getStatistics("ConnectionTimeoutError");
        assertTrue(stats.isPresent());
        assertEquals(stats.get().getRecoveryAttempts(), 1);
        assertEquals(stats.get().getRecoverySuccesses(), 1);
        assertEquals(stats.get().getRecoverySuccessRate(), 1.0, 0.01);
    }
    
    @Test
    public void testRecoveryWithFailedStrategy() throws ExecutionException, InterruptedException {
        TestRecoveryStrategy failStrategy = new TestRecoveryStrategy();
        failStrategy.canRecoverResult = true;
        failStrategy.recoveryResult = RecoveryResult.failure("Recovery failed", 200);
        
        errorRouter.addRecoveryStrategy(failStrategy);
        TestRecoveryObserver observer = new TestRecoveryObserver();
        errorRouter.addObserver(observer);
        
        CompletableFuture<Optional<RecoveryResult>> future = errorRouter.routeError(testError);
        Optional<RecoveryResult> result = future.get();
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isFailure());
        assertTrue(observer.recoveryAttempted);
        assertTrue(observer.recoveryCompleted);
        
        // Check statistics
        Map<String, Object> overallStats = errorRouter.getOverallStatistics();
        assertEquals(overallStats.get("totalRecoveriesAttempted"), 1L);
        assertEquals(overallStats.get("totalRecoveriesSucceeded"), 0L);
        
        Optional<ErrorRouter.ErrorStatistics> stats = errorRouter.getStatistics("ConnectionTimeoutError");
        assertTrue(stats.isPresent());
        assertEquals(stats.get().getRecoveryAttempts(), 1);
        assertEquals(stats.get().getRecoverySuccesses(), 0);
        assertEquals(stats.get().getRecoverySuccessRate(), 0.0, 0.01);
    }
    
    @Test
    public void testNoRecoveryForUnrecoverableError() throws ExecutionException, InterruptedException {
        StateCorruptionError unrecoverableError = new StateCorruptionError(
            StateCorruptionError.CorruptionType.DATA_STRUCTURE, "test", "critical"
        );
        
        TestRecoveryStrategy strategy = new TestRecoveryStrategy();
        strategy.canRecoverResult = true;
        errorRouter.addRecoveryStrategy(strategy);
        
        CompletableFuture<Optional<RecoveryResult>> future = errorRouter.routeError(unrecoverableError);
        Optional<RecoveryResult> result = future.get();
        
        assertFalse(result.isPresent()); // No recovery attempted for unrecoverable error
        assertFalse(strategy.recoveryCalled); // Strategy should not be called
    }
    
    @Test
    public void testRouteException() throws ExecutionException, InterruptedException {
        TestErrorObserver observer = new TestErrorObserver();
        errorRouter.addObserver(observer);
        
        RuntimeException exception = new RuntimeException("Test exception");
        CompletableFuture<Optional<RecoveryResult>> future = errorRouter.routeException(exception);
        future.get();
        
        assertTrue(observer.errorReceived);
        assertNotNull(observer.lastError);
        assertTrue(observer.lastError instanceof ApplicationError);
        assertEquals(observer.lastError.getCause(), exception);
    }
    
    @Test
    public void testClearStatistics() throws ExecutionException, InterruptedException {
        errorRouter.routeError(testError).get();
        
        Map<String, Object> statsBefore = errorRouter.getOverallStatistics();
        assertEquals(statsBefore.get("totalErrorsHandled"), 1L);
        
        errorRouter.clearStatistics();
        
        Map<String, Object> statsAfter = errorRouter.getOverallStatistics();
        assertEquals(statsAfter.get("totalErrorsHandled"), 0L);
        assertEquals(statsAfter.get("uniqueErrorTypes"), 0);
        assertTrue(errorRouter.getErrorStatistics().isEmpty());
    }
    
    // Helper classes for testing
    private static class TestErrorObserver implements ErrorRouter.ErrorObserver {
        boolean errorReceived = false;
        NetworkError lastError;
        Map<String, Object> lastContext;
        
        @Override
        public void onErrorOccurred(NetworkError error, Map<String, Object> context) {
            errorReceived = true;
            lastError = error;
            lastContext = context;
        }
    }
    
    private static class TestRecoveryObserver implements ErrorRouter.ErrorObserver {
        boolean recoveryAttempted = false;
        boolean recoveryCompleted = false;
        ErrorRecoveryStrategy usedStrategy;
        
        @Override
        public void onErrorOccurred(NetworkError error, Map<String, Object> context) {
            // Not used in these tests
        }
        
        @Override
        public void onRecoveryAttempted(NetworkError error, ErrorRecoveryStrategy strategy, RecoveryContext context) {
            recoveryAttempted = true;
            usedStrategy = strategy;
        }
        
        @Override
        public void onRecoveryCompleted(NetworkError error, ErrorRecoveryStrategy strategy, 
                                      RecoveryResult result, RecoveryContext context) {
            recoveryCompleted = true;
        }
    }
    
    private static class TestRecoveryStrategy implements ErrorRecoveryStrategy {
        boolean canRecoverResult = false;
        boolean recoveryCalled = false;
        RecoveryResult recoveryResult = RecoveryResult.notApplicable("Test strategy");
        private final int priority;
        
        TestRecoveryStrategy() {
            this(50);
        }
        
        TestRecoveryStrategy(int priority) {
            this.priority = priority;
        }
        
        @Override
        public boolean canRecover(NetworkError error) {
            return canRecoverResult;
        }
        
        @Override
        public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
            recoveryCalled = true;
            return CompletableFuture.completedFuture(recoveryResult);
        }
        
        @Override
        public NetworkError.RecoveryStrategy getRecoveryType() {
            return NetworkError.RecoveryStrategy.RETRY;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
    }
}